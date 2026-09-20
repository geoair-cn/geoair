package cn.geoair.map.dynamic.statics.mvt.v4;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.percent.GiProgressReporter;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputType;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.V3TileWriteStats;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.archive.MbtilesArchiveUtils;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.archive.PmtilesUtils;
import cn.geoair.map.dynamic.statics.mvt.v4.input.V4InputValidator;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.output.V3TileStoreFactory;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.VectorTileCommonUtils;
import cn.geoair.map.dynamic.statics.mvt.v4.dto.V4Options;
import cn.geoair.map.dynamic.statics.mvt.v4.dto.V4TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.v4.group.V4TileGroupStore;
import cn.geoair.map.dynamic.statics.mvt.v4.input.V4FeatureReader;
import cn.geoair.map.dynamic.statics.mvt.v4.input.V4FeatureReaderFactory;
import cn.geoair.map.dynamic.statics.mvt.v4.input.V4GeoJsonFeatureMapper;
import cn.geoair.map.dynamic.statics.mvt.v4.stats.V4TileStats;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 静态矢量瓦片生成器 V4：不依赖 Spark 运行时的单机实现。
 *
 * <p>与 V3 的关系：<b>参数、分配规则、编码器、输出与归档全部复用 V3 的实现</b>，
 * V4 只替换"执行编排"这一层 —— 把 V3 的
 * 「读取 → 逐要素分配瓦片 → {@code reduceByKey} 聚合 → 编码写出」换成单机顺序执行
 * （读取 → 分配 → 内存缓冲 + 溢写 → 归并后逐瓦片编码写出）。</p>
 *
 * <p>因此 V4 的产物与 V3 可比：同一份数据、同一组参数下，瓦片键、每瓦片要素集合
 * （含聚合安全阀的削减结果）、属性输出规则都由同一批代码决定。</p>
 *
 * <p><b>瓦片统计由 V4 自己做</b>（tippecanoe 的 {@code tilestats}）：聚合回调里按<b>输入序号</b>
 * 去重，然后写进任务元数据。V3 没有统计阶段，所以 V4 不沿用"给每行打一个唯一标记"
 * 的做法 —— 在 V4 里那个标记既没有消费者、又会变成 PBF 属性（见 {@link V4TileStats}）。</p>
 *
 * <p><b>本类只调用 V1/V2/V3 的公开 API</b>，不修改它们。V3 侧唯一的改动是去掉一处
 * 无消费者的统计标记，与 V4 的执行编排无关（见 {@code V4开发计划.md} 第八节）。</p>
 *
 * @author 张逢吉
 */
public class V4VectorTileGenerator {

    private static final GiLogger LOG = GirLoggerFactory.getLogger();
    private static final String TABLE_NAME_PATTERN = "^[a-zA-Z0-9_\\.\\\"\\s]+$";

    /** 上一次执行的写出统计，由 {@link #doGenerate} 结束后填充。 */
    private V3TileWriteStats lastStats;

    /** 上游要素读取器抛出的异常会被包进 lambda，这里留一个引用以便原样抛回。 */
    private volatile Exception readFailure;

    public void doGenerate(V4TileSliceParameter parameter) throws Exception {
        doGenerate(parameter, null);
    }

