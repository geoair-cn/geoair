package cn.geoair.map.dynamic.statics.mvt.v4.input;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

/**
 * V4 输入读取的行消费回调。
 *
 * <p>用回调而不是返回 {@code List}：GeoJSON 文件可能有几十万到上千万要素，
 * 一次性收集成列表会直接吃掉堆；回调式让读取层始终只持有当前一行。</p>
 *
 * @author 张逢吉
 */
@FunctionalInterface
public interface V4RowConsumer {

    /** 消费一行；读取层保证不会传 null。 */
    void accept(GirAdvOneRow row) throws Exception;
}
