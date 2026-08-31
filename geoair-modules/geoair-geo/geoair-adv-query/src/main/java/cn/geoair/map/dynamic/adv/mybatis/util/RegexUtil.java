package cn.geoair.map.dynamic.adv.mybatis.util;

import java.util.regex.Pattern;

/**
 * 正则替换工具类，用于动态 SQL 中 {@code <foreach>} 标签的参数名替换。
 *
 * <p>将 SQL 片段中的 {@code item} / {@code index} 变量名替换为实际的集合索引访问表达式。 例如：将 {@code item.name} 替换为 {@code
 * list[0].name}。
 *
 * <p>替换规则：仅匹配字符串开头的变量名，且变量名后不能紧跟标识符字符（字母、数字、下划线）， 以避免误替换（如 {@code itemName} 不会被替换）。
 *
 * @author zhangjun
 */
public class RegexUtil {

    private RegexUtil() {}

    /**
     * 将 content 中开头出现的 item 替换为 newItem。
     *
     * <p>使用正则 {@code ^\s*item(?![^.,:\s])} 进行匹配：
     *
     * <ul>
     *   <li>{@code ^\s*} — 允许开头有空白
     *   <li>{@code item} — 要替换的变量名
     *   <li>{@code (?![^.,:\s])} — 负向前瞻，变量名后不能紧跟标识符字符
     * </ul>
     *
     * @param content 原始 SQL 片段（如 {@code item.name}）
     * @param item 要替换的变量名（如 {@code item}）
     * @param newItem 替换后的表达式（如 {@code list[0]}）
     * @return 替换后的字符串
     */
    public static String replace(String content, String item, String newItem) {
        return content.replaceFirst(
                "^\\s*" + Pattern.quote(item) + "(?![^.,:\\s])", Matcher.quoteReplacement(newItem));
    }

    /** 对 {@link Matcher#quoteReplacement} 的便捷引用，用于转义替换字符串中的特殊字符。 */
    private static class Matcher {
        static String quoteReplacement(String s) {
            return java.util.regex.Matcher.quoteReplacement(s);
        }
    }
}
