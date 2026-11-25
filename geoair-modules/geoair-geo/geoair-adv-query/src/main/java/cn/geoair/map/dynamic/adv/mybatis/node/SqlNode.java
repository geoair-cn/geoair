package cn.geoair.map.dynamic.adv.mybatis.node;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;
import java.util.Set;

/**
 * 动态 SQL 节点接口，是整个动态 SQL 引擎的核心抽象。
 *
 * <p>SQL 模板被解析为一棵 SqlNode 树，每个节点负责生成 SQL 片段。
 * 节点类型包括：文本节点、条件节点（if）、循环节点（foreach）、修剪节点（trim/where/set）等。
 *
 * <p>定义了两个核心方法：
 *
 * <ul>
 *   <li>{@link #apply(Context)} — 执行 SQL 生成，将结果追加到 Context
 *   <li>#applyParameter(Set)} — 静态分析，提取模板中引用的参数名（不执行条件判断）
 * </ul>
 *
 * @see MixedSqlNode
 * @see TextSqlNode
 * @see IfSqlNode
 * @see ForeachSqlNode
 * @see TrimSqlNode
 * @author zhangjun
 */
public interface SqlNode {

    /**
     * 执行 SQL 生成，将本节点的 SQL 片段追加到上下文。
     *
     * <p>执行过程中会：
     *
     * <ul>
     *   <li>求值 {@code ${}} 表达式并替换为常量值
     *   <li>保留 {@code #{}} 表达式不变（后续由 {@link cn.geoair.map.dynamic.adv.mybatis.DynamicSqlEngine}
     *       统一替换为 {@code ?}）
     *   <li>根据条件判断是否包含子节点（if 标签）
     *   <li>展开循环（foreach 标签）
     * </ul>
     *
     * @param context SQL 生成上下文
     */
    void apply(Context context);

    /**
     * 静态分析：提取本节点模板中引用的所有参数名。
     *
     * <p>不执行条件判断和循环展开，仅从模板文本中提取 {@code ${}} 和 {@code #{}} 中的表达式名。 用于提前获知 SQL 模板依赖哪些参数。
     *
     * @param set 参数名集合，方法将参数名添加到此集合中
     */
    void applyParameter(Set<String> set);
}
