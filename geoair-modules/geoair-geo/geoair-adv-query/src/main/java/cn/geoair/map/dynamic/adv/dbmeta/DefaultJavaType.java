package cn.geoair.map.dynamic.adv.dbmeta;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.Date;
import java.util.UUID;

/** java类型的枚举 */
public enum DefaultJavaType implements DataType {

    /*
     * ***********************************************************************************
     * ******************************
     *
     * JAVA DATA TYPE
     *
     * ===================================================================================
     * ============================== String number-int/long number-double/float date
     * byte[] byte[]-geometry
     *
     ******************************************************************************************************************/

    /*
     * ***********************************************************************************
     * ******************************
     *
     *
     *
     ****************************************************************************************************************/
    JAVA_STRING("VARCHAR", String.class, 0, 1, 1) {},
    JAVA_CHAR("CHAR", String.class, 0, 1, 1) {}, // 对应PG CHAR类型
    JAVA_TEXT("TEXT", String.class, 1, 1, 1) {}, // 对应PG TEXT类型（无长度限制，忽略长度）
    JAVA_UUID("UUID", UUID.class, 1, 1, 1) {}, // 对应PG UUID类型
    JAVA_JSON("JSON", String.class, 1, 1, 1) {}, // 对应PG JSON类型
    JAVA_JSONB("JSONB", String.class, 1, 1, 1) {}, // 对应PG JSONB类型
    JAVA_CITEXT("CITEXT", String.class, 0, 1, 1) {}, // 对应PG 大小写不敏感文本类型

    /*
     * ***********************************************************************************
     * ******************************
     *
     * Boolean 类型
     *
     ****************************************************************************************************************/
    JAVA_BOOLEAN("BOOLEAN", Boolean.class, 1, 1, 1) {},

    /*
     * ***********************************************************************************
     * ******************************
     *
     * 整数类型（补充PG高精度整数）
     *
     ****************************************************************************************************************/
    JAVA_INTEGER("INT", Integer.class, 1, 1, 1) {},
    JAVA_LONG("LONG", Long.class, 1, 1, 1) {},
    JAVA_SHORT("SMALLINT", Short.class, 1, 1, 1) {}, // 对应PG SMALLINT类型
    JAVA_BYTE("TINYINT", Byte.class, 1, 1, 1) {}, // 对应PG TINYINT（扩展类型）
    JAVA_BIGINT("BIGINT", Long.class, 1, 1, 1) {}, // 对应PG BIGINT类型
    JAVA_SERIAL("SERIAL", Integer.class, 1, 1, 1) {}, // 对应PG SERIAL自增类型
    JAVA_BIGSERIAL("BIGSERIAL", Long.class, 1, 1, 1) {}, // 对应PG BIGSERIAL自增类型

    /*
     * ***********************************************************************************
     * ******************************
     *
     * 浮点/小数类型（补充PG高精度小数）
     *
     ****************************************************************************************************************/
    JAVA_FLOAT("FLOAT", Float.class, 1, 0, 0) {},
    JAVA_DOUBLE("DOUBLE", Double.class, 1, 0, 0) {},
    JAVA_DECIMAL("DECIMAL", BigDecimal.class, 1, 0, 0) {},
    JAVA_NUMERIC("NUMERIC", BigDecimal.class, 1, 0, 0) {}, // 对应PG NUMERIC类型（与DECIMAL等价）
    JAVA_REAL("REAL", Float.class, 1, 0, 0) {}, // 对应PG REAL类型（单精度浮点）

    /*
     * ***********************************************************************************
     * ******************************
     *
     * 日期时间类型（补充PG时间间隔/时区类型）
     *
     ****************************************************************************************************************/
    JAVA_DATE("DATE", Date.class, 1, 1, 1) {},
    JAVA_SQL_TIMESTAMP("TIMESTAMP", java.sql.Timestamp.class, 1, 1, 1) {},
    JAVA_SQL_TIME("TIME", Time.class, 1, 1, 1) {},
    JAVA_SQL_DATE("DATE", java.sql.Date.class, 1, 1, 1) {},
    JAVA_LOCAL_DATE("DATE", LocalDate.class, 1, 1, 1) {},
    JAVA_LOCAL_TIME("TIME", LocalTime.class, 1, 1, 1) {},
    JAVA_LOCAL_DATE_TIME("TIMESTAMP", LocalDateTime.class, 1, 1, 1) {},
    JAVA_OFFSET_DATE_TIME("TIMESTAMPTZ", OffsetDateTime.class, 1, 1, 1) {}, // 对应PG带时区时间
    JAVA_OFFSET_TIME("TIMETZ", OffsetTime.class, 1, 1, 1) {}, // 对应PG带时区时间
    JAVA_INTERVAL("INTERVAL", String.class, 1, 2, 3) {}, // 对应PG INTERVAL类型（映射为String，需自定义解析）

    /*
     * ***********************************************************************************
     * ******************************
     *
     * 二进制类型（补充PG大对象类型）
     *
     ****************************************************************************************************************/
    JAVA_BYTES("BYTEA", Byte[].class, 1, 1, 1) {}, // 对应PG BYTEA类型
    JAVA_BLOB("BLOB", Byte[].class, 1, 1, 1) {}, // 对应PG BLOB（通过扩展类型支持）
    JAVA_GEOMETRY("GEOMETRY", Byte[].class, 1, 1, 1) {}, // 对应PG GEOMETRY空间类型

    /*
     * ***********************************************************************************
     * ******************************
     *
     * 特殊数值类型
     *
     ****************************************************************************************************************/
    JAVA_MONEY("MONEY", BigDecimal.class, 1, 0, 0) {}; // 对应PG MONEY类型（映射为BigDecimal避免精度丢失）

    private final String name;

    private final Class clazz;

    private final int ignoreLength;

    private final int ignorePrecision;

    private final int ignoreScale;

    private DefaultJavaType(
            String name, Class clazz, int ignoreLength, int ignorePrecision, int ignoreScale) {
        this.name = name;
        this.clazz = clazz;
        this.ignoreLength = ignoreLength;
        this.ignorePrecision = ignorePrecision;
        this.ignoreScale = ignoreScale;
    }

    public String getName() {
        return name;
    }

    public Class getJavaClazz() {
        return clazz;
    }

    public int getIgnoreLength() {
        return ignoreLength;
    }

    public int getIgnorePrecision() {
        return ignorePrecision;
    }

    public int getIgnoreScale() {
        return ignoreScale;
    }

    @Override
    public int ignoreLength() {
        return ignoreLength;
    }

    @Override
    public int ignorePrecision() {
        return ignorePrecision;
    }

    @Override
    public int ignoreScale() {
        return ignoreScale;
    }

    @Override
    public boolean support() {
        return true;
    }

    @Override
    public Class supportClass() {
        return clazz;
    }
}
