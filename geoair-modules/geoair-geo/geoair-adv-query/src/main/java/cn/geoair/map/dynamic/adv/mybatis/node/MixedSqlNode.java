package cn.geoair.map.dynamic.adv.mybatis.node;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;
import java.util.List;
import java.util.Set;

/**
 * 组合节点，包含多个子 {@link SqlNode}，按顺序依次执行。
 *
 * <p>这是 SQL 节点树中的"容器"节点，对应 XML 中的父元素（如 {@code <root>}）。 执行时遍历所有子节点，依次调用其 {@link
 * SqlNode#apply(Context)} 方法。
 *
 * @author zhangjun
 */
public class MixedSqlNode implements SqlNode {

    private final List<SqlNode> contents;

    public MixedSqlNode(List<SqlNode> contents) {
        this.contents = contents;
    }

    @Override
    public void apply(Context context) {
        for (SqlNode node : contents) {
            node.apply(context);
        }
    }

    @Override
    public void applyParameter(Set<String> set) {
        for (SqlNode node : contents) {
            node.applyParameter(set);
        }
    }
}
