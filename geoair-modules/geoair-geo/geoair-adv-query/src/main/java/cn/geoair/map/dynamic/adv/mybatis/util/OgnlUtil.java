package cn.geoair.map.dynamic.adv.mybatis.util;

import ognl.Ognl;
import ognl.OgnlException;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OGNL 表达式求值工具类。
 *
 * <p>封装了 OGNL 库的常用操作，提供对 {@code Map<String, Object>} 上下文的表达式求值能力。 主要用于 MyBatis 动态 SQL 中 {@code <if
 * test="...">} 和 {@code ${}} / {@code #{}} 的参数解析。
 *
 * <p><b>安全提示：</b>OGNL 表达式可以执行任意 Java 方法调用。如果 SQL 模板来自不可信的用户输入， 必须对 {@code test} 属性和 {@code ${}}
 * 表达式进行严格的白名单校验，防止远程代码执行（RCE）。
 *
 * @author zhangjun
 */
public class OgnlUtil {

    private OgnlUtil() {}

    /**
     * 对 OGNL 表达式求值，返回结果对象。
     *
     * @param expression OGNL 表达式，如 "list.size() > 0"、"name"
     * @param root 求值上下文的根对象（变量名 → 值的映射）
     * @return 表达式求值结果，可能为 null
     * @throws RuntimeException 如果表达式语法错误或求值失败
     */
    @SuppressWarnings("unchecked")
    public static Object getValue(String expression, Map<String, Object> root) {
        try {
            Map<String, Object> context = Ognl.createDefaultContext(root);
            return Ognl.getValue(Ognl.parseExpression(expression), context, root);
        } catch (OgnlException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 对 OGNL 表达式求值并转换为布尔结果。
     *
     * <p>支持以下类型的自动转换：
     *
     * <ul>
     *   <li>{@link Boolean} — 直接返回
     *   <li>{@link Number} — 非零为 true
     *   <li>其他类型 — 抛出异常
     * </ul>
     *
     * @param expression OGNL 表达式
     * @param root 求值上下文
     * @return 布尔结果
     * @throws RuntimeException 如果结果无法转换为布尔值
     */
    public static Boolean getBooleanValue(String expression, Map<String, Object> root) {
        Object value = getValue(expression, root);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return !new BigDecimal(String.valueOf(value)).equals(BigDecimal.ZERO);
        } else {
            throw new RuntimeException(
                    "expression value is not boolean or number type: " + expression);
        }
    }

    /**
     * 对 OGNL 表达式求值并转换为可迭代对象。
     *
     * <p>支持以下类型的自动转换：
     *
     * <ul>
     *   <li>{@link Iterable} — 直接返回
     *   <li>数组（包括基本类型数组） — 转为 List
     *   <li>{@link Map} — 返回 entrySet()
     * </ul>
     *
     * @param expression OGNL 表达式，通常为集合变量名，如 "list"、"map.entrySet()"
     * @param root 求值上下文
     * @return 可迭代对象
     * @throws RuntimeException 如果结果为 null 或不可迭代
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Iterable<?> getIterable(String expression, Map<String, Object> root) {
        Object value = getValue(expression, root);
        if (value == null) {
            throw new RuntimeException(
                    "The expression '" + expression + "' evaluated to a null value.");
        }
        if (value instanceof Iterable) {
            return (Iterable<?>) value;
        }
        if (value.getClass().isArray()) {
            int size = Array.getLength(value);
            List<Object> answer = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                answer.add(Array.get(value, i));
            }
            return answer;
        }
        if (value instanceof Map) {
            return ((Map) value).entrySet();
        }
        throw new RuntimeException(
                "Error evaluating expression '"
                        + expression
                        + "'. Return value ("
                        + value
                        + ") was not iterable.");
    }
}
