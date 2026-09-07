package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.TileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;

/**
 * 将 V3 单图层读取配置映射为内部读取适配参数。
 * <p>
 * 适配器仅复用 V1/V2 已验证的“读取、坐标转换、瓦片覆盖计算”函数；它不会回写调用方传入的
 * V1/V2 参数，也不会参与 V1/V2 的生成流程。
 *
 * @author 张逢吉
 */
final class V3LegacyParameterAdapter {

    private V3LegacyParameterAdapter() {
    }

    static TileSliceParameter toReadParameter(
            MultiLayerTileSliceParameter task, MvtLayerSliceParameter layer) {
        return new TileSliceParameter()
                .setInputSource(layer.getInputSource())
                .setGeomFieldName(layer.getGeomFieldName())
                .setIdFieldName(layer.getIdFieldName())
                .setQueryStatement(layer.getQueryStatement())
                .setLayerName(layer.getLayerName())
                .setSourceDataSrid(layer.getSourceDataSrid())
                .setOutGridSrid(task.getOutGridSrid())
                .setReadStrategy(layer.getReadStrategy())
                .setMaxPartionNum(layer.getMaxPartionNum())
                .setMinZoom(layer.getMinZoom() == null ? task.getMinZoom() : layer.getMinZoom())
                .setMaxZoom(layer.getMaxZoom() == null ? task.getMaxZoom() : layer.getMaxZoom());
    }
}
