package cn.geoair.map.dynamic.mvt.consumer;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import java.util.function.Consumer;
import lombok.Setter;
 

/** 基于Consumer模式的VectorTile PBF构建器（流式推送要素，低内存占用） */
 
public abstract class VectorTileBuilderConsumer implements Consumer<GirAdvOneRow> {
    public static GiLogger log = GirLoggerFactory.getLogger();
    @Setter protected CustomTransformConsumer customTransformConsumer;

    /** 构建最终的PBF字节数组 所有要素推送完成后调用此方法 */
    public abstract byte[] build();
}
