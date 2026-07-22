package cn.geoair.map.dynamic.adv.query.typehandler;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 类型处理上下文
 */
public class AdvTypeHandlerContext {

    private final Class<?> entityClass;
    private final String propertyName;
    private final String columnName;
    private final Class<?> propertyType;

    private AdvTypeHandlerContext(Class<?> entityClass, String propertyName, String columnName, Class<?> propertyType) {
        this.entityClass = entityClass;
        this.propertyName = propertyName;
        this.columnName = columnName;
        this.propertyType = propertyType;
    }

    public static AdvTypeHandlerContext of(
            Class<?> entityClass, String propertyName, String columnName, Class<?> propertyType) {
        return new AdvTypeHandlerContext(entityClass, propertyName, columnName, propertyType);
    }

    public static AdvTypeHandlerContext simple(String columnName) {
        return new AdvTypeHandlerContext(null, null, columnName, null);
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getColumnName() {
        return columnName;
    }

    public Class<?> getPropertyType() {
        return propertyType;
    }
}
