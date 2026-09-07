package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.percent.GiProgressReporter;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.BBoxApo;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.ReadStrategy;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2.ProgressTracker;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.DataReadCommonUtils;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.hutool.core.util.IdUtil;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;
import org.apache.spark.util.LongAccumulator;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 静态矢量瓦片生成器 V3：一个 tile_data PBF 同时容纳多个 MVT 内部图层。
 * <p>
 * V3 不修改 V1/V2 的 DTO、生成器或写表行为。它仅在读取和坐标转换阶段复用已存在的
 * 可序列化函数，聚合键、PBF 编码和输出记录语义均为独立实现：输出表中一条记录代表一个
 * {@code tileSetName + edition + z/x/y} 的完整瓦片集合。
 *
 * @author 张逢吉
 */
public class SparkVectorTileGeneratorV3 implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_READ_PARTITION = 20;
    private static final int WRITE_BATCH_SIZE = 300;
    private static final String TABLE_NAME_PATTERN = "^[a-zA-Z0-9_\\.\\\"\\s]+$";

    private static final GiLogger LOG = GirLoggerFactory.getLogger();
    private transient SparkSession sparkSession;

    public SparkVectorTileGeneratorV3(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    /** 执行 V3 多图层静态切片任务。 */
    public void doGenerate(MultiLayerTileSliceParameter parameter) throws Exception {
        doGenerate(parameter, null);
    }

    /**
     * 执行 V3 多图层静态切片任务，并按 Spark Stage 上报执行进度。
     *
     * <p>回调语义与 V2 保持一致：{@link GiProgressReporter#report(Number, Number)} 的
     * {@code allCount/currentCount} 分别代表当前 Spark Stage 的总任务数和已结束任务数。
     * 因此它反映的是分布式执行进度，而非单纯的要素数量进度。</p>
     *
     * @param parameter 切片参数
     * @param percentReporter 可选进度上报器；传入 {@code null} 时仅输出日志
     */
    public void doGenerate(
            MultiLayerTileSliceParameter parameter, GiProgressReporter percentReporter) throws Exception {
        validateParameter(parameter);
        createTableIfAbsent(parameter);

        ProgressTracker tracker = ProgressTracker.init(sparkSession, 3, percentReporter);
        LongAccumulator featuresRead = tracker.getFeaturesRead();

        tracker.setStageName("读取并转换多图层数据");
        JavaPairRDD<String, V3TileFeatureGroup> allTileFeatures = null;
        for (MvtLayerSliceParameter layer : parameter.getLayers()) {
            TileSliceParameter readParameter = V3LegacyParameterAdapter.toReadParameter(parameter, layer);
            JavaRDD<GirAdvOneRow> source = readLayer(readParameter)
                    .map(row -> {
                        if (row != null) {
                            featuresRead.add(1L);
                        }
                        return row;
                    });
            JavaPairRDD<String, V3TileFeatureGroup> current = source
                    .map(V3SparkTaskFunctions.newTransformFunction(readParameter))
                    .filter(row -> row != null)
                    .flatMapToPair(new V3SparkTaskFunctions.LayerMapToTileFunction(layer.getLayerName(), readParameter));
            allTileFeatures = allTileFeatures == null ? current : allTileFeatures.union(current);
        }
        if (allTileFeatures == null) {
            return;
        }
        tracker.completeStage("图层数: " + parameter.getLayers().size());

        tracker.setStageName("聚合多图层瓦片");
        int partitionNum = Math.max(1, parameter.getReducePartitionNum());
        JavaPairRDD<String, V3TileFeatureGroup> aggregated = allTileFeatures.reduceByKey(
                new V3SparkTaskFunctions.MergeTileFeatureGroupFunction(
                        parameter.getLayers(), parameter.getOutGridSrid()), partitionNum);
        tracker.completeStage("目标分区: " + partitionNum);

        tracker.setStageName("写入多图层瓦片");
        writeTiles(aggregated, parameter, tracker);
        tracker.completeStage();
        tracker.printSummary();
        LOG.info("V3 多图层切片完成，tileSetName:{}，内部图层数:{}", parameter.getTileSetName(), parameter.getLayers().size());
    }

    private JavaRDD<GirAdvOneRow> readLayer(TileSliceParameter parameter) throws Exception {
        ReadStrategy strategy = Optional.ofNullable(parameter.getReadStrategy()).orElse(ReadStrategy.ID_PAGE);
        if (strategy == ReadStrategy.ID_PAGE) {
            return readDataByIdPage(parameter);
        }
        if (strategy == ReadStrategy.BBOX) {
            return readDataByBBox(parameter);
        }
        throw new IllegalArgumentException("V3 不支持的读取策略：" + strategy);
    }

    private JavaRDD<GirAdvOneRow> readDataByIdPage(TileSliceParameter parameter) throws Exception {
        IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(parameter.getInputSource().toDataSource());
        long totalCount = executor.pCount(parameter.getQueryStatement());
        if (totalCount <= 0) {
            throw new IllegalArgumentException("图层 " + parameter.getLayerName() + " 查询结果为空");
        }
        int requested = Optional.ofNullable(parameter.getMaxPartionNum()).orElse(DEFAULT_READ_PARTITION);
        int partitionNum = (int) Math.max(1, Math.min((long) Math.max(1, requested), totalCount));
        int countPerTask = (int) Math.min(Integer.MAX_VALUE,
                Math.max(1L, (totalCount + partitionNum - 1L) / partitionNum));
        String orderField = parameter.getIdFieldName() == null || parameter.getIdFieldName().trim().isEmpty()
                ? parameter.getGeomFieldName() : parameter.getIdFieldName();
        List<Integer> pages = DataReadCommonUtils.buildPageNumberList(totalCount, partitionNum);
        Dataset<Integer> dataSet = sparkSession.createDataset(pages, Encoders.INT())
                .repartition(Math.min(pages.size(), partitionNum));
        return dataSet.javaRDD().flatMap(new cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.SparkTaskSerializableUtil.IdPageFlatMapFunction(
                parameter, parameter.getQueryStatement(), orderField, countPerTask));
    }

    private JavaRDD<GirAdvOneRow> readDataByBBox(TileSliceParameter parameter) throws Exception {
        IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(parameter.getInputSource().toDataSource());
        BBoxApo extent = executor.eGetExtent(parameter.getQueryStatement(), parameter.getGeomFieldName());
        if (extent == null) {
            throw new IllegalArgumentException("图层 " + parameter.getLayerName() + " 无法获取空间范围");
        }
        int partitionNum = Math.max(1, Optional.ofNullable(parameter.getMaxPartionNum()).orElse(DEFAULT_READ_PARTITION));
        List<String> conditions = DataReadCommonUtils.buildBboxPartitionConditions(
                extent, partitionNum, parameter.getSourceDataSrid());
        Dataset<String> dataSet = sparkSession.createDataset(conditions, Encoders.STRING())
                .repartition(conditions.size());
        return dataSet.javaRDD().flatMap(new cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.SparkTaskSerializableUtil.BboxFlatMapFunction(
                parameter, parameter.getQueryStatement(), parameter.getGeomFieldName(), parameter.getSourceDataSrid()));
    }

    private void writeTiles(
            JavaPairRDD<String, V3TileFeatureGroup> aggregated,
            MultiLayerTileSliceParameter parameter,
            ProgressTracker tracker) {
        final LongAccumulator tilesWritten = tracker.getTilesWritten();
        final LongAccumulator batchesWritten = tracker.getBatchesWritten();
        final LongAccumulator bytesWritten = tracker.getBytesWritten();
        aggregated.foreachPartition((VoidFunction<Iterator<Tuple2<String, V3TileFeatureGroup>>>) iterator -> {
            DataSourceConfig output = parameter.getOutputSource();
            String table = output.getTableNameForSql();
            try (Connection connection = output.toDataSource().getConnection();
                 PreparedStatement delete = connection.prepareStatement(deleteSql(table));
                 PreparedStatement insert = connection.prepareStatement(insertSql(table))) {
                connection.setAutoCommit(false);
                int batchSize = 0;
                long batchBytes = 0L;
                try {
                    while (iterator.hasNext()) {
                        Tuple2<String, V3TileFeatureGroup> item = iterator.next();
                        PbfInfo pbf = MultiLayerMvtEncoderV3.encode(item._1, item._2, parameter);
                        bindDelete(delete, item._1, pbf, parameter);
                        bindInsert(insert, item._1, pbf, parameter);
                        batchSize++;
                        batchBytes += pbf.getData() == null ? 0L : pbf.getData().length;
                        if (batchSize >= WRITE_BATCH_SIZE) {
                            delete.executeBatch();
                            insert.executeBatch();
                            connection.commit();
                            tilesWritten.add(batchSize);
                            batchesWritten.add(1L);
                            bytesWritten.add(batchBytes);
                            batchSize = 0;
                            batchBytes = 0L;
                        }
                    }
                    if (batchSize > 0) {
                        delete.executeBatch();
                        insert.executeBatch();
                        connection.commit();
                        tilesWritten.add(batchSize);
                        batchesWritten.add(1L);
                        bytesWritten.add(batchBytes);
                    }
                } catch (Exception e) {
                    connection.rollback();
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            } catch (Exception e) {
                throw new RuntimeException("V3 多图层瓦片写入失败", e);
            }
        });
    }

    private static void bindDelete(
            PreparedStatement statement, String tileId, PbfInfo pbf, MultiLayerTileSliceParameter parameter) throws Exception {
        TileZxyApo zxy = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileId);
        statement.setInt(1, zxy.getZ());
        statement.setInt(2, zxy.getX());
        statement.setInt(3, zxy.getY());
        statement.setInt(4, pbf.getGridSrid());
        statement.setString(5, parameter.getTileSetName());
        statement.setString(6, parameter.getEdition());
        statement.addBatch();
    }

    private static void bindInsert(
            PreparedStatement statement, String tileId, PbfInfo pbf, MultiLayerTileSliceParameter parameter) throws Exception {
        TileZxyApo zxy = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileId);
        statement.setString(1, IdUtil.getSnowflakeNextIdStr());
        statement.setInt(2, zxy.getZ());
        statement.setInt(3, zxy.getX());
        statement.setInt(4, getTmsY(zxy, pbf.getGridSrid()));
        statement.setInt(5, zxy.getY());
        statement.setInt(6, pbf.getGridSrid());
        statement.setBytes(7, pbf.getData());
        statement.setString(8, parameter.getTileSetName());
        statement.setString(9, parameter.getEdition());
        statement.setLong(10, System.currentTimeMillis());
        statement.addBatch();
    }

    private static int getTmsY(TileZxyApo zxy, int gridSrid) {
        if (gridSrid == 3857) {
            return GirGeoTools.defaultInstance().getTileGrid3857Opt()
                    .convertY(zxy.getZ(), zxy.getY(), TileYAxis.XYZ, TileYAxis.TMS);
        }
        return GirGeoTools.defaultInstance().getTileGrid4326SeparateOpt()
                .convertY(zxy.getZ(), zxy.getY(), TileYAxis.XYZ, TileYAxis.TMS);
    }

    private static String insertSql(String table) {
        return "INSERT INTO " + table + " (id,z,x,tms_y,y,grid_srid,tile_data,layer_name,edition,insert_time)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?)";
    }

    private static String deleteSql(String table) {
        return "DELETE FROM " + table + " WHERE z=? AND x=? AND y=? AND grid_srid=? AND layer_name=?"
                + " AND COALESCE(edition, '') = COALESCE(?, '')";
    }

    private void createTableIfAbsent(MultiLayerTileSliceParameter parameter) {
        DataSourceConfig output = parameter.getOutputSource();
        IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(output.toDataSource());
        String table = executor.tbGetTableNameWithSchema(output.getTableNameForSql());
        validateTableName(table);
        if (executor.dIsTableExists(table)) {
            return;
        }
        String indexName = "zxy_v3_" + IdUtil.getSnowflakeNextIdStr();
        String ddl = String.format("CREATE TABLE %s ("
                        + "id text, z int4, x int4, tms_y int4, y int4, grid_srid int4, tile_data bytea, "
                        + "layer_name text, edition text, insert_time int8);"
                        + "CREATE INDEX %s ON %s (z,x,y,grid_srid,layer_name,edition);",
                table, indexName, table);
        executor.dExecuteDDL(ddl, table, "创建 V3 多图层瓦片表");
    }

    private static void validateParameter(MultiLayerTileSliceParameter parameter) {
        if (parameter == null || parameter.getOutputSource() == null) {
            throw new IllegalArgumentException("V3 输出数据源不能为空");
        }
        if (parameter.getOutputSource().getTableNameForSql() == null
                || parameter.getOutputSource().getTableNameForSql().trim().isEmpty()) {
            throw new IllegalArgumentException("V3 输出表名不能为空");
        }
        if (parameter.getTileSetName() == null || parameter.getTileSetName().trim().isEmpty()) {
            throw new IllegalArgumentException("V3 tileSetName 不能为空");
        }
        if (parameter.getMinZoom() > parameter.getMaxZoom()) {
            throw new IllegalArgumentException("V3 minZoom 不能大于 maxZoom");
        }
        if (parameter.getLayers() == null || parameter.getLayers().isEmpty()) {
            throw new IllegalArgumentException("V3 至少需要配置一个内部图层");
        }
        Set<String> layerNames = new HashSet<>();
        for (MvtLayerSliceParameter layer : parameter.getLayers()) {
            if (layer == null || layer.getLayerName() == null || layer.getLayerName().trim().isEmpty()
                    || layer.getInputSource() == null || layer.getGeomFieldName() == null
                    || layer.getGeomFieldName().trim().isEmpty() || layer.getQueryStatement() == null
                    || layer.getQueryStatement().trim().isEmpty()) {
                throw new IllegalArgumentException("V3 每个图层都必须配置 layerName、inputSource、geomFieldName 和 queryStatement");
            }
            if (!layerNames.add(layer.getLayerName())) {
                throw new IllegalArgumentException("V3 内部图层名称重复：" + layer.getLayerName());
            }
        }
    }

    private static void validateTableName(String tableName) {
        if (tableName == null || !tableName.matches(TABLE_NAME_PATTERN)) {
            throw new IllegalArgumentException("输出表名包含非法字符：" + tableName);
        }
    }
}
