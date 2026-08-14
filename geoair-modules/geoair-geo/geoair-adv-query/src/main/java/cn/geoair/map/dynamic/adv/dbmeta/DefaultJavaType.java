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

/** Java类型枚举，实现TypeMetadata统一接口 */
public enum DefaultJavaType implements TypeMetadata {

    // ========== 字符串/文本类 ==========
    JAVA_STRING("VARCHAR", String.class, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.CHAR),
    JAVA_CHAR("CHAR", String.class, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.CHAR),
    JAVA_TEXT("TEXT", String.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.TEXT),
    JAVA_UUID("UUID", UUID.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.CHAR),
    JAVA_JSON("JSON", String.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.TEXT),
    JAVA_JSONB("JSONB", String.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.TEXT),
    JAVA_CITEXT("CITEXT", String.class, IgnorePolicy.KEEP, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.CHAR),

    // ========== 布尔类 ==========
    JAVA_BOOLEAN("BOOLEAN", Boolean.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.BOOLEAN),

    // ========== 整数类 ==========
    JAVA_SHORT("SMALLINT", Short.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.INT),
    JAVA_INTEGER("INT", Integer.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.INT),
    JAVA_LONG("LONG", Long.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.INT),
    JAVA_BYTE("TINYINT", Byte.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.INT),
    JAVA_BIGINT("BIGINT", Long.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.INT),
    JAVA_SERIAL("SERIAL", Integer.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.INT),
    JAVA_BIGSERIAL("BIGSERIAL", Long.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.INT),

    // ========== 浮点/小数类 ==========
    JAVA_FLOAT("FLOAT", Float.class, IgnorePolicy.IGNORE, IgnorePolicy.KEEP, IgnorePolicy.KEEP, CategoryEnum.FLOAT),
    JAVA_DOUBLE("DOUBLE", Double.class, IgnorePolicy.IGNORE, IgnorePolicy.KEEP, IgnorePolicy.KEEP, CategoryEnum.FLOAT),
    JAVA_DECIMAL("DECIMAL", BigDecimal.class, IgnorePolicy.IGNORE, IgnorePolicy.KEEP, IgnorePolicy.KEEP, CategoryEnum.FLOAT),
    JAVA_NUMERIC("NUMERIC", BigDecimal.class, IgnorePolicy.IGNORE, IgnorePolicy.KEEP, IgnorePolicy.KEEP, CategoryEnum.FLOAT),
    JAVA_REAL("REAL", Float.class, IgnorePolicy.IGNORE, IgnorePolicy.KEEP, IgnorePolicy.KEEP, CategoryEnum.FLOAT),

    // ========== 日期时间类 ==========
    JAVA_DATE("DATE", Date.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.DATE),
    JAVA_SQL_TIMESTAMP("TIMESTAMP", java.sql.Timestamp.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.TIMESTAMP),
    JAVA_SQL_TIME("TIME", Time.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.TIME),
    JAVA_SQL_DATE("DATE", java.sql.Date.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.DATE),
    JAVA_LOCAL_DATE("DATE", LocalDate.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.DATE),
    JAVA_LOCAL_TIME("TIME", LocalTime.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.TIME),
    JAVA_LOCAL_DATE_TIME("TIMESTAMP", LocalDateTime.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.TIMESTAMP),
    JAVA_OFFSET_DATE_TIME("TIMESTAMPTZ", OffsetDateTime.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.TIMESTAMP),
    JAVA_OFFSET_TIME("TIMETZ", OffsetTime.class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.TIME),
    JAVA_INTERVAL("INTERVAL", String.class, IgnorePolicy.IGNORE, IgnorePolicy.CONDITIONAL, IgnorePolicy.MUTUAL_DEPENDENT, CategoryEnum.INTERVAL),

    // ========== 二进制/大对象类 ==========
    JAVA_BYTES("BYTEA", byte[].class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.BYTES),
    JAVA_BLOB("BLOB", byte[].class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.BLOB),
    JAVA_GEOMETRY("GEOMETRY", byte[].class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.GEOMETRY),
    JAVA_GEOGRAPHY("GEOGRAPHY", byte[].class, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, IgnorePolicy.IGNORE, CategoryEnum.GEOMETRY),

    // ========== 特殊类型 ==========
    JAVA_MONEY("MONEY", BigDecimal.class, IgnorePolicy.IGNORE, IgnorePolicy.KEEP, IgnorePolicy.KEEP, CategoryEnum.FLOAT);

    private final String name;
    private final Class<?> clazz;
    private final IgnorePolicy ignoreLength;
    private final IgnorePolicy ignorePrecision;
    private final IgnorePolicy ignoreScale;
    private final CategoryEnum category;

    DefaultJavaType(String name, Class<?> clazz, IgnorePolicy ignoreLength, IgnorePolicy ignorePrecision,
                    IgnorePolicy ignoreScale, CategoryEnum category) {
        this.name = name;
        this.clazz = clazz;
        this.ignoreLength = ignoreLength;
        this.ignorePrecision = ignorePrecision;
        this.ignoreScale = ignoreScale;
        this.category = category;
    }

    @Override
    public String getName() { return name; }

    public Class<?> getJavaClazz() { return clazz; }

    @Override
    public int ignoreLength() { return ignoreLength.code(); }

    @Override
    public int ignorePrecision() { return ignorePrecision.code(); }

    @Override
    public int ignoreScale() { return ignoreScale.code(); }

    @Override
    public boolean support() { return true; }

    @Override
    public Class<?> supportClass() { return clazz; }

    @Override
    public CategoryEnum getCategory() { return category; }

    @Override
    public CategoryGroupEnum getCategoryGroup() { return category.group(); }

    @Override
    public Config config() { return category.config(); }
}
