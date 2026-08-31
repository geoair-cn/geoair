package cn.geoair.map.dynamic.adv.mybatis.node;

import java.util.Arrays;

/**
 * SET 子句节点，继承自 {@link TrimSqlNode}。
 *
 * <p>自动去除 SQL 尾部多余的逗号，并在内容非空时添加 "SET " 前缀。
 *
 * <p>示例：
 *
 * <pre>
 *   &lt;set&gt;
 *     &lt;if test="name != null"&gt;name = #{name},&lt;/if&gt;
 *     &lt;if test="age != null"&gt;age = #{age},&lt;/if&gt;
 *   &lt;/set&gt;
 *   -- 当 name 有值、age 无值时生成：SET name = ?
 *   -- 当都无值时生成：空字符串
 * </pre>
 *
 * @author zhangjun
 */
public class SetSqlNode extends TrimSqlNode {

    public SetSqlNode(SqlNode contents) {
        super(contents, "SET ", null, null, Arrays.asList(","));
    }
}
