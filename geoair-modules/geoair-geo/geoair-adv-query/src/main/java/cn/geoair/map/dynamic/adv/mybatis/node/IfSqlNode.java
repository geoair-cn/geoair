package cn.geoair.map.dynamic.adv.mybatis.node;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;
import java.util.Set;

/**
 * 条件节点，对应 {@code <if test="...">} 标签。
 * <p>
 * 执行时通过 OGNL 求值 {@code test} 属性，结果为 true 时执行子节点内容。
 *
 * @author zhangjun
 */
public class IfSqlNode implements SqlNode {

    private final String test;
    private final SqlNode contents;

    public IfSqlNode(String test, SqlNode contents) {
        this.test = test;
        this.contents = contents;
    }

    /**
     * 求值 test 表达式，为 true 时执行子节点。
     */
    @Override
    public void apply(Context context) {
        Boolean value = context.getOgnlBooleanValue(test);
        if (value) {
            context.appendSql(" ");
            contents.apply(context);
        }
    }

    /**
     * 无论 test 结果如何，始终提取子节点的参数名（静态分析不执行条件判断）。
     */
    @Override
    public void applyParameter(Set<String> set) {
        contents.applyParameter(set);
    }
}
