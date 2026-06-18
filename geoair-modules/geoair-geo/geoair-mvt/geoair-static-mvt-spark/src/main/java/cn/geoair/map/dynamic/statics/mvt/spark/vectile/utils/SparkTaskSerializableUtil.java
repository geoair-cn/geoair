package cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.dialect.pg.AdvExecutorPG;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.mvt.tools.model.VecConstant;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.PbfTargetInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.PgConnectInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.PgConnectInfoBase;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import java.io.Serializable;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import scala.Tuple2;

/** 生成可序列化的spark的任务 */
@Slf4j
public class SparkTaskSerializableUtil implements Serializable {

    // 序列化ID（必须）
    private static final long serialVersionUID = 1L;

    /** ID分页读取的FlatMapFunction */
    public static class IdPageFlatMapFunction
            implements FlatMapFunction<Integer, GirAdvOneRow>, Serializable {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;

        private final String queryStatement;

        private final String orderFieldName;

        private final int countPerTask;

        public IdPageFlatMapFunction(
                TileSliceParameter parameter,
                String queryStatement,
                String orderFieldName,
                int countPerTask) {
            this.parameter = parameter;
            this.queryStatement = queryStatement;
            this.orderFieldName = orderFieldName;
            this.countPerTask = countPerTask;
        }

        @Override
        public Iterator<GirAdvOneRow> call(Integer pageNum) throws Exception {

            PgConnectInfoBase pgConnectInfo = parameter.getInputConnectInfo();
            IAdvExecutor iAdvExecutor = new AdvExecutorPG(pgConnectInfo.toDataSource());

            long startTime = System.currentTimeMillis();
            // 构建排序SQL
            String orderSql =
                    iAdvExecutor.pBuildSqlWithOrder(
                            queryStatement,
                            ListUtil.of(OrderApo.create(orderFieldName, AdvEnumsOrder.升序)));
            // 构建分页SQL
            String pageSql = iAdvExecutor.pBuildPageSql(orderSql, countPerTask, pageNum, true);
            // 读取当前页数据
            List<GirAdvOneRow> pageFeatures = iAdvExecutor.bSelectList(pageSql);

            // 打印耗时
            log.info(
                    "Spark ID分页任务"
                            + pageNum
                            + "耗时："
                            + (System.currentTimeMillis() - startTime)
                            + "ms，读取条数："
                            + pageFeatures.size());

            return pageFeatures.iterator();
        }
    }

    /** BBox读取的FlatMapFunction */
    public static class BboxFlatMapFunction
            implements FlatMapFunction<String, GirAdvOneRow>, Serializable {

        private static final long serialVersionUID = 1L;

        // 仅保存可序列化的参数
        private final TileSliceParameter parameter;

        private final String queryStatement;

        private final String geomFieldName;

        private final int sourceDataSrid;

        public BboxFlatMapFunction(
                TileSliceParameter parameter,
                String queryStatement,
                String geomFieldName,
                int sourceDataSrid) {
            this.parameter = parameter;
            this.queryStatement = queryStatement;
            this.geomFieldName = geomFieldName;
            this.sourceDataSrid = sourceDataSrid;
        }

        @Override
        public Iterator<GirAdvOneRow> call(String partitionCondition) throws Exception {

            PgConnectInfoBase pgConnectInfo = parameter.getInputConnectInfo();
            IAdvExecutor iAdvExecutor = new AdvExecutorPG(pgConnectInfo.toDataSource());

            // 解析分区范围
            String[] coords = partitionCondition.split(",");
            double xmin = Double.parseDouble(coords[0]);
            double xmax = Double.parseDouble(coords[1]);
            double ymin = Double.parseDouble(coords[2]);
            double ymax = Double.parseDouble(coords[3]);

            // 构建BBox查询SQL
            String bboxQuerySql =
                    DataReadCommonUtils.buildBboxQuerySql(
                            queryStatement, geomFieldName, xmin, ymin, xmax, ymax, sourceDataSrid);

            // 读取当前分片数据
            List<GirAdvOneRow> partitionFeatures = iAdvExecutor.bSelectList(bboxQuerySql);
            return partitionFeatures.iterator();
        }
    }

