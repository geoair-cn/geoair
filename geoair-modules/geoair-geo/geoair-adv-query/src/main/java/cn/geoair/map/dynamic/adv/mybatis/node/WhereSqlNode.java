package cn.geoair.map.dynamic.adv.mybatis.node;

import java.util.Arrays;
import java.util.List;

/**
 * WHERE 子句节点，继承自 {@link TrimSqlNode}。
 *
 * <p>自动处理 SQL 首部多余的 AND/OR 关键字，并在内容非空时添加 "WHERE " 前缀。
 *
 * <p>示例：
 *
 * <pre>
 *   &lt;where&gt;
 *     &lt;if test="name != null"&gt;AND name = #{name}&lt;/if&gt;
 *     &lt;if test="age != null"&gt;AND age = #{age}&lt;/if&gt;
 *   &lt;/where&gt;
 *   -- 当 name 有值、age 无值时生成：WHERE name = ?
 *   -- 当都无值时生成：空字符串（不会生成 "WHERE"）
 * </pre>
 *
 * @author zhangjun
 */
public class WhereSqlNode extends TrimSqlNode {

    private static final List<String> PREFIXES_TO_OVERRIDE =
            Arrays.asList(
                    "AND ", "AND\r", "AND\t", "AND\n", "OR ", "OR\r", "OR\t", "OR\n", "and ",
                    "and\r", "and\t", "and\n", "or ", "or\r", "or\t", "or\n");

    public WhereSqlNode(SqlNode contents) {
        super(contents, "WHERE ", null, PREFIXES_TO_OVERRIDE, null);
    }
}
