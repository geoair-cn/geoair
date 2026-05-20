package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Bean转QueryFilter工具类
 * <p>将JavaBean对象自动转换为查询条件</p>
 *
 * @author zhangjun
 */
public class BeanToQueryFilterConverter {

    /**
     * 将Bean转换为QueryFilter
     *
     * @param bean Bean对象
     * @return QueryFilter实例
     */
    public static GirAdvWhereFilter convert(Object bean) {
        return convert(bean, ConvertOptions.defaultOptions());
    }

    /**
     * 将Bean转换为QueryFilter
     *
     * @param bean    Bean对象
     * @param options 转换配置选项
     * @return QueryFilter实例
     */
    public static GirAdvWhereFilter convert(Object bean, ConvertOptions options) {
        GirAdvWhereFilter filter = GirAdvWhereFilter.of();

        if (bean == null) {
            return filter;
        }

        // 提取字段值
        Map<String, Object> fieldMap = extractFieldValues(bean, options);

        // 应用字段映射
        if (options.hasFieldMappings()) {
            applyFieldMappings(filter, bean, options);
        } else {
            // 默认转换为等值条件
            applyDefaultConditions(filter, fieldMap, options);
        }

        return filter;
    }

    /**
     * 将Bean转换为QueryFilter（带字段映射）
     *
     * @param bean          Bean对象
     * @param fieldMappings 字段映射配置
     * @return QueryFilter实例
     */
    public static GirAdvWhereFilter convertWithMapping(Object bean, Map<String, FieldMapping> fieldMappings) {
        return convertWithMapping(bean, fieldMappings, ConvertOptions.defaultOptions());
    }

    /**
     * 将Bean转换为QueryFilter（带字段映射）
     *
     * @param bean          Bean对象
     * @param fieldMappings 字段映射配置
     * @param options       转换配置选项
     * @return QueryFilter实例
     */
    public static GirAdvWhereFilter convertWithMapping(Object bean,
                                                       Map<String, FieldMapping> fieldMappings,
                                                       ConvertOptions options) {
        GirAdvWhereFilter filter = GirAdvWhereFilter.of();

        if (bean == null || fieldMappings == null || fieldMappings.isEmpty()) {
            return filter;
        }

        for (Map.Entry<String, FieldMapping> entry : fieldMappings.entrySet()) {
            String beanFieldName = entry.getKey();
            FieldMapping mapping = entry.getValue();

            // 获取字段值
            Object value = getFieldValue(bean, beanFieldName);

            // 处理null值
            if (value == null) {
                if (mapping.isIgnoreNull() || options.isIgnoreNull()) {
                    continue;
                }
                if (options.isThrowOnNull()) {
                    throw new IllegalArgumentException("Field '" + beanFieldName + "' value is null");
                }
            }

            // 处理空字符串
            if (options.isIgnoreEmptyString() && value instanceof String && StrUtil.isBlank((String) value)) {
                continue;
            }

            // 确定列名
            String columnName = mapping.getColumnName() != null ? mapping.getColumnName() : beanFieldName;
            if (options.isToUnderlineCase() && mapping.getColumnName() == null) {
                columnName = toUnderlineCase(beanFieldName);
            }

            // 确定操作符
            AdvOperatorEnums operator = mapping.getOperator() != null ? mapping.getOperator() : AdvOperatorEnums.等于;

            // 处理特殊操作符的值
            Object processedValue = processValueByOperator(value, operator);

            // 添加条件
            filter.addCondition(columnName, operator, processedValue);
        }

        return filter;
    }