    /**
     * 执行 V4 切片任务。
     *
     * @param parameter V4 任务参数（内含完整的 V3 参数）
     * @param reporter  可选进度上报器；传 null 时仅输出日志
     */
    public void doGenerate(V4TileSliceParameter parameter, GiProgressReporter reporter) throws Exception {
        validateParameter(parameter);
        MultiLayerTileSliceParameter base = parameter.getBase();
        V4Options options = parameter.resolveOptions();
        boolean archive = isArchiveOutput(base);
        V4SliceProgress progress = new V4SliceProgress(reporter, archive ? 4 : 3);

        if (resolveOutputType(base) == V3TileOutputType.POSTGRESQL) {
            createTableIfAbsent(base);
        }

        V4TileGroupStore.V4GroupStats groupStats = null;
        V4TileWriter writer = new V4TileWriter(base, progress);
        // 统计开关关闭时不建收集器：聚合回调与元数据都不需要做额外工作
        V4TileStats tileStats = options.isTileStats() ? new V4TileStats(base.getLayers()) : null;
        try (V4TileGroupStore store = new V4TileGroupStore(
                base.getLayers(), base.getOutGridSrid(), options, tileStats)) {

            progress.stage("读取并转换多图层数据");
            Map<String, Long> geometrySkips = new LinkedHashMap<>();
            int attributeTypeFailures = 0;
            for (MvtLayerSliceParameter layer : base.getLayers()) {
                attributeTypeFailures += readLayer(base, layer, options, store, progress, geometrySkips);
            }
            progress.completeStage("图层数: " + base.getLayers().size()
                    + "，要素数: " + progress.getFeaturesRead());

            progress.stage("聚合多图层瓦片并编码写出");
            groupStats = store.finish(writer::write);
            progress.completeStage("瓦片数: " + progress.getTilesWritten());

            progress.stage("写出任务元数据");
            writer.writeMetadata(JSON.toJSONString(buildOutputMetadata(base, tileStats)));
            if (tileStats != null) {
                LOG.info("V4 瓦片统计：{}", tileStats.summary());
            }
            progress.completeStage("元数据: " + (base.getOutputConfig().isWriteMetadata()
                    ? base.getOutputConfig().getMetadataFileName() : "已关闭"));

            if (attributeTypeFailures > 0) {
                LOG.warn("V4 --attribute-type 有 {} 个字段值无法按声明类型转换，已保留原值", attributeTypeFailures);
            }
            if (!geometrySkips.isEmpty()) {
                LOG.info("V4 几何为空的要素已跳过，按图层统计: {}", geometrySkips);
            }
        } finally {
            writer.close();
        }

        if (archive) {
            progress.stage("归档单文件瓦片");
            archiveTiles(base, tileStats);
            progress.completeStage("归档完成");
        }

        lastStats = new V3TileWriteStats(
                progress.getTilesWritten(), progress.getBytesWritten(), progress.getBatchesWritten(),
                progress.getFeaturesRead(), archiveFileSize(base));
        LOG.info("V4 多图层切片完成，tileSetName:{}，内部图层数:{}，{}，{}",
                base.getTileSetName(), base.getLayers().size(), groupStats, lastStats);
    }

    /** 上一次执行（{@link #doGenerate}）的写出统计；一个实例对应一次执行。 */
    public V3TileWriteStats getLastStats() {
        return lastStats;
    }

    // ------------------------------------------------------------------
    // 读取与分配
    // ------------------------------------------------------------------

    /**
     * 读取一个图层并把每个要素分配到它覆盖的瓦片。
     *
     * @return {@code --attribute-type} 转换失败的字段个数
     */
    private int readLayer(MultiLayerTileSliceParameter base, MvtLayerSliceParameter layer,
            V4Options options, V4TileGroupStore store, V4SliceProgress progress,
            Map<String, Long> geometrySkips) throws Exception {
        TileSliceParameter readParameter = toReadParameter(base, layer);
        V4FeatureReader reader = V4FeatureReaderFactory.getReader(layer);
        long[] sequence = {0L};
        long[] skipped = {0L};
        int[] failures = {0};
        // 读取器的回调里不能抛受检异常以外的扩展，异常统一转成 RuntimeException 再在下面还原
        reader.read(layer, row -> {
            try {
                long currentSequence = sequence[0]++;
                failures[0] += V4GeoJsonFeatureMapper.applyAttributeTypes(
                        row, options.getAttributeTypes(), layer.getGeomFieldName());
                // 这里不写统计去重键：V4 的统计按输入序号去重（同一个源要素的多块瓦片行共用
                // 一个序号），不需要往行里塞任何标记 —— 塞了就会变成 PBF 属性（V3 的老问题）。
                GirAdvOneRow transformed = VectorTileCommonUtils.transformSingleFeature(row, readParameter);
                if (transformed == null) {
                    skipped[0]++;
                    return;
                }
                progress.addFeature();
                Map<String, List<GirAdvOneRow>> tiles = VectorTileCommonUtils.mapSingleFeatureToTiles(
                        transformed, readParameter.getGeomFieldName(),
                        readParameter.getMinZoom(), readParameter.getMaxZoom(),
                        readParameter.getOutGridSrid());
                for (Map.Entry<String, List<GirAdvOneRow>> tile : tiles.entrySet()) {
                    store.add(tile.getKey(), layer.getLayerName(), tile.getValue(), currentSequence);
                }
                store.spillIfNeeded();
            } catch (Exception e) {
                readFailure = e;
                throw new IllegalStateException("V4 处理图层 " + layer.getLayerName() + " 的要素失败", e);
            }
        });
        if (readFailure != null) {
            Exception failure = readFailure;
            readFailure = null;
            throw failure;
        }
        if (skipped[0] > 0) {
            geometrySkips.put(layer.getLayerName(), skipped[0]);
        }
        return failures[0];
    }

