package cn.geoair.map.dynamic.adv.dbmeta;

import java.util.*;

/** PostgreSQL UDT类型与Java类型映射枚举 */
public enum PostgreSqlType implements DataBaseFieldType {

    // 字符串/文本类
    VARCHAR("varchar", "VARCHAR", DefaultJavaType.JAVA_STRING, CategoryEnum.CHAR),
    CHAR("char", "CHAR", DefaultJavaType.JAVA_CHAR, CategoryEnum.CHAR),
    TEXT("text", "TEXT", DefaultJavaType.JAVA_TEXT, CategoryEnum.TEXT),
    UUID("uuid", "UUID", DefaultJavaType.JAVA_UUID, CategoryEnum.CHAR),
    JSON("json", "JSON", DefaultJavaType.JAVA_JSON, CategoryEnum.TEXT),
    JSONB("jsonb", "JSONB", DefaultJavaType.JAVA_JSONB, CategoryEnum.TEXT),
    CITEXT("citext", "CITEXT", DefaultJavaType.JAVA_CITEXT, CategoryEnum.CHAR),

    // 布尔类
    BOOLEAN("boolean", "BOOLEAN", DefaultJavaType.JAVA_BOOLEAN, CategoryEnum.BOOLEAN),

    // 整数类
    INT2("int2", "SMALLINT", DefaultJavaType.JAVA_SHORT, CategoryEnum.INT),
    INT4("int4", "INTEGER", DefaultJavaType.JAVA_INTEGER, CategoryEnum.INT),
    INT8("int8", "BIGINT", DefaultJavaType.JAVA_BIGINT, CategoryEnum.INT),
    SERIAL("serial", "SERIAL", DefaultJavaType.JAVA_SERIAL, CategoryEnum.INT),
    BIGSERIAL("bigserial", "BIGSERIAL", DefaultJavaType.JAVA_BIGSERIAL, CategoryEnum.INT),

    // 浮点/小数类
    FLOAT4("float4", "REAL", DefaultJavaType.JAVA_REAL, CategoryEnum.FLOAT),
    FLOAT8("float8", "DOUBLE PRECISION", DefaultJavaType.JAVA_DOUBLE, CategoryEnum.FLOAT),
    NUMERIC("numeric", "NUMERIC", DefaultJavaType.JAVA_NUMERIC, CategoryEnum.FLOAT),
    DECIMAL("decimal", "DECIMAL", DefaultJavaType.JAVA_DECIMAL, CategoryEnum.FLOAT),

    // 日期时间类
    DATE("date", "DATE", DefaultJavaType.JAVA_LOCAL_DATE, CategoryEnum.DATE),
    TIME("time", "TIME", DefaultJavaType.JAVA_LOCAL_TIME, CategoryEnum.TIME),
    TIMETZ("timetz", "TIME WITH TIME ZONE", DefaultJavaType.JAVA_OFFSET_TIME, CategoryEnum.TIME),
    TIMESTAMP(
            "timestamp", "TIMESTAMP", DefaultJavaType.JAVA_LOCAL_DATE_TIME, CategoryEnum.TIMESTAMP),
    TIMESTAMPTZ(
            "timestamptz",
            "TIMESTAMP WITH TIME ZONE",
            DefaultJavaType.JAVA_OFFSET_DATE_TIME,
            CategoryEnum.TIMESTAMP),
    INTERVAL("interval", "INTERVAL", DefaultJavaType.JAVA_INTERVAL, CategoryEnum.INTERVAL),

    // 二进制/特殊类
    BYTEA("bytea", "BYTEA", DefaultJavaType.JAVA_BYTES, CategoryEnum.BYTES),
    BLOB("blob", "BLOB", DefaultJavaType.JAVA_BLOB, CategoryEnum.BLOB),

    // 空间类
    GEOMETRY(
            DefaultJavaType.JAVA_GEOMETRY,
            CategoryEnum.GEOMETRY,
            "geometry",
            "\"public\".\"geometry\""),
    GEOGRAPHY(
            DefaultJavaType.JAVA_GEOGRAPHY,
            CategoryEnum.GEOMETRY,
            "geography",
            "\"public\".\"geography\""),

    MONEY("money", "MONEY", DefaultJavaType.JAVA_MONEY, CategoryEnum.FLOAT);

    private final List<String> udtNames;
    private final String standardName;
    private final DefaultJavaType javaType;
    private final CategoryEnum category;

    private static final Map<String, PostgreSqlType> UDT_NAME_MAP = new HashMap<>();

    static {
        for (PostgreSqlType type : values()) {
            for (String name : type.udtNames) {
                UDT_NAME_MAP.put(name.toLowerCase(), type);
            }
        }
    }

    /** 单一 udtName 的构造器 */
    PostgreSqlType(
            String udtName, String standardName, DefaultJavaType javaType, CategoryEnum category) {
        this.udtNames = Arrays.asList(udtName);
        this.standardName = standardName;
        this.javaType = javaType;
        this.category = category;
    }

    /** 多个 udtName 变体的构造器 */
    PostgreSqlType(DefaultJavaType javaType, CategoryEnum category, String... udtNames) {
        this.udtNames = Arrays.asList(udtNames);
        this.standardName = this.name();
        this.javaType = javaType;
        this.category = category;
    }

    /** 根据 udtName 查找（匹配任一变体名） */
    public static PostgreSqlType getByUdtName(String udtName) {
        if (udtName == null) return null;
        return UDT_NAME_MAP.get(udtName.toLowerCase());
    }

    @Override
    public List<String> getUdtNames() {
        return Collections.unmodifiableList(this.udtNames);
    }

    @Override
    public String getStandardName() {
        return this.standardName;
    }

    public DefaultJavaType getJavaType() {
        return javaType;
    }

    @Override
    public CategoryEnum getCategory() {
        return category;
    }

    @Override
    public CategoryGroupEnum getCategoryGroup() {
        return category.group();
    }

    @Override
    public String getName() {
        return standardName;
    }

    @Override
    public int ignoreLength() {
        return javaType.ignoreLength();
    }

    @Override
    public int ignorePrecision() {
        return javaType.ignorePrecision();
    }

    @Override
    public int ignoreScale() {
        return javaType.ignoreScale();
    }

    @Override
    public boolean support() {
        return javaType.support();
    }

    @Override
    public Class<?> supportClass() {
        return javaType.supportClass();
    }

    @Override
    public Config config() {
        return category.config();
    }

    @Override
    public String toString() {
        return standardName;
    }
}
