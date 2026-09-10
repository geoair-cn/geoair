package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.geoair.map.dynamic.mvt.tools.model.VecConstant;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.VectorTileCommonUtils;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.IdUtil;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** V3 专用 Spark 可序列化函数。 */
final class V3SparkTaskFunctions {

    private V3SparkTaskFunctions() {
    }

    /** V3 图层分页读取任务。 */
    static class IdPageFlatMapFunction implements FlatMapFunction<Integer, GirAdvOneRow> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;
        private final String queryStatement;
        private final String orderFieldName;
        private final int countPerTask;

        IdPageFlatMapFunction(TileSliceParameter parameter, String queryStatement,
                String orderFieldName, int countPerTask) {
            this.parameter = parameter;
            this.queryStatement = queryStatement;
            this.orderFieldName = orderFieldName;
            this.countPerTask = countPerTask;
        }

        @Override
        public Iterator<GirAdvOneRow> call(Integer pageNum) {
            DataSourceConfig source = parameter.getInputSource();
            IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(source.toDataSource());
            String orderSql = executor.pBuildSqlWithOrder(queryStatement,
                    ListUtil.of(OrderApo.create(orderFieldName, AdvEnumsOrder.升序)));
            return executor.bSelectList(executor.pBuildPageSql(orderSql, countPerTask, pageNum, true)).iterator();
        }
    }

    /** V3 图层 BBOX 分片读取任务。 */
    static class BboxFlatMapFunction implements FlatMapFunction<String, GirAdvOneRow> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;
        private final String queryStatement;
        private final String geomFieldName;
        private final int sourceDataSrid;

        BboxFlatMapFunction(TileSliceParameter parameter, String queryStatement,
                String geomFieldName, int sourceDataSrid) {
            this.parameter = parameter;
            this.queryStatement = queryStatement;
            this.geomFieldName = geomFieldName;
            this.sourceDataSrid = sourceDataSrid;
        }

        @Override
        public Iterator<GirAdvOneRow> call(String condition) {
            String[] coords = condition.split(",");
            String sql = V3DataReadUtils.buildBboxQuerySql(queryStatement, geomFieldName,
                    Double.parseDouble(coords[0]), Double.parseDouble(coords[2]),
                    Double.parseDouble(coords[1]), Double.parseDouble(coords[3]), sourceDataSrid);
            IAdvExecutor executor = AdvExecutorFactory.getAdvExecutorByDataSource(parameter.getInputSource().toDataSource());
            return executor.bSelectList(sql).iterator();
        }
    }

    /** 将一个 V3 图层的要素按瓦片映射为图层分组 value。 */
    static class LayerMapToTileFunction
            implements PairFlatMapFunction<GirAdvOneRow, String, V3TileFeatureGroup>, Serializable {

        private static final long serialVersionUID = 1L;
        private final String layerName;
        private final TileSliceParameter readParameter;

        LayerMapToTileFunction(String layerName, TileSliceParameter readParameter) {
            this.layerName = layerName;
            this.readParameter = readParameter;
        }

        @Override
        public Iterator<Tuple2<String, V3TileFeatureGroup>> call(GirAdvOneRow row) {
            if (row == null) {
                return Collections.emptyIterator();
            }
            Iterator<Tuple2<String, List<GirAdvOneRow>>> tiles =
                    VectorTileCommonUtils.mapSingleFeatureToTilesStream(
                            row,
                            readParameter.getGeomFieldName(),
                            readParameter.getMinZoom(),
                            readParameter.getMaxZoom(),
                            readParameter.getOutGridSrid());
            List<Tuple2<String, V3TileFeatureGroup>> result = new ArrayList<>();
            while (tiles.hasNext()) {
                Tuple2<String, List<GirAdvOneRow>> tile = tiles.next();
                result.add(new Tuple2<>(tile._1, V3TileFeatureGroup.single(layerName, tile._2)));
            }
            return result.iterator();
        }
    }

    /** 按 tileId 合并多个图层的要素分组。 */
    static class MergeTileFeatureGroupFunction
            implements Function2<V3TileFeatureGroup, V3TileFeatureGroup, V3TileFeatureGroup>, Serializable {

        private static final long serialVersionUID = 1L;
        private final Map<String, MvtLayerSliceParameter> layersByName;
        private final int outGridSrid;

        MergeTileFeatureGroupFunction(List<MvtLayerSliceParameter> layers, int outGridSrid) {
            this.layersByName = new LinkedHashMap<>();
            for (MvtLayerSliceParameter layer : layers) {
                this.layersByName.put(layer.getLayerName(), layer);
            }
            this.outGridSrid = outGridSrid;
        }

        @Override
        public V3TileFeatureGroup call(V3TileFeatureGroup left, V3TileFeatureGroup right) {
            return left.merge(right, layersByName, outGridSrid);
        }
    }

    /** V3 图层要素的坐标转换任务。 */
    static class TransformFeatureFunction
            implements org.apache.spark.api.java.function.Function<GirAdvOneRow, GirAdvOneRow> {
        private static final long serialVersionUID = 1L;
        private final TileSliceParameter parameter;

        TransformFeatureFunction(TileSliceParameter parameter) {
            this.parameter = parameter;
        }

        @Override
        public GirAdvOneRow call(GirAdvOneRow row) {
            row.put(VecConstant.FeatureRowID, IdUtil.fastSimpleUUID());
            return VectorTileCommonUtils.transformSingleFeature(row, parameter);
        }
    }

    static TransformFeatureFunction newTransformFunction(TileSliceParameter parameter) {
        return new TransformFeatureFunction(parameter);
    }
}