    /**
     * 组装坐标转换与瓦片覆盖所需的内部参数。
     *
     * <p>V3 有一份同样的适配器（{@code V3LegacyParameterAdapter}），但它是包级私有、
     * V4 调不到，所以这里按同样的字段映射自己拼一遍 —— 字段取值与 V3 逐项对齐。</p>
     */
    private static TileSliceParameter toReadParameter(
            MultiLayerTileSliceParameter task, MvtLayerSliceParameter layer) {
        return new TileSliceParameter()
                .setGeomFieldName(layer.getGeomFieldName())
                .setIdFieldName(layer.getIdFieldName())
                .setLayerName(layer.getLayerName())
                .setSourceDataSrid(layer.resolveSourceDataSrid())
                .setOutGridSrid(task.getOutGridSrid())
                .setMinZoom(layer.getMinZoom() == null ? task.getMinZoom() : layer.getMinZoom())
                .setMaxZoom(layer.getMaxZoom() == null ? task.getMaxZoom() : layer.getMaxZoom());
    }

    // ------------------------------------------------------------------
    // 元数据与归档
    // ------------------------------------------------------------------

    /**
     * 组装目录清单、MBTiles 与 PMTiles 共用的元数据（字段与 V3 同一套）。
     *
     * @param tileStats 瓦片统计；为 null 表示本任务关闭统计（不写 {@code tilestats} 段）
     */
    private Map<String, Object> buildOutputMetadata(MultiLayerTileSliceParameter parameter, V4TileStats tileStats) {
        V3TileOutputConfig outputConfig = parameter.getOutputConfig();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("format", "pbf");
        metadata.put("contentEncoding", parameter.isGzipPbf() ? "gzip" : "none");
        metadata.put("tilePathTemplate", "{z}/{x}/{y}.pbf");
        metadata.put("yAxis", outputConfig.getTileYAxis() == null
                ? TileYAxis.XYZ.name()
                : outputConfig.getTileYAxis().name());
        metadata.put("gridSrid", parameter.getOutGridSrid());
        metadata.put("tileSetName", parameter.getTileSetName());
        metadata.put("edition", parameter.getEdition());
        metadata.put("minZoom", parameter.getMinZoom());
        metadata.put("maxZoom", parameter.getMaxZoom());
        List<Map<String, Object>> layers = new ArrayList<>();
        List<Map<String, Object>> vectorLayers = new ArrayList<>();
        for (MvtLayerSliceParameter layer : parameter.getLayers()) {
            Map<String, Object> layerMetadata = new LinkedHashMap<>();
            layerMetadata.put("name", layer.getLayerName());
            layerMetadata.put("minZoom", layer.getMinZoom());
            layerMetadata.put("maxZoom", layer.getMaxZoom());
            layers.add(layerMetadata);
            Map<String, Object> vectorLayer = new LinkedHashMap<>();
            vectorLayer.put("id", layer.getLayerName());
            vectorLayer.put("minzoom", layer.getMinZoom() == null ? parameter.getMinZoom() : layer.getMinZoom());
            vectorLayer.put("maxzoom", layer.getMaxZoom() == null ? parameter.getMaxZoom() : layer.getMaxZoom());
            vectorLayer.put("fields", tileStats == null
                    ? Collections.emptyMap() : tileStats.fieldTypes(layer.getLayerName()));
            vectorLayers.add(vectorLayer);
        }
        metadata.put("layers", layers);
        metadata.put("name", parameter.getTileSetName());
        metadata.put("version", parameter.getEdition() == null || parameter.getEdition().trim().isEmpty()
                ? "1.0" : parameter.getEdition());
        metadata.put("type", "overlay");
        metadata.put("vector_layers", vectorLayers);
        if (tileStats != null) {
            // tippecanoe 把 tilestats 写在元数据里（目录输出的 metadata.json / 归档的 JSON 头）
            metadata.put("tilestats", JSON.toJSON(tileStats.toTileStats()));
        }
        metadata.put("engine", "v4");
        return metadata;
    }

