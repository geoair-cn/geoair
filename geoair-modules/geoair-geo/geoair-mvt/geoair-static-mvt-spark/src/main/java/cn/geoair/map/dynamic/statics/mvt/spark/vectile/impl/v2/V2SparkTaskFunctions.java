package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.mvt.tools.model.VecConstant;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.PbfTargetInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.VectorTileCommonUtils;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.IdUtil;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import scala.Tuple2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * V2 版本专用的 Spark 函数集合。
 * <p>
 * 该类完整承载 V2 使用的 executor 闭包，不依赖 V1/V3 的任务实现。
 */
public class V2SparkTaskFunctions implements Serializable {

    private static final long serialVersionUID = 1L;

    /** V2 的 ID 分页读取任务。 */
    public static class IdPageFlatMapFunction implements FlatMapFunction<Integer, GirAdvOneRow> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;
        private final String queryStatement;
        private final String orderFieldName;
        private final int countPerTask;

        public IdPageFlatMapFunction(TileSliceParameter parameter, String queryStatement,
                String orderFieldName, int countPerTask) {
            this.parameter = parameter;
            this.queryStatement = queryStatement;
            this.orderFieldName = orderFieldName;
            this.countPerTask = countPerTask;
        }

        @Override
        public Iterator<GirAdvOneRow> call(Integer pageNum) {
            DataSourceConfig inputSource = parameter.getInputSource();
            IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(inputSource.toDataSource());
            String orderSql = executor.pBuildSqlWithOrder(queryStatement,
                    ListUtil.of(OrderApo.create(orderFieldName, AdvEnumsOrder.升序)));
            return executor.bSelectList(executor.pBuildPageSql(orderSql, countPerTask, pageNum, true)).iterator();
        }
    }

    /** V2 的 BBOX 分片读取任务。 */
    public static class BboxFlatMapFunction implements FlatMapFunction<String, GirAdvOneRow> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;
        private final String queryStatement;
        private final String geomFieldName;
        private final int sourceDataSrid;

        public BboxFlatMapFunction(TileSliceParameter parameter, String queryStatement,
                String geomFieldName, int sourceDataSrid) {
            this.parameter = parameter;
            this.queryStatement = queryStatement;
            this.geomFieldName = geomFieldName;
            this.sourceDataSrid = sourceDataSrid;
        }

        @Override
        public Iterator<GirAdvOneRow> call(String condition) {
            String[] coords = condition.split(",");
            String sql = V2DataReadUtils.buildBboxQuerySql(queryStatement, geomFieldName,
                    Double.parseDouble(coords[0]), Double.parseDouble(coords[2]),
                    Double.parseDouble(coords[1]), Double.parseDouble(coords[3]), sourceDataSrid);
            IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(parameter.getInputSource().toDataSource());
            return executor.bSelectList(sql).iterator();
        }
    }

    /** V2 的要素坐标转换任务。 */
    public static class TransformFeatureFunction
            implements org.apache.spark.api.java.function.Function<GirAdvOneRow, GirAdvOneRow> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;

        public TransformFeatureFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public GirAdvOneRow call(GirAdvOneRow row) {
            row.put(VecConstant.FeatureRowID, IdUtil.fastSimpleUUID());
            return VectorTileCommonUtils.transformSingleFeature(row, parameter);
        }
    }

    /** V2 的普通聚合及密度限制任务。 */
    public static class AggregateAndLimitFeatureFunction
            implements Function2<List<GirAdvOneRow>, List<GirAdvOneRow>, List<GirAdvOneRow>> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;

        public AggregateAndLimitFeatureFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public List<GirAdvOneRow> call(List<GirAdvOneRow> left, List<GirAdvOneRow> right) {
            return VectorTileCommonUtils.limitTileFeatures(
                    VectorTileCommonUtils.aggregateTileFeatures(left, right), parameter);
        }
    }

    /** V2 的流式要素到瓦片映射任务。 */
    public static class MapToTileFunction
            implements PairFlatMapFunction<GirAdvOneRow, String, List<GirAdvOneRow>> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;

        public MapToTileFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Iterator<Tuple2<String, List<GirAdvOneRow>>> call(GirAdvOneRow feature) {
            if (feature == null) {
                return Collections.emptyIterator();
            }
            return VectorTileCommonUtils.mapSingleFeatureToTilesStream(feature,
                    parameter.getGeomFieldName(), parameter.getMinZoom(), parameter.getMaxZoom(), parameter.getOutGridSrid());
        }
    }

    /** V2 的 PBF 编码任务。 */
    public static class GeneratePbfFunction implements org.apache.spark.api.java.function.Function<
            Tuple2<String, List<GirAdvOneRow>>, Tuple2<String, PbfInfo>> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;
        private final PbfTargetInfo pbfTargetInfo;

        public GeneratePbfFunction(TileSliceParameter parameter, PbfTargetInfo pbfTargetInfo) {
            this.parameter = parameter;
            this.pbfTargetInfo = pbfTargetInfo;
        }

        @Override
        public Tuple2<String, PbfInfo> call(Tuple2<String, List<GirAdvOneRow>> feature) throws Exception {
            return new Tuple2<>(feature._1, VectorTileCommonUtils.generateSingleTilePbf(
                    feature._1, feature._2, parameter, pbfTargetInfo));
        }
    }

    /**
     * 单 zoom 级别的流式瓦片映射函数。
     * <p>
     * 用于统计路径，替代原版 {@code MapToTileFunctionToStatic}（HashMap 全量收集版本）。
     * 使用 {@link cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.TileIterator}
     * 懒生成瓦片，避免 OOM。
     */
    public static class MapToTileFunctionSingleZoom
            implements PairFlatMapFunction<GirAdvOneRow, String, List<GirAdvOneRow>>, Serializable {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;
        private final int zoom;

        public MapToTileFunctionSingleZoom(TileSliceParameter parameter, int zoom) {
            this.parameter = parameter;
            this.zoom = zoom;
        }

        @Override
        public Iterator<Tuple2<String, List<GirAdvOneRow>>> call(GirAdvOneRow feature) throws Exception {
            if (feature == null) {
                return Collections.emptyIterator();
            }
            // 使用流式版本，只映射到指定的单一 zoom 级别
            return VectorTileCommonUtils.mapSingleFeatureToTilesStream(
                    feature,
                    parameter.getGeomFieldName(),
                    zoom,
                    zoom,
                    parameter.getOutGridSrid());
        }
    }

    /**
     * 有界聚合函数 — 始终限制合并后的列表大小，防止 OOM。
     * <p>
     * 即使用户未开启 featureLimitEnabled，也使用 DEFAULT_HARD_LIMIT 作为安全兜底，
     * 避免高密度瓦片（如 zoom 4-6 覆盖大面积区域）累积数万要素导致内存溢出。
     * <p>
     * 策略：
     * <ul>
     *   <li>合并前先检查两个 list 的总大小</li>
     *   <li>超限时只保留 list1 全部 + list2 的前 N 个，不创建超大临时 list</li>
     *   <li>合并后再应用密度优化/截断</li>
     * </ul>
     */
    public static class BoundedAggregateFunction
            implements Function2<List<GirAdvOneRow>, List<GirAdvOneRow>, List<GirAdvOneRow>>,
            Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 硬上限：即使用户未配置 featureLimit，也最多保留此数量的要素。
         * 用 CPU 换内存 —— 多余的要素会被丢弃，但不会 OOM。
         */
        private static final int DEFAULT_HARD_LIMIT = 8000;

        private final TileSliceParameter parameter;

        public BoundedAggregateFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public List<GirAdvOneRow> call(List<GirAdvOneRow> list1, List<GirAdvOneRow> list2) throws Exception {
            int limit = getEffectiveLimit();
            int totalSize = list1.size() + list2.size();

            // 未超限：正常合并
            if (totalSize <= limit) {
                List<GirAdvOneRow> merged = new ArrayList<>(totalSize);
                merged.addAll(list1);
                merged.addAll(list2);
                return merged;
            }

            // 超限：有界合并（不超过 limit），再应用密度优化
            List<GirAdvOneRow> bounded = new ArrayList<>(limit);
            bounded.addAll(list1);
            int remaining = limit - list1.size();
            if (remaining > 0) {
                int take = Math.min(remaining, list2.size());
                bounded.addAll(list2.subList(0, take));
            }
            return V2VectorTileUtils.limitTileFeatures(bounded, parameter);
        }

        /**
         * 获取有效限制值：用户配置优先，否则使用硬上限兜底。
         */
        private int getEffectiveLimit() {
            if (parameter.isFeatureLimitEnabled() && parameter.getFeatureLimit() != null) {
                return parameter.getFeatureLimit();
            }
            return DEFAULT_HARD_LIMIT;
        }
    }

    /** V2 Root 瓦片写库行构建任务。 */
    public static class BuildRowFunction implements org.apache.spark.api.java.function.Function<Tuple2<String, PbfInfo>, Row> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;

        public BuildRowFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Row call(Tuple2<String, PbfInfo> tuple) {
            PbfInfo pbf = tuple._2;
            byte[] data = pbf.getData();
            pbf.setData(null);
            return buildRow(tuple._1, pbf.getGridSrid(), data, parameter.getLayerName(), parameter.getEdition());
        }
    }

    /** V2 Boundary 瓦片写库行构建任务。 */
    public static class BuildRowBoundaryFunction implements org.apache.spark.api.java.function.Function<Tuple2<String, PbfInfo>, Row> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;

        public BuildRowBoundaryFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Row call(Tuple2<String, PbfInfo> tuple) {
            PbfInfo pbf = tuple._2;
            byte[] data = pbf.getDataBoundary();
            pbf.setDataBoundary(null);
            return buildRow(tuple._1, pbf.getGridSrid(), data, parameter.getLayerNameBoundary(), parameter.getEdition());
        }
    }

    /** V2 Label 瓦片写库行构建任务。 */
    public static class BuildRowLabelFunction implements org.apache.spark.api.java.function.Function<Tuple2<String, PbfInfo>, Row> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;

        public BuildRowLabelFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Row call(Tuple2<String, PbfInfo> tuple) {
            PbfInfo pbf = tuple._2;
            byte[] data = pbf.getDataLabel();
            pbf.setDataLabel(null);
            return buildRow(tuple._1, pbf.getGridSrid(), data, parameter.getLayerNameLabel(), parameter.getEdition());
        }
    }

    private static Row buildRow(String tileId, int gridSrid, byte[] data, String layerName, String edition) {
        TileZxyApo zxy = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileId);
        int tmsY = gridSrid == 3857
                ? GirGeoTools.defaultInstance().getTileGrid3857Opt().convertY(zxy.getZ(), zxy.getY(), TileYAxis.XYZ, TileYAxis.TMS)
                : GirGeoTools.defaultInstance().getTileGrid4326SeparateOpt().convertY(zxy.getZ(), zxy.getY(), TileYAxis.XYZ, TileYAxis.TMS);
        return RowFactory.create(zxy.getZ(), zxy.getX(), tmsY, zxy.getY(), gridSrid, data, layerName, edition);
    }
}
