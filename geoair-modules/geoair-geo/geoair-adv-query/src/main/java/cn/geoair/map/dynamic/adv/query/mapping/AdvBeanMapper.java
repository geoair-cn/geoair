package cn.geoair.map.dynamic.adv.query.mapping;

import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanMappingMeta.AdvBeanPropertyMeta;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： bean结果映射器
 */
public class AdvBeanMapper {

    private final AdvTypeHandlerRegistry typeHandlerRegistry = AdvTypeHandlerRegistry.getInstance();

    public <T> T mapRow(ResultSet rs, Class<T> beanType) throws SQLException {
        T bean = createBean(beanType);
        mapCurrentRow(rs, bean, beanType);
        return bean;
    }

    public <T> List<T> mapList(ResultSet rs, Class<T> beanType) throws SQLException {
        List<T> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapCurrentRow(rs, createBean(beanType), beanType));
        }
        return list;
    }

    private <T> T mapCurrentRow(ResultSet rs, T bean, Class<T> beanType) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        AdvBeanMappingMeta mappingMeta = AdvBeanMappingMeta.of(beanType);
        for (int i = 1; i <= columnCount; i++) {
            String columnLabel = metaData.getColumnLabel(i);
            AdvBeanPropertyMeta propertyMeta = mappingMeta.resolvePropertyByColumnOrProperty(columnLabel);
            if (propertyMeta == null || propertyMeta.isIgnored()) {
                continue;
            }
            Object rawValue = rs.getObject(i);
            Object convertedValue = typeHandlerRegistry.convertForRead(
                    rawValue,
                    propertyMeta.getPropertyType(),
                    AdvTypeHandlerContext.of(
                            beanType,
                            propertyMeta.getPropertyName(),
                            columnLabel,
                            propertyMeta.getPropertyType()));
            propertyMeta.writeValue(bean, convertedValue);
        }
        return bean;
    }

    private <T> T createBean(Class<T> beanType) {
        try {
            return beanType.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建对象失败：" + beanType.getName(), e);
        }
    }
}