    /**
     * 提取Bean中的所有字段值
     */
    public static Map<String, Object> extractFieldValues(Object bean, ConvertOptions options) {
        Map<String, Object> fieldMap = new LinkedHashMap<>();

        if (bean instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) bean;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = options.isToUnderlineCase() ? toUnderlineCase(entry.getKey()) : entry.getKey();
                fieldMap.put(key, entry.getValue());
            }
        } else {
            fieldMap = GirAdvSqlUtils.getRowData(bean, options.isToUnderlineCase(), options.isIgnoreNull(), ListUtil.empty());
        }

        return fieldMap;
    }

    /**
     * 应用默认条件（等值条件）
     */
    private static void applyDefaultConditions(GirAdvWhereFilter filter,
                                               Map<String, Object> fieldMap,
                                               ConvertOptions options) {
        for (Map.Entry<String, Object> entry : fieldMap.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            // 处理null值
            if (value == null) {
                if (options.isIgnoreNull()) {
                    continue;
                }
                if (options.isThrowOnNull()) {
                    throw new IllegalArgumentException("Field '" + fieldName + "' value is null");
                }
            }

            // 处理空字符串
            if (options.isIgnoreEmptyString() && value instanceof String && StrUtil.isBlank((String) value)) {
                continue;
            }

            // 处理集合为空
            if (options.isIgnoreEmptyCollection() && value instanceof Collection && ((Collection<?>) value).isEmpty()) {
                continue;
            }

            // 确定操作符（根据值类型智能选择）
            AdvOperatorEnums operator = detectOperator(value, options);

            // 处理特殊操作符的值
            Object processedValue = processValueByOperator(value, operator);

            // 添加条件
            filter.addCondition(fieldName, operator, processedValue);
        }
    }

    /**
     * 应用字段映射
     */
    private static void applyFieldMappings(GirAdvWhereFilter filter, Object bean, ConvertOptions options) {
        Map<String, FieldMapping> fieldMappings = options.getFieldMappings();
        if (fieldMappings == null || fieldMappings.isEmpty()) {
            applyDefaultConditions(filter, extractFieldValues(bean, options), options);
            return;
        }

        for (Map.Entry<String, FieldMapping> entry : fieldMappings.entrySet()) {
            String beanFieldName = entry.getKey();
            FieldMapping mapping = entry.getValue();

            Object value = getFieldValue(bean, beanFieldName);

            if (value == null) {
                if (mapping.isIgnoreNull() || options.isIgnoreNull()) {
                    continue;
                }
            }

            String columnName = mapping.getColumnName() != null ? mapping.getColumnName() : beanFieldName;
            if (options.isToUnderlineCase() && mapping.getColumnName() == null) {
                columnName = toUnderlineCase(beanFieldName);
            }

            AdvOperatorEnums operator = mapping.getOperator() != null ? mapping.getOperator() : AdvOperatorEnums.等于;
            Object processedValue = processValueByOperator(value, operator);

            filter.addCondition(columnName, operator, processedValue);
        }
    }

    /**
     * 根据值类型智能检测操作符
     *
     * @param value   值
     * @param options 转换配置选项
     * @return 操作符枚举
     */
    private static AdvOperatorEnums detectOperator(Object value, ConvertOptions options) {
        if (value == null) {
            return AdvOperatorEnums.IS_NULL;
        }

        // 集合类型 -> IN
        if (value instanceof Collection || value instanceof Object[]) {
            return AdvOperatorEnums.IN;
        }

        // 字符串类型 -> 智能判断
        if (value instanceof String) {
            String str = (String) value;

            // 检查通配符位置，智能选择LIKE类型
            boolean hasLeftWildcard = str.startsWith("%");
            boolean hasRightWildcard = str.endsWith("%");


            // 情况1：用户已经指定了通配符模式
            if (hasLeftWildcard && hasRightWildcard) {
                // %xxx% -> 全模糊
                return AdvOperatorEnums.LIKE_ALL;
            } else if (hasLeftWildcard) {
                // %xxx -> 右模糊
                return AdvOperatorEnums.LIKE_RIGHT;
            } else if (hasRightWildcard) {
                // xxx% -> 左模糊
                return AdvOperatorEnums.LIKE_LEFT;
            }

            // 情况2：没有通配符，根据配置决定
            if (options.isAutoLike() && str.length() >= options.getAutoLikeThreshold()) {
                // 长字符串自动转为全模糊
                return AdvOperatorEnums.LIKE_ALL;
            }
        }

        // 默认等值查询
        return AdvOperatorEnums.等于;
    }

    /**
     * 根据操作符处理值
     */
    private static Object processValueByOperator(Object value, AdvOperatorEnums operator) {
        if (value == null) {
            return null;
        }

        switch (operator) {
            case BETWEEN:
                if (value instanceof Object[] && ((Object[]) value).length == 2) {
                    return value;
                }
                if (value instanceof Collection && ((Collection<?>) value).size() == 2) {
                    return ((Collection<?>) value).toArray();
                }
                return value;
            case IN:
                if (value instanceof Collection || value instanceof Object[]) {
                    return value;
                }
                return Collections.singletonList(value);
            default:
                return value;
        }
    }

    /**
     * 获取Bean中指定字段的值
     */
    private static Object getFieldValue(Object bean, String fieldName) {
        try {
            return BeanUtil.getProperty(bean, fieldName);
        } catch (Exception e) {
            // 尝试直接获取字段
            try {
                Field field = bean.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(bean);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * 驼峰转下划线
     */
    private static String toUnderlineCase(String camelCase) {
        if (camelCase == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    result.append("_");
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    // ==================== 内部类 ====================


}
