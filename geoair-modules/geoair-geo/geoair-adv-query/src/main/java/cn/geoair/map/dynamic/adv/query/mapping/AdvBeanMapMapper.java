package cn.geoair.map.dynamic.adv.query.mapping;

import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanMappingMeta.AdvBeanPropertyMeta;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Map → Bean 映射器（与 {@link AdvBeanMapper} 对称，后者是 ResultSet → Bean）。
 *
 * <p>使用 TypeHandler 体系进行类型转换，Geometry / 日期 / 枚举等复杂类型能正确映射。
 *
 * <pre>{@code
 * AdvBeanMapMapper mapper = new AdvBeanMapMapper(typeHandlerRegistry);
 * List<User> users = mapper.mapList(rowList, User.class);
 * }</pre>
 *
 * @author zhangjun
 */
public class AdvBeanMapMapper {

    private final AdvTypeHandlerRegistry typeHandlerRegistry;

    public AdvBeanMapMapper(AdvTypeHandlerRegistry typeHandlerRegistry) {
        this.typeHandlerRegistry = typeHandlerRegistry;
    }

    /** 将单行 Map 转换为指定类型的 Bean */
    public <T> T mapToBean(Map<String, Object> row, Class<T> beanType) {
        if (row == null || row.isEmpty()) {
            return newInstance(beanType);
        }
        T bean = newInstance(beanType);
        AdvBeanMappingMeta mappingMeta = AdvBeanMappingMeta.of(beanType);
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            writeProperty(bean, beanType, mappingMeta, entry.getKey(), entry.getValue());
        }
        return bean;
    }

    /** 将多行 Map 列表转换为 Bean 列表 */
    public <T> List<T> mapList(List<? extends Map<String, Object>> rowList, Class<T> beanType) {
        if (rowList == null || rowList.isEmpty()) {
            return new ArrayList<>();
        }
        List<T> result = new ArrayList<>(rowList.size());
        AdvBeanMappingMeta mappingMeta = AdvBeanMappingMeta.of(beanType);
        for (Map<String, Object> row : rowList) {
            if (row == null) continue;
            T bean = newInstance(beanType);
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                writeProperty(bean, beanType, mappingMeta, entry.getKey(), entry.getValue());
            }
            result.add(bean);
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> void writeProperty(
            T bean,
            Class<T> beanType,
            AdvBeanMappingMeta mappingMeta,
            String columnLabel,
            Object rawValue) {
        AdvBeanPropertyMeta propertyMeta =
                mappingMeta.resolvePropertyByColumnOrProperty(columnLabel);
        if (propertyMeta == null || propertyMeta.isIgnored()) {
            return;
        }
        AdvTypeHandler fieldHandler = propertyMeta.getAdvTypeHandler();
        Object convertedValue;
        if (fieldHandler != null) {
            convertedValue =
                    fieldHandler.convertForRead(
                            rawValue,
                            propertyMeta.getPropertyType(),
                            AdvTypeHandlerContext.of(
                                    beanType,
                                    propertyMeta.getPropertyName(),
                                    columnLabel,
                                    propertyMeta.getPropertyType()));
        } else {
            convertedValue =
                    typeHandlerRegistry.convertForRead(
                            rawValue,
                            propertyMeta.getPropertyType(),
                            AdvTypeHandlerContext.of(
                                    beanType,
                                    propertyMeta.getPropertyName(),
                                    columnLabel,
                                    propertyMeta.getPropertyType()));
        }
        propertyMeta.writeValue(bean, convertedValue);
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(Class<T> clazz) {
        try {
            if (Map.class.isAssignableFrom(clazz)) {
                return (T) new LinkedHashMap<String, Object>();
            }
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建对象失败：" + clazz.getName(), e);
        }
    }
}