    /** 把已写入暂存目录的瓦片归档为 MBTiles 或 PMTiles（复用 V3 的归档实现）。 */
    private void archiveTiles(MultiLayerTileSliceParameter parameter, V4TileStats tileStats) throws Exception {
        if (parameter.getOutGridSrid() != 3857) {
            throw new IllegalArgumentException("V4 MBTiles/PMTiles 输出仅支持 WebMercator（EPSG:3857）网格");
        }
        V3TileOutputConfig outputConfig = parameter.getOutputConfig();
        String metadataJson = JSON.toJSONString(buildOutputMetadata(parameter, tileStats));
        if (resolveOutputType(parameter) == V3TileOutputType.MBTILES) {
            MbtilesArchiveUtils.archive(
                    Paths.get(outputConfig.getStagingDirectory()), Paths.get(outputConfig.getMbtilesFile()),
                    outputConfig.getTileYAxis(), outputConfig.isOverwrite(), outputConfig.getArchiveBatchSize(),
                    MbtilesArchiveUtils.metadata(parameter.getTileSetName(), parameter.getEdition(),
                            parameter.getMinZoom(), parameter.getMaxZoom(), metadataJson));
            return;
        }
        if (resolveOutputType(parameter) == V3TileOutputType.PMTILES) {
            PmtilesUtils.archive(
                    Paths.get(outputConfig.getStagingDirectory()), Paths.get(outputConfig.getPmtilesFile()),
                    outputConfig.getTileYAxis(), outputConfig.isOverwrite(),
                    parameter.isGzipPbf(), metadataJson);
        }
    }

    private long archiveFileSize(MultiLayerTileSliceParameter parameter) {
        V3TileOutputType outputType = resolveOutputType(parameter);
        V3TileOutputConfig outputConfig = parameter.getOutputConfig();
        String file = null;
        if (outputType == V3TileOutputType.MBTILES) {
            file = outputConfig.getMbtilesFile();
        } else if (outputType == V3TileOutputType.PMTILES) {
            file = outputConfig.getPmtilesFile();
        }
        if (file == null) {
            return 0L;
        }
        try {
            Path path = Paths.get(file);
            return Files.exists(path) ? Files.size(path) : 0L;
        } catch (IOException e) {
            LOG.warn("V4 读取归档文件大小失败: {} - {}", file, e.getMessage());
            return 0L;
        }
    }

    // ------------------------------------------------------------------
    // PostgreSQL 建表
    // ------------------------------------------------------------------

