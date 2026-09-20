package cn.geoair.map.dynamic.statics.mvt.v4.group;

import java.util.List;

/**
 * 逐瓦片回调"最终交给编码器的行"。
 *
 * <p>与 {@link V4TileConsumer} 的区别：那个回调拿到的是已经重建好的聚合单元
 * （属性行与序号已经分离），这一个拿到的是带输入序号的原始行，因此能回答
 * "这块瓦片里的这一行是输入里的第几个要素" —— 统计去重需要这个信息。</p>
 *
 * <p>回调时机在削减与排序<b>之后</b>：这些行就是"这块瓦片里到底有哪些要素"的唯一定义，
 * 据此统计既不会把被削掉的行算进来，也不会漏掉排序后改过位置的行。</p>
 *
 * <p>只有旁路消费者需要它（目前是 V4 的瓦片统计）。不需要时传 {@code null}，
 * 聚合容器不做任何额外工作。</p>
 *
 * @author 张逢吉
 */
@FunctionalInterface
public interface V4EmittedRowListener {

    /**
     * @param layerName MVT 内部图层名
     * @param rows      本图层在这块瓦片里最终保留的行（按 {@code sequence} 去重即可得到源要素）
     */
    void onRows(String layerName, List<V4OrderedRow> rows);
}
