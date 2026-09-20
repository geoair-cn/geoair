package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.VectorTileCommonUtils;
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
            // 这里不写统计去重键：V3 没有统计阶段（StatisticUtils 只被 V1/V2 调用），
            // 写进去没人消费，只会被"未配置字段白名单=全字段输出"的多图层编码器
            // 当成普通属性写进 PBF（一块 8000 要素的瓦片里它占掉绝大部分体积）。
            return VectorTileCommonUtils.transformSingleFeature(row, parameter);
        }
    }

    static TransformFeatureFunction newTransformFunction(TileSliceParameter parameter) {
        return new TransformFeatureFunction(parameter);
    }
}