    /** 要素转换函数（可序列化） */
    public static class TransformFeatureFunction
            implements Serializable,
                    org.apache.spark.api.java.function.Function<GirAdvOneRow, GirAdvOneRow> {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;

        public TransformFeatureFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public GirAdvOneRow call(GirAdvOneRow girAdvOneRow) throws Exception {

            // 用于后期生成统计值的进行去重，不进入矢量瓦片生成逻辑中
            girAdvOneRow.put(VecConstant.FeatureRowID, IdUtil.fastSimpleUUID());

            return VectorTileCommonUtils.transformSingleFeature(girAdvOneRow, parameter);
        }
    }

    public static class AggregateAndLimitFeatureFunction
            implements Function2<List<GirAdvOneRow>, List<GirAdvOneRow>, List<GirAdvOneRow>>,
                    Serializable {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;

        public AggregateAndLimitFeatureFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public List<GirAdvOneRow> call(List<GirAdvOneRow> list1, List<GirAdvOneRow> list2)
                throws Exception {
            List<GirAdvOneRow> mergedList =
                    VectorTileCommonUtils.aggregateTileFeatures(list1, list2);
            return VectorTileCommonUtils.limitTileFeatures(mergedList, parameter);
        }
    }


    /** 瓦片映射函数（可序列化 + 无OOM版本） */
    public static class MapToTileFunction
            implements PairFlatMapFunction<GirAdvOneRow, String, List<GirAdvOneRow>>, Serializable {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;

        public MapToTileFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Iterator<Tuple2<String, List<GirAdvOneRow>>> call(GirAdvOneRow feature) throws Exception {
            if (feature == null) {
                return Collections.emptyIterator();
            }

            // 直接调用
            return VectorTileCommonUtils.mapSingleFeatureToTilesStream(
                    feature,
                    parameter.getGeomFieldName(),
                    parameter.getMinZoom(),
                    parameter.getMaxZoom(),
                    parameter.getOutGridSrid());
        }
    }
    /** 瓦片映射函数（可序列化）  */
    @Deprecated
    public static class MapToTileFunction1
            implements PairFlatMapFunction<GirAdvOneRow, String, List<GirAdvOneRow>>, Serializable {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;

        public MapToTileFunction1(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Iterator<Tuple2<String, List<GirAdvOneRow>>> call(GirAdvOneRow feature)
                throws Exception {
            if (feature == null) {
                return Collections.emptyIterator();
            }
            Map<String, List<GirAdvOneRow>> tileMap =
                    VectorTileCommonUtils.mapSingleFeatureToTiles(
                            feature,
                            parameter.getGeomFieldName(),
                            parameter.getMinZoom(),
                            parameter.getMaxZoom(),
                            parameter.getOutGridSrid());
            List<Tuple2<String, List<GirAdvOneRow>>> result = new ArrayList<>();
            tileMap.forEach(
                    (tileId, list) -> {
                        if (StrUtil.isNotBlank(tileId) && list != null && !list.isEmpty()) {
                            result.add(new Tuple2<>(tileId, list));
                        }
                    });
            return result.iterator();
        }
    }

    @Deprecated
    public static class MapToTileFunctionToStatic
            implements PairFlatMapFunction<GirAdvOneRow, String, List<GirAdvOneRow>>, Serializable {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;

        Integer zoom;

        public MapToTileFunctionToStatic(TileSliceParameter parameter, Integer zoom) {
            this.parameter = parameter;
            this.zoom = zoom;
        }

        @Override
        public Iterator<Tuple2<String, List<GirAdvOneRow>>> call(GirAdvOneRow feature)
                throws Exception {
            if (feature == null) {
                return Collections.emptyIterator();
            }
            Map<String, List<GirAdvOneRow>> tileMap =
                    VectorTileCommonUtils.mapSingleFeatureToTiles(
                            feature,
                            parameter.getGeomFieldName(),
                            zoom,
                            zoom,
                            parameter.getOutGridSrid());
            List<Tuple2<String, List<GirAdvOneRow>>> result = new ArrayList<>();
            tileMap.forEach(
                    (tileId, list) -> {
                        if (StrUtil.isNotBlank(tileId) && list != null && !list.isEmpty()) {
                            result.add(new Tuple2<>(tileId, list));
                        }
                    });
            return result.iterator();
        }
    }

    /** PBF生成函数（可序列化） */
    public static class GeneratePbfFunction
            implements Serializable,
                    org.apache.spark.api.java.function.Function<
                            Tuple2<String, List<GirAdvOneRow>>, Tuple2<String, PbfInfo>> {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;

        private final PbfTargetInfo pbfTargetInfo;

        public GeneratePbfFunction(TileSliceParameter parameter, PbfTargetInfo pbfTargetInfo) {
            this.parameter = parameter;
            this.pbfTargetInfo = pbfTargetInfo;
        }

        @Override
        public Tuple2<String, PbfInfo> call(Tuple2<String, List<GirAdvOneRow>> tileFeature)
                throws Exception {
            PbfInfo pbf = null;
            try {
                pbf =
                        VectorTileCommonUtils.generateSingleTilePbf(
                                tileFeature._1, tileFeature._2, parameter, pbfTargetInfo);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return new Tuple2<>(tileFeature._1, pbf);
        }
    }

    static int getTmsY(int zoom, int y, int x, int gridSrid) {
        int tms_y = y;
        if (gridSrid == 3857) {
            tms_y = GirGeoTools.defaultInstance().getTileGrid3857Opt().reverseY(y, zoom);
        } else {
            tms_y = GirGeoTools.defaultInstance().getTileGrid4326SeparateOpt().reverseY(y, zoom);
        }
        return tms_y;
    }

    /** Row构建函数（可序列化） */
    public static class BuildRowFunction
            implements Serializable,
                    org.apache.spark.api.java.function.Function<Tuple2<String, PbfInfo>, Row> {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;

        public BuildRowFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Row call(Tuple2<String, PbfInfo> tuple) throws Exception {
            String tileId = tuple._1;
            TileZxyApo tileZxyApo = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileId);
            int zoom = tileZxyApo.getZ();
            int y = tileZxyApo.getY();
            int x = tileZxyApo.getX();
            PbfInfo pbfInfo = tuple._2;
            byte[] bytes = pbfInfo.getData();
            pbfInfo.setData(null);
            int gridSrid = pbfInfo.getGridSrid();
            int tms_y = getTmsY(zoom, y, x, gridSrid);
            return RowFactory.create(
                    zoom,
                    x,
                    tms_y,
                    y,
                    gridSrid,
                    bytes,
                    parameter.getLayerName(),
                    parameter.getEdition());
        }
    }

    public static class BuildRowBoundaryFunction
            implements Serializable,
                    org.apache.spark.api.java.function.Function<Tuple2<String, PbfInfo>, Row> {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;

        public BuildRowBoundaryFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Row call(Tuple2<String, PbfInfo> tuple) throws Exception {

            String tileId = tuple._1;
            TileZxyApo tileZxyApo = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileId);

            int zoom = tileZxyApo.getZ();
            int y = tileZxyApo.getY();
            int x = tileZxyApo.getX();
            PbfInfo pbfInfo = tuple._2;
            byte[] bytes = pbfInfo.getDataBoundary();
            pbfInfo.setDataBoundary(null);
            int gridSrid = pbfInfo.getGridSrid();
            int tms_y = getTmsY(zoom, y, x, gridSrid);
            return RowFactory.create(
                    zoom,
                    x,
                    tms_y,
                    y,
                    gridSrid,
                    bytes,
                    parameter.getLayerNameBoundary(),
                    parameter.getEdition());
        }
    }

    public static class BuildRowLabelFunction
            implements Serializable,
                    org.apache.spark.api.java.function.Function<Tuple2<String, PbfInfo>, Row> {

        private static final long serialVersionUID = 1L;

        private final TileSliceParameter parameter;

        public BuildRowLabelFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public Row call(Tuple2<String, PbfInfo> tuple) throws Exception {

            String tileId = tuple._1;
            PbfInfo pbfInfo = tuple._2;
            byte[] bytes = pbfInfo.getDataLabel();
            pbfInfo.setDataLabel(null);
            TileZxyApo tileZxyApo = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileId);
            int zoom = tileZxyApo.getZ();
            int y = tileZxyApo.getY();
            int x = tileZxyApo.getX();
            int gridSrid = pbfInfo.getGridSrid();
            int tms_y = getTmsY(zoom, y, x, gridSrid);
            return RowFactory.create(
                    zoom,
                    x,
                    tms_y,
                    y,
                    gridSrid,
                    bytes,
                    parameter.getLayerNameLabel(),
                    parameter.getEdition());
        }
    }
}
