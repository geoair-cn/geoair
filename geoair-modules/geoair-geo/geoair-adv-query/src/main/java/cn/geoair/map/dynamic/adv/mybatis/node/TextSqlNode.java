package cn.geoair.map.dynamic.adv.mybatis.node;

import cn.geoair.map.dynamic.adv.mybatis.context.Context;
import cn.geoair.map.dynamic.adv.mybatis.token.TokenHandler;
import cn.geoair.map.dynamic.adv.mybatis.token.TokenParser;

import java.util.Set;

/**
 * 文本节点，包含一段纯 SQL 文本（可能包含 {@code ${}} 和 {@code #{}} 表达式）。
 *
 * <p>执行时：
 *
 * <ul>
 *   <li>{@code ${expression}} — 通过 OGNL 求值后直接替换为常量值（<b>存在 SQL 注入风险，仅用于可信输入</b>）
 *   <li>{@code #{expression}} — 保持不变，后续由引擎统一替换为 {@code ?} 占位符
 * </ul>
 *
 * @author zhangjun
 */
public class TextSqlNode implements SqlNode {

    private final String text;

    public TextSqlNode(String text) {
        this.text = text;
    }

    /**
     * 解析 ${} 表达式并追加到上下文。
     *
     * <p><b>安全提示：</b>${} 使用原始字符串拼接，无转义。确保表达式来源可信。
     */
    @Override
    public void apply(Context context) {
        TokenParser tokenParser =
                new TokenParser(
                        "${",
                        "}",
                        new TokenHandler() {
                            @Override
                            public String handleToken(String paramName) {
                                Object value = context.getOgnlValue(paramName);
                                return value == null ? "" : value.toString();
                            }
                        });
        context.appendSql(tokenParser.parse(text));
    }

    /** 提取文本中 ${} 和 #{} 表达式中的参数名。 */
    @Override
    public void applyParameter(Set<String> set) {
        TokenParser dollarParser =
                new TokenParser(
                        "${",
                        "}",
                        new TokenHandler() {
                            @Override
                            public String handleToken(String paramName) {
                                set.add(paramName);
                                return paramName;
                            }
                        });
        String resolved = dollarParser.parse(text);

        TokenParser hashParser =
                new TokenParser(
                        "#{",
                        "}",
                        new TokenHandler() {
                            @Override
                            public String handleToken(String paramName) {
                                set.add(paramName);
                                return paramName;
                            }
                        });
        hashParser.parse(resolved);
    }
}
