package cn.geoair.map.dynamic.adv.dbmeta;

import java.util.HashMap;
import java.util.Map;

/** PostgreSQL UDT类型与Java类型映射枚举（含内部类型名+TypeMetadata.CATEGORY关联） */
public enum PostgreSqlType implements TypeMetadata {

    // 字符串/文本类（关联CATEGORY.CHAR/TEXT）
    VARCHAR("varchar", "VARCHAR", DefaultJavaType.JAVA_STRING, CATEGORY.CHAR),
    CHAR("char", "CHAR", DefaultJavaType.JAVA_CHAR, CATEGORY.CHAR),
    TEXT("text", "TEXT", DefaultJavaType.JAVA_TEXT, CATEGORY.TEXT),
    UUID("uuid", "UUID", DefaultJavaType.JAVA_UUID, CATEGORY.CHAR),
    JSON("json", "JSON", DefaultJavaType.JAVA_JSON, CATEGORY.TEXT),
    JSONB("jsonb", "JSONB", DefaultJavaType.JAVA_JSONB, CATEGORY.TEXT),
    CITEXT("citext", "CITEXT", DefaultJavaType.JAVA_CITEXT, CATEGORY.CHAR),

    // 布尔类（关联CATEGORY.BOOLEAN）
    BOOLEAN("boolean", "BOOLEAN", DefaultJavaType.JAVA_BOOLEAN, CATEGORY.BOOLEAN),

    // 整数类（含内部类型名，关联CATEGORY.INT）
    INT2("int2", "SMALLINT", DefaultJavaType.JAVA_SHORT, CATEGORY.INT), // 16位整数
    INT4("int4", "INTEGER", DefaultJavaType.JAVA_INTEGER, CATEGORY.INT), // 32位整数
    INT8("int8", "BIGINT", DefaultJavaType.JAVA_BIGINT, CATEGORY.INT), // 64位整数
    SERIAL("serial", "SERIAL", DefaultJavaType.JAVA_SERIAL, CATEGORY.INT), // 自增int4
    BIGSERIAL("bigserial", "BIGSERIAL", DefaultJavaType.JAVA_BIGSERIAL, CATEGORY.INT),

    // 浮点/小数类（关联CATEGORY.FLOAT）
    FLOAT4("float4", "REAL", DefaultJavaType.JAVA_REAL, CATEGORY.FLOAT), // 32位浮点
    FLOAT8("float8", "DOUBLE PRECISION", DefaultJavaType.JAVA_DOUBLE, CATEGORY.FLOAT), // 64位浮点
    NUMERIC("numeric", "NUMERIC", DefaultJavaType.JAVA_NUMERIC, CATEGORY.FLOAT),
    DECIMAL("decimal", "DECIMAL", DefaultJavaType.JAVA_DECIMAL, CATEGORY.FLOAT),

    // 日期时间类（关联CATEGORY.DATE/TIME/DATETIME/TIMESTAMP）
    DATE("date", "DATE", DefaultJavaType.JAVA_LOCAL_DATE, CATEGORY.DATE),
    TIME("time", "TIME", DefaultJavaType.JAVA_LOCAL_TIME, CATEGORY.TIME),
    TIMETZ("timetz", "TIME WITH TIME ZONE", DefaultJavaType.JAVA_OFFSET_TIME, CATEGORY.TIME),
    TIMESTAMP("timestamp", "TIMESTAMP", DefaultJavaType.JAVA_LOCAL_DATE_TIME, CATEGORY.TIMESTAMP),
    TIMESTAMPTZ(
            "timestamptz",
            "TIMESTAMP WITH TIME ZONE",
            DefaultJavaType.JAVA_OFFSET_DATE_TIME,
            CATEGORY.TIMESTAMP),
    INTERVAL("interval", "INTERVAL", DefaultJavaType.JAVA_INTERVAL, CATEGORY.INTERVAL),

    // 二进制/特殊类（关联CATEGORY.BYTES/GEOMETRY/OTHER）
    BYTEA("bytea", "BYTEA", DefaultJavaType.JAVA_BYTES, CATEGORY.BYTES),
    BLOB("blob", "BLOB", DefaultJavaType.JAVA_BLOB, CATEGORY.BLOB),
    GEOMETRY("geometry", "GEOMETRY", DefaultJavaType.JAVA_GEOMETRY, CATEGORY.GEOMETRY),
    MONEY("money", "MONEY", DefaultJavaType.JAVA_MONEY, CATEGORY.FLOAT); // 货币类型归为浮点类

    // 原有字段
    private final String udtName;

    private final String standardName;

    private final DefaultJavaType javaType;

    private final CATEGORY category;

    // 缓存：udtName -> 枚举实例（加速查询）
    private static final Map<String, PostgreSqlType> UDT_NAME_MAP = new HashMap<>();

    static {
        // 初始化缓存，忽略大小写（PostgreSQL类型名大小写不敏感）
        for (PostgreSqlType type : values()) {
            UDT_NAME_MAP.put(type.udtName.toLowerCase(), type);
        }
    }

    // 构造方法新增category参数
    PostgreSqlType(
            String udtName, String standardName, DefaultJavaType javaType, CATEGORY category) {
        this.udtName = udtName;
        this.standardName = standardName;
        this.javaType = javaType;
        this.category = category;
    }

    /** 根据PostgreSQL内部类型名（如int4、int8）获取枚举实例 */
    public static PostgreSqlType getByUdtName(String udtName) {
        if (udtName == null) {
            return null;
        }
        return UDT_NAME_MAP.get(udtName.toLowerCase());
    }

    /** 获取对应的Java类型 */
    public DefaultJavaType getJavaType() {
        return javaType;
    }

    /** 新增：获取TypeMetadata.CATEGORY（实现接口方法） */
    @Override
    public CATEGORY getCategory() {
        return this.category; // 直接返回枚举中定义的category
    }

    /** 新增：通过category获取对应的CATEGORY_GROUP（实现接口方法） */
    @Override
    public CATEGORY_GROUP getCategoryGroup() {
        return this.category.group(); // 复用CATEGORY枚举中定义的group关联
    }

    // 其他接口方法实现（保持不变）
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
    public Config config() {
        // 复用CATEGORY的config配置
        return this.category.config();
    }

    @Override
    public String toString() {
        return standardName;
    }
}
