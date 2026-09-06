package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.tools.AdvMvtDensityUtils;
import cn.geoair.map.dynamic.mvt.tools.PipelineBuilder;
import cn.geoair.map.dynamic.mvt.tools.model.PbfInfo;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MultiLayerTileSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerGeometryMode;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.TileUtils;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import no.ecc.vectortile.VectorTileEncoder;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V3 单 PBF 多 MVT 内部图层编码器。
 * <p>
 * 同一个 {@link VectorTileEncoder} 被多个图层共享，因此编码结果始终是一个完整的 MVT Tile，
 * 而不是多个 PBF 字节数组的拼接。
 *
 * @author 张逢吉
 */
public final class MultiLayerMvtEncoderV3 {

    private MultiLayerMvtEncoderV3() {
    }

    /**
     * 编码一个 tileId 对应的全部内部图层。
     *
     * @param tileId Bing QuadKey
     * @param group 按内部图层归集的要素
     * @param parameter V3 任务参数
     * @return 可直接持久化的 gzip PBF
     */
    public static PbfInfo encode(
            String tileId, V3TileFeatureGroup group, MultiLayerTileSliceParameter parameter) throws Exception {
        TileZxyApo zxy = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileId);
        Envelope envelope = TileUtils.getTileEnvelope(
                zxy.getZ(), zxy.getX(), zxy.getY(), parameter.getOutGridSrid());
        Map<String, List<GirAdvOneRow>> features = group.copyFeaturesByLayer();
        byte[] encoded = encodeWithTileLimit(features, envelope, zxy.getZ(), parameter);
        return new PbfInfo().setData(encoded).setZoom(zxy.getZ()).setGridSrid(parameter.getOutGridSrid());
    }

    private static byte[] encodeWithTileLimit(
            Map<String, List<GirAdvOneRow>> features,
            Envelope envelope,
            int zoom,
            MultiLayerTileSliceParameter parameter) throws Exception {
        byte[] bytes = encodeAll(features, envelope, zoom, parameter);
        Long limit = parameter.isTileSizeLimitEnabled() ? parameter.getTileSizeLimitByte() : null;
        if (limit == null || limit <= 0 || bytes.length <= limit) {
            return bytes;
        }

        // 总大小超限时，按优先级从低到高裁剪。每次减半而非一次清空，尽可能保留低优先级图层。
        List<MvtLayerSliceParameter> candidates = new ArrayList<>(parameter.getLayers());
        candidates.sort(Comparator.comparingInt(MvtLayerSliceParameter::getPriority));
        boolean changed = true;
        while (bytes.length > limit && changed) {
            changed = false;
            for (MvtLayerSliceParameter layer : candidates) {
                List<GirAdvOneRow> rows = features.get(layer.getLayerName());
                if (rows == null || rows.size() <= 1) {
                    continue;
                }
                int targetSize = Math.max(1, rows.size() / 2);
                features.put(layer.getLayerName(), new ArrayList<>(rows.subList(0, targetSize)));
                bytes = encodeAll(features, envelope, zoom, parameter);
                changed = true;
                if (bytes.length <= limit) {
                    break;
                }
            }
        }
        return bytes;
    }

    private static byte[] encodeAll(
            Map<String, List<GirAdvOneRow>> features,
            Envelope envelope,
            int zoom,
            MultiLayerTileSliceParameter parameter) throws Exception {
        VectorTileEncoder encoder = new VectorTileEncoder(4096, 8, false);
        PipelineBuilder pipeline = PipelineBuilder.newBuilder(envelope, parameter.getOutGridSrid());
        for (MvtLayerSliceParameter layer : parameter.getLayers()) {
            List<GirAdvOneRow> rows = features.get(layer.getLayerName());
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            for (GirAdvOneRow row : rows) {
                Geometry geometry = getOutputGeometry(row, layer, envelope, zoom, parameter.getOutGridSrid());
                if (geometry == null || geometry.isEmpty()) {
                    continue;
                }
                Geometry screenGeometry = pipeline.transform(geometry);
                if (screenGeometry != null && !screenGeometry.isEmpty()) {
                    encoder.addFeature(layer.getLayerName(), getAttributes(row, layer), screenGeometry);
                }
            }
        }
        return AdvMvtDensityUtils.gZip(encoder.encode());
    }

    private static Geometry getOutputGeometry(
            GirAdvOneRow row,
            MvtLayerSliceParameter layer,
            Envelope envelope,
            int zoom,
            int outGridSrid) {
        Geometry geometry = row.getGeometry(layer.getGeomFieldName());
        if (geometry == null || geometry.isEmpty()) {
            return null;
        }
        MvtLayerGeometryMode mode = layer.getGeometryMode() == null
                ? MvtLayerGeometryMode.ORIGINAL : layer.getGeometryMode();
        if (mode == MvtLayerGeometryMode.CENTROID) {
            geometry = geometry.getCentroid();
        } else if (mode == MvtLayerGeometryMode.BOUNDARY) {
            geometry = geometry.getBoundary();
        }
        if (geometry == null || geometry.isEmpty()) {
            return null;
        }
        if (layer.getSimplificationLevel() != null && layer.getSimplificationLevel() > 0) {
            // 以瓦片宽度换算的保守容差，确保不同网格和层级下简化尺度一致。
            double tolerance = envelope.getWidth() / 4096D * layer.getSimplificationLevel();
            if (tolerance > 0) {
                geometry = TopologyPreservingSimplifier.simplify(geometry, tolerance);
            }
        }
        return geometry;
    }

    private static Map<String, Object> getAttributes(GirAdvOneRow row, MvtLayerSliceParameter layer) {
        Map<String, Object> attributes = new HashMap<>();
        boolean includeAll = layer.getIncludeFields() == null || layer.getIncludeFields().isEmpty();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            if (key.equals(layer.getGeomFieldName())) {
                continue;
            }
            boolean included = includeAll || layer.getIncludeFields().contains(key)
                    || (layer.getSysIncludeFields() != null && layer.getSysIncludeFields().contains(key));
            if (included) {
                attributes.put(key, entry.getValue() == null ? "" : entry.getValue());
            }
        }
        if (layer.getIdFieldName() != null && !layer.getIdFieldName().trim().isEmpty()) {
            attributes.put(layer.getIdFieldName(), row.get(layer.getIdFieldName()));
        }
        return attributes;
    }
}
