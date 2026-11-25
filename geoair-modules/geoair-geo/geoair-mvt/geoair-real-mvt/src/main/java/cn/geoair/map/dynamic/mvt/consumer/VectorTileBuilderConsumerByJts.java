package cn.geoair.map.dynamic.mvt.consumer;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;
import cn.geoair.map.dynamic.mvt.tools.PipelineBuilder;

import no.ecc.vectortile.VectorTileEncoder;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** 基于Consumer模式的VectorTile PBF构建器 */
public class VectorTileBuilderConsumerByJts extends VectorTileBuilderConsumer {
    public static GiLogger log = GirLoggerFactory.getLogger();
    // 瓦片范围
    private final Envelope envelope;

    private final TileGlobalConfig tileGlobalConfig;

    // 图层名称
    private final String layerName;

    private boolean hasFeature = false;

    VectorTileEncoder encoder = new VectorTileEncoder(4096, 8, false);

    PipelineBuilder pipelineBuilder;

    private VectorTileBuilderConsumerByJts(
            Envelope envelope,
            String layerName,
            int outGridSrid,
            TileGlobalConfig tileGlobalConfig) {
        this.envelope = envelope;
        this.tileGlobalConfig = tileGlobalConfig;
        this.layerName = layerName;
        try {
            pipelineBuilder = PipelineBuilder.newBuilder(envelope, outGridSrid);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    /**
     * 创建Builder实例*
     *
     * @param layerName 图层名称
     * @return VectorTileBuilder
     */
    public static VectorTileBuilderConsumerByJts create(
            Envelope envelope,
            String layerName,
            int outGridSrid,
            TileGlobalConfig tileGlobalConfig) {
        return new VectorTileBuilderConsumerByJts(
                envelope, layerName, outGridSrid, tileGlobalConfig);
    }

    /** 核心方法：消费单个要素（实现Consumer接口） 逐个推送要素到Builder，无需一次性加载所有 */
    @Override
    public void accept(GirAdvOneRow fe) {
        buildOne(fe);
    }

    public void buildOne(GirAdvOneRow feature) {
        if (customTransformConsumer != null) {
            customTransformConsumer.accept(feature, tileGlobalConfig);
        }
        hasFeature = true;
        // 获取几何对象
        Geometry geom = (Geometry) feature.get("geom");
        if (geom == null || geom.isEmpty()) return;
        Map<String, Object> featureCopy = new HashMap<>();
        Set<Map.Entry<String, Object>> entries = feature.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            Object value = entry.getValue();
            if (value == null) {
                value = "";
            }
            featureCopy.put(entry.getKey(), value);
        }
        featureCopy.remove("geom");
        Geometry transform = pipelineBuilder.transform(geom);
        encoder.addFeature(layerName, featureCopy, transform);
    }

    /** 构建最终的PBF字节数组 所有要素推送完成后调用此方法 */
    public byte[] build() {
        // 没有要素的时候，就直接返回，不走下面的逻辑
        if (!hasFeature) {
            return new byte[0];
        }
        return encoder.encode();
    }
}
