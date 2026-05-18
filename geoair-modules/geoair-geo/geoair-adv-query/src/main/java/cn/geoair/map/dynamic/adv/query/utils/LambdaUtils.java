package cn.geoair.map.dynamic.adv.query.utils;

import cn.hutool.core.util.StrUtil;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Lambda表达式工具类
 * <p>用于从Lambda表达式中提取属性名称</p>
 * <p>使用示例：</p>
 * <pre>
 * String columnName = LambdaUtils.getColumnName(User::getName);
 * // 返回 "name"
 * </pre>
 *
 * @author 张俊
 * @date Created in 2026/5/18 19:49
 */
public class LambdaUtils {

    /**
     * 获取Lambda表达式对应的字段名
     *
     * @param function Lambda表达式（必须是Serializable类型）
     * @param <T>      实体类型
     * @param <R>      属性类型
     * @return 字段名
     */
    public static <T, R> String getColumnName(Function<T, R> function, boolean isToUnderlineCase) {
        if (function == null) {
            throw new IllegalArgumentException("Function cannot be null");
        }

        try {
            // 获取SerializedLambda
            SerializedLambda serializedLambda = getSerializedLambda(function);

            // 获取实现的方法名
            String implMethodName = serializedLambda.getImplMethodName();

            // 处理getter方法：getXxx -> xxx, isXxx -> xxx
            String fieldName;
            if (implMethodName.startsWith("get") && implMethodName.length() > 3) {
                fieldName = decapitalize(implMethodName.substring(3));
            } else if (implMethodName.startsWith("is") && implMethodName.length() > 2) {
                fieldName = decapitalize(implMethodName.substring(2));
            } else {
                // 如果不是标准的getter/is方法，直接返回方法名
                fieldName = implMethodName;
            }
            if (isToUnderlineCase) {
                return StrUtil.toUnderlineCase(fieldName);
            } else {
                return fieldName;
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to get column name from lambda: " + e.getMessage(), e);
        }
    }

//    /**
//     * 获取Lambda表达式对应的字段名（带自定义转换）
//     *
//     * @param function      Lambda表达式
//     * @param nameConverter 字段名转换器
//     * @param <T>           实体类型
//     * @param <R>           属性类型
//     * @return 转换后的字段名
//     */
//    public static <T, R> String getColumnName(Function<T, R> function, Function<String, String> nameConverter) {
//        String columnName = getColumnName(function);
//        if (nameConverter != null) {
//            columnName = nameConverter.apply(columnName);
//        }
//        return columnName;
//    }

//    /**
//     * 批量获取Lambda表达式对应的字段名
//     *
//     * @param functions Lambda表达式数组
//     * @param <T>       实体类型
//     * @param <R>       属性类型
//     * @return 字段名列表
//     */
//    @SafeVarargs
//    public static <T, R> List<String> getColumnNames(Function<T, R>... functions) {
//        if (functions == null || functions.length == 0) {
//            return Collections.emptyList();
//        }
//
//        List<String> columnNames = new ArrayList<>();
//        for (Function<T, R> function : functions) {
//            columnNames.add(getColumnName(function));
//        }
//        return columnNames;
//    }

    /**
     * 检查Function是否可序列化
     *
     * @param function Function对象
     * @return true=可序列化，false=不可序列化
     */
    public static boolean isSerializable(Function<?, ?> function) {
        try {
            getSerializedLambda(function);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取SerializedLambda对象
     *
     * @param function Function对象
     * @return SerializedLambda实例
     */
    private static SerializedLambda getSerializedLambda(Function<?, ?> function) {
        try {
            // 获取writeReplace方法
            Method writeReplaceMethod = function.getClass().getDeclaredMethod("writeReplace");
            writeReplaceMethod.setAccessible(true);

            // 调用writeReplace获取SerializedLambda
            return (SerializedLambda) writeReplaceMethod.invoke(function);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Function must be a lambda expression that is serializable. " +
                            "Make sure the lambda is properly typed.", e
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to get SerializedLambda: " + e.getMessage(), e);
        }
    }

    /**
     * 首字母小写（使用Introspector的decapitalize方法逻辑）
     *
     * @param name 原始字符串
     * @return 首字母小写后的字符串
     */
    private static String decapitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // 如果前两个字符都是大写，则不做处理（避免缩写词被错误转换）
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
                Character.isUpperCase(name.charAt(0))) {
            return name;
        }

        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }


}
