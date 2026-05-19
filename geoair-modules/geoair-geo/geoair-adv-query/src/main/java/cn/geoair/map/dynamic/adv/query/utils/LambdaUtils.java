package cn.geoair.map.dynamic.adv.query.utils;

import cn.geoair.map.dynamic.adv.query.wherequery.SFunction;
import cn.hutool.core.util.StrUtil;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
     * 获取Lambda表达式对应的字段名（默认不转换）
     *
     * @param function Lambda表达式
     * @param <T>      实体类型
     * @param <R>      属性类型
     * @return 字段名
     */
    public static <T, R> String getColumnName(SFunction<T, R> function) {
        return getColumnName(function, false);
    }

    /**
     * 获取Lambda表达式对应的字段名
     *
     * @param function          Lambda表达式
     * @param isToUnderlineCase 是否转换为下划线命名（驼峰转下划线）
     * @param <T>               实体类型
     * @param <R>               属性类型
     * @return 字段名
     */
    public static <T, R> String getColumnName(SFunction<T, R> function, boolean isToUnderlineCase) {
        if (function == null) {
            throw new IllegalArgumentException("SFunction cannot be null");
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


    private static final Map<Class<?>, SerializedLambda> LAMBDA_CACHE = new ConcurrentHashMap<>();


    /**
     * 获取SerializedLambda对象（带缓存）
     *
     * @param function Function对象
     * @return SerializedLambda实例
     */
    private static SerializedLambda getSerializedLambda(SFunction<?, ?> function) {
        // 先从缓存中获取
        SerializedLambda cached = LAMBDA_CACHE.get(function.getClass());
        if (cached != null) {
            return cached;
        }

        try {
            // 获取writeReplace方法
            Method writeReplaceMethod = function.getClass().getDeclaredMethod("writeReplace");
            writeReplaceMethod.setAccessible(true);

            // 调用writeReplace获取SerializedLambda
            SerializedLambda serializedLambda = (SerializedLambda) writeReplaceMethod.invoke(function);

            // 放入缓存
            LAMBDA_CACHE.put(function.getClass(), serializedLambda);

            return serializedLambda;
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
