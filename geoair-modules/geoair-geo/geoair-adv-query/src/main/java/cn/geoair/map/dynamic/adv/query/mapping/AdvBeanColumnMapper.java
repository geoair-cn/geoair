package cn.geoair.map.dynamic.adv.query.mapping;

import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanMappingMeta.AdvBeanPropertyMeta;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.hutool.core.util.StrUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： bean到列值的映射器（写方向）
 */
public class AdvBeanColumnMapper {

    private final AdvTypeHandlerRegistry typeHandlerRegistry;

    public AdvBeanColumnMapper(AdvTypeHandlerRegistry typeHandlerRegistry) {
        this.typeHandlerRegistry = typeHandlerRegistry;
    }

    public Map<String, Object> toColumnValueMap(
            Object bean,
            boolean toUnderlineCase,
            boolean ignoreNullValue,
            boolean ignoreEmptyString,
            List<String> ignoreFieldNames) {
        Map<String, Object> rowData = new LinkedHashMap<>();
        if (bean == null) {
            return rowData;
        }
        AdvBeanMappingMeta mappingMeta = AdvBeanMappingMeta.of(bean.getClass());
        if (mappingMeta.isMapType()) {
            fillFromMap(
                    rowData,
                    (Map<?, ?>) bean,
                    bean.getClass(),
                    toUnderlineCase,
                    ignoreNullValue,
                    ignoreEmptyString,
                    ignoreFieldNames);
            return rowData;
        }
        for (AdvBeanPropertyMeta property : mappingMeta.getWritableProperties(ignoreFieldNames)) {
            Object value = property.readValue(bean);
            if (value == null && ignoreNullValue) {
                continue;
            }
            if (ignoreEmptyString && value instanceof String && ((String) value).trim().isEmpty()) {
                continue;
            }
            String columnName = property.resolveColumnName(toUnderlineCase);
            // 不在此处预转换：保留原始类型以便 buildPlaceholders 感知自定义占位符
            // 最终的 JDBC 转换由 PreparedStatementBinder 在执行时完成
            rowData.put(columnName, value);
        }
        return rowData;
    }

    private void fillFromMap(
            Map<String, Object> rowData,
            Map<?, ?> source,
            Class<?> beanType,
            boolean toUnderlineCase,
            boolean ignoreNullValue,
            boolean ignoreEmptyString,
            List<String> ignoreFieldNames) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String propertyName = String.valueOf(entry.getKey());
            if (shouldIgnore(propertyName, ignoreFieldNames)) {
                continue;
            }
            Object value = entry.getValue();
            if (value == null && ignoreNullValue) {
                continue;
            }
            if (ignoreEmptyString && value instanceof String && ((String) value).trim().isEmpty()) {
                continue;
            }
            String columnName =
                    toUnderlineCase ? StrUtil.toUnderlineCase(propertyName) : propertyName;
            // 不在此处预转换：保留原始类型以便 buildPlaceholders 感知自定义占位符
            // 最终的 JDBC 转换由 PreparedStatementBinder 在执行时完成
            rowData.put(columnName, value);
        }
    }

    private boolean shouldIgnore(String propertyName, List<String> ignoreFieldNames) {
        if (ignoreFieldNames == null || ignoreFieldNames.isEmpty()) {
            return false;
        }
        for (String ignore : ignoreFieldNames) {
            if (ignore != null && ignore.equalsIgnoreCase(propertyName)) {
                return true;
            }
        }
        return false;
    }
}
