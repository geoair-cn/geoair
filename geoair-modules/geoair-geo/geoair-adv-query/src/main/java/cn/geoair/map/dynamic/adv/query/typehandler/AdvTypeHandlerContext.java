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
    private final java.sql.Connection connection;

    private AdvTypeHandlerContext(
            Class<?> entityClass,
            String propertyName,
            String columnName,
            Class<?> propertyType,
            java.sql.Connection connection) {
        this.entityClass = entityClass;
        this.propertyName = propertyName;
        this.columnName = columnName;
        this.propertyType = propertyType;
        this.connection = connection;
    }

    public static AdvTypeHandlerContext of(
            Class<?> entityClass, String propertyName, String columnName, Class<?> propertyType) {
        return new AdvTypeHandlerContext(entityClass, propertyName, columnName, propertyType, null);
    }

    public static AdvTypeHandlerContext simple(String columnName) {
        return new AdvTypeHandlerContext(null, null, columnName, null, null);
    }

    public static AdvTypeHandlerContext withConnection(
            java.sql.Connection connection, String columnName) {
        return new AdvTypeHandlerContext(null, null, columnName, null, connection);
    }

    public java.sql.Connection getConnection() {
        return connection;
    }
}
