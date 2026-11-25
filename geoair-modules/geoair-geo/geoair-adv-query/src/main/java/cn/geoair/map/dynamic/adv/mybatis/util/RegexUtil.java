package cn.geoair.map.dynamic.adv.mybatis.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则替换工具类，用于动态 SQL 中 {@code <foreach>} 标签的参数名替换。
 * <p>
 * 将 SQL 片段中的 {@code item} / {@code index} 变量名替换为实际的集合索引访问表达式。
 * 例如：将 {@code item.name} 替换为 {@code list[0].name}。
 *
 * <p>替换规则：保留前导空白，只替换开头的变量名。变量名后须为属性、下标、参数选项分隔符、
 * 空白或字符串结尾，避免把 {@code itemName} 当作 {@code item}。
 *
 * @author zhangjun
 */
public class RegexUtil {

    private RegexUtil() {
    }

    /**
     * 将 content 中开头出现的 item 替换为 newItem。
     *
     * @param content 原始 SQL 片段（如 {@code item.name}）
     * @param item    要替换的变量名（如 {@code item}）
     * @param newItem 替换后的表达式（如 {@code list[0]}）
     * @return 替换后的字符串
     */
    public static String replace(String content, String item, String newItem) {
        return content.replaceFirst(
                "^(\\s*)" + Pattern.quote(item) + "(?=$|[.,:\\s\\[])",
                "$1" + Matcher.quoteReplacement(newItem));
    }
}
