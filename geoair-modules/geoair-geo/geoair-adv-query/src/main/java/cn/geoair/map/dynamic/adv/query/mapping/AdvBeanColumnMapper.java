package cn.geoair.map.dynamic.adv.query.mapping;

import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanMappingMeta.AdvBeanPropertyMeta;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： bean到列值的映射器
 */
public class AdvBeanColumnMapper {

    private final AdvTypeHandlerRegistry typeHandlerRegistry = AdvTypeHandlerRegistry.getInstance();

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
        for (AdvBeanPropertyMeta property : mappingMeta.getWritableProperties(ignoreFieldNames)) {
            Object value = property.readValue(bean);
            if (value == null && ignoreNullValue) {
                continue;
            }
            if (ignoreEmptyString && value instanceof String && ((String) value).trim().isEmpty()) {
                continue;
            }
            String columnName = property.resolveColumnName(toUnderlineCase);
            Object jdbcValue = typeHandlerRegistry.convertForWrite(
                    value,
                    property.getPropertyType(),
                    AdvTypeHandlerContext.of(
                            bean.getClass(),
                            property.getPropertyName(),
                            columnName,
                            property.getPropertyType()));
            rowData.put(columnName, jdbcValue);
        }
        return rowData;
    }
}