    private void createTableIfAbsent(MultiLayerTileSliceParameter parameter) {
        DataSourceConfig output = parameter.getOutputConfig().getPostgresqlOutputSource();
        IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(output.toDataSource());
        String table = executor.tbGetTableNameWithSchema(output.getTableNameForSql());
        validateTableName(table);
        if (executor.dIsTableExists(table)) {
            return;
        }
        String indexName = "zxy_v4_" + IdUtil.getSnowflakeNextIdStr();
        String ddl = String.format("CREATE TABLE %s ("
                        + "id text, z int4, x int4, tms_y int4, y int4, grid_srid int4, tile_data bytea, "
                        + "layer_name text, edition text, insert_time int8);"
                        + "CREATE INDEX %s ON %s (z,x,y,grid_srid,layer_name,edition);",
                table, indexName, table);
        executor.dExecuteDDL(ddl, table, "创建 V4 多图层瓦片表");
    }

    // ------------------------------------------------------------------
    // 校验
    // ------------------------------------------------------------------

    private static void validateParameter(V4TileSliceParameter parameter) {
        if (parameter == null || parameter.getBase() == null) {
            throw new IllegalArgumentException("V4 切片参数与 V3 基础参数不能为空");
        }
        MultiLayerTileSliceParameter base = parameter.getBase();
        V3TileOutputType outputType = resolveOutputType(base);
        if (outputType == V3TileOutputType.POSTGRESQL) {
            DataSourceConfig output = base.getOutputConfig().getPostgresqlOutputSource();
            if (output == null) {
                throw new IllegalArgumentException("V4 PostgreSQL 输出数据源不能为空");
            }
            if (output.getTableNameForSql() == null || output.getTableNameForSql().trim().isEmpty()) {
                throw new IllegalArgumentException("V4 PostgreSQL 输出表名不能为空");
            }
        } else {
            V3TileStoreFactory.validate(base.getOutputConfig());
            if (isArchiveOutput(base) && base.getOutGridSrid() != 3857) {
                throw new IllegalArgumentException("V4 MBTiles/PMTiles 输出仅支持 WebMercator（EPSG:3857）网格");
            }
        }
        if (base.getTileSetName() == null || base.getTileSetName().trim().isEmpty()) {
            throw new IllegalArgumentException("V4 tileSetName 不能为空");
        }
        if (base.getMinZoom() > base.getMaxZoom()) {
            throw new IllegalArgumentException("V4 minZoom 不能大于 maxZoom");
        }
        if (base.getLayers() == null || base.getLayers().isEmpty()) {
            throw new IllegalArgumentException("V4 至少需要配置一个内部图层");
        }
        Set<String> layerNames = new HashSet<>();
        for (MvtLayerSliceParameter layer : base.getLayers()) {
            if (layer == null || layer.getLayerName() == null || layer.getLayerName().trim().isEmpty()
                    || layer.getGeomFieldName() == null || layer.getGeomFieldName().trim().isEmpty()) {
                throw new IllegalArgumentException("V4 每个图层都必须配置 layerName 和 geomFieldName");
            }
            V4InputValidator.validate(layer);
            if (!layerNames.add(layer.getLayerName())) {
                throw new IllegalArgumentException("V4 内部图层名称重复：" + layer.getLayerName());
            }
        }
        V4Options options = parameter.resolveOptions();
        if (options.getSpillRowThreshold() <= 0) {
            throw new IllegalArgumentException("V4 spillRowThreshold 必须大于 0");
        }
    }

    private static V3TileOutputType resolveOutputType(MultiLayerTileSliceParameter parameter) {
        if (parameter.getOutputConfig() == null || parameter.getOutputConfig().getOutputType() == null) {
            throw new IllegalArgumentException("V4 outputConfig 与 outputType 不能为空");
        }
        return parameter.getOutputConfig().getOutputType();
    }

    private static boolean isArchiveOutput(MultiLayerTileSliceParameter parameter) {
        V3TileOutputType outputType = resolveOutputType(parameter);
        return outputType == V3TileOutputType.MBTILES || outputType == V3TileOutputType.PMTILES;
    }

    private static void validateTableName(String tableName) {
        if (tableName == null || !tableName.matches(TABLE_NAME_PATTERN)) {
            throw new IllegalArgumentException("输出表名包含非法字符：" + tableName);
        }
    }
}
