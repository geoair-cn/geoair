package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;

/**
 * 将 V3 单图层配置映射为坐标转换和瓦片覆盖计算所需的内部参数。
 * <p>
 * 适配器不携带 JDBC 或 GeoJSON 输入参数，只复用既有的坐标转换和瓦片覆盖算法；
 * 它不会回写调用方参数，也不会参与 V1/V2 的生成流程。
 *
 * @author 张逢吉
 */
final class V3LegacyParameterAdapter {

    private V3LegacyParameterAdapter() {
    }

    static TileSliceParameter toReadParameter(
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
}
