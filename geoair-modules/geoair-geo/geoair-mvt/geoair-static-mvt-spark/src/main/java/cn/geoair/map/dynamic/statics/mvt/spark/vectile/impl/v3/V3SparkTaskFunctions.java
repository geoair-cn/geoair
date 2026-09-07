package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.SparkTaskSerializableUtil;
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

    /** 复用已有且经过验证的坐标转换函数，V3 的聚合和编码仍完全独立。 */
    static SparkTaskSerializableUtil.TransformFeatureFunction newTransformFunction(
            TileSliceParameter parameter) {
        return new SparkTaskSerializableUtil.TransformFeatureFunction(parameter);
    }
}
