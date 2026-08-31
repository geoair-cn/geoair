package cn.geoair.comp.jdbc.url.enums;

import cn.hutool.db.dialect.DialectName;
import cn.hutool.db.dialect.DriverNamePool;

import lombok.Getter;

/**
 * JDBC URL 所属数据库类型，并统一关联 Hutool 的方言名称和默认驱动类名。
 *
 * @author 张逢吉
 */
@Getter
public enum DatabaseType {
    MYSQL(DialectName.MYSQL, DriverNamePool.DRIVER_MYSQL, "mysql"),
    POSTGRESQL(DialectName.POSTGRESQL, DriverNamePool.DRIVER_POSTGRESQL, "postgresql"),
    ORACLE(DialectName.ORACLE, DriverNamePool.DRIVER_ORACLE, "oracle"),
    SQLSERVER(DialectName.SQLSERVER, DriverNamePool.DRIVER_SQLSERVER, "sqlserver"),
    H2(DialectName.H2, DriverNamePool.DRIVER_H2, "h2"),
    SQLITE(DialectName.SQLITE3, DriverNamePool.DRIVER_SQLLITE3, "sqlite"),
    // DM/HANA 在较早 Hutool 版本中不存在，采用名称解析以兼容不同的 Hutool 5.x 版本。
    DM(dialect("DM"), driver("DRIVER_DM7", "dm.jdbc.driver.DmDriver"), "dm"),
    SAP_HANA(dialect("HANA"), driver("DRIVER_HANA", "com.sap.db.jdbc.Driver"), "sap"),
    PHOENIX(DialectName.PHOENIX, DriverNamePool.DRIVER_PHOENIX, "phoenix"),
    UNKNOWN(null, null, "");

    /** Hutool 数据库方言；旧版 Hutool 未包含的类型可能为 null。 */
    private final DialectName dialectName;

    /** Hutool 定义的 JDBC 驱动类名。 */
    private final String driverClassName;

    /** JDBC URL 中 jdbc: 之后的驱动协议名。 */
    private final String jdbcDriverName;

    DatabaseType(DialectName dialectName, String driverClassName, String jdbcDriverName) {
        this.dialectName = dialectName;
        this.driverClassName = driverClassName;
        this.jdbcDriverName = jdbcDriverName;
    }

    public static DatabaseType fromDialectName(DialectName dialectName) {
        if (dialectName == null) {
            return UNKNOWN;
        }
        for (DatabaseType value : values()) {
            if (dialectName == value.dialectName) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static DatabaseType fromDriverClassName(String driverClassName) {
        if (driverClassName == null) {
            return UNKNOWN;
        }
        for (DatabaseType value : values()) {
            if (value.driverClassName != null && driverClassName.contains(value.driverClassName)) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static DatabaseType fromJdbcDriverName(String jdbcDriverName) {
        if (jdbcDriverName == null) {
            return UNKNOWN;
        }
        for (DatabaseType value : values()) {
            if (value.jdbcDriverName.equalsIgnoreCase(jdbcDriverName)) {
                return value;
            }
        }
        return UNKNOWN;
    }

    private static DialectName dialect(String name) {
        try {
            return DialectName.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String driver(String fieldName, String fallback) {
        try {
            return (String) DriverNamePool.class.getField(fieldName).get(null);
        } catch (ReflectiveOperationException ignored) {
            return fallback;
        }
    }
}
