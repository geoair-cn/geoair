package cn.geoair.map.dynamic.adv.dbmeta;

import java.util.*;

/**
 * MySQL 数据类型与Java类型映射枚举
 */
public enum MysqlType implements DataBaseFieldType {

    // 整数类
    TINYINT("tinyint", "TINYINT", DefaultJavaType.JAVA_BYTE, CATEGORY.INT),
    SMALLINT("smallint", "SMALLINT", DefaultJavaType.JAVA_SHORT, CATEGORY.INT),
    MEDIUMINT("mediumint", "MEDIUMINT", DefaultJavaType.JAVA_INTEGER, CATEGORY.INT),
    INT("int", "INT", DefaultJavaType.JAVA_INTEGER, CATEGORY.INT),
    INTEGER("integer", "INTEGER", DefaultJavaType.JAVA_INTEGER, CATEGORY.INT),
    BIGINT("bigint", "BIGINT", DefaultJavaType.JAVA_BIGINT, CATEGORY.INT),

    // 浮点/小数类
    FLOAT("float", "FLOAT", DefaultJavaType.JAVA_FLOAT, CATEGORY.FLOAT),
    DOUBLE("double", "DOUBLE", DefaultJavaType.JAVA_DOUBLE, CATEGORY.FLOAT),
    DOUBLE_PRECISION("double precision", "DOUBLE PRECISION", DefaultJavaType.JAVA_DOUBLE, CATEGORY.FLOAT),
    DECIMAL("decimal", "DECIMAL", DefaultJavaType.JAVA_DECIMAL, CATEGORY.FLOAT),
    NUMERIC("numeric", "NUMERIC", DefaultJavaType.JAVA_NUMERIC, CATEGORY.FLOAT),

    // 字符串类
    VARCHAR("varchar", "VARCHAR", DefaultJavaType.JAVA_STRING, CATEGORY.CHAR),
    CHAR("char", "CHAR", DefaultJavaType.JAVA_CHAR, CATEGORY.CHAR),
    TEXT("text", "TEXT", DefaultJavaType.JAVA_TEXT, CATEGORY.TEXT),
    TINYTEXT("tinytext", "TINYTEXT", DefaultJavaType.JAVA_TEXT, CATEGORY.TEXT),
    MEDIUMTEXT("mediumtext", "MEDIUMTEXT", DefaultJavaType.JAVA_TEXT, CATEGORY.TEXT),
    LONGTEXT("longtext", "LONGTEXT", DefaultJavaType.JAVA_TEXT, CATEGORY.TEXT),
    ENUM("enum", "ENUM", DefaultJavaType.JAVA_STRING, CATEGORY.CHAR),
    SET("set", "SET", DefaultJavaType.JAVA_STRING, CATEGORY.CHAR),

    // 日期时间类
    DATE("date", "DATE", DefaultJavaType.JAVA_LOCAL_DATE, CATEGORY.DATE),
    TIME("time", "TIME", DefaultJavaType.JAVA_LOCAL_TIME, CATEGORY.TIME),
    DATETIME("datetime", "DATETIME", DefaultJavaType.JAVA_LOCAL_DATE_TIME, CATEGORY.DATETIME),
    TIMESTAMP("timestamp", "TIMESTAMP", DefaultJavaType.JAVA_LOCAL_DATE_TIME, CATEGORY.TIMESTAMP),
    YEAR("year", "YEAR", DefaultJavaType.JAVA_INTEGER, CATEGORY.INT),

    // 二进制类
    BINARY("binary", "BINARY", DefaultJavaType.JAVA_BYTES, CATEGORY.BYTES),
    VARBINARY("varbinary", "VARBINARY", DefaultJavaType.JAVA_BYTES, CATEGORY.BYTES),
    BLOB("blob", "BLOB", DefaultJavaType.JAVA_BLOB, CATEGORY.BLOB),
    TINYBLOB("tinyblob", "TINYBLOB", DefaultJavaType.JAVA_BLOB, CATEGORY.BLOB),
    MEDIUMBLOB("mediumblob", "MEDIUMBLOB", DefaultJavaType.JAVA_BLOB, CATEGORY.BLOB),
    LONGBLOB("longblob", "LONGBLOB", DefaultJavaType.JAVA_BLOB, CATEGORY.BLOB),

    // JSON
    JSON("json", "JSON", DefaultJavaType.JAVA_JSON, CATEGORY.TEXT),

    // 空间类
    GEOMETRY(DefaultJavaType.JAVA_GEOMETRY, CATEGORY.GEOMETRY,
            "geometry", "GEOMETRY"),
    POINT(DefaultJavaType.JAVA_GEOMETRY, CATEGORY.GEOMETRY,
            "point", "POINT"),
    LINESTRING(DefaultJavaType.JAVA_GEOMETRY, CATEGORY.GEOMETRY,
            "linestring", "LINESTRING"),
    POLYGON(DefaultJavaType.JAVA_GEOMETRY, CATEGORY.GEOMETRY,
            "polygon", "POLYGON"),
    MULTIPOINT(DefaultJavaType.JAVA_GEOMETRY, CATEGORY.GEOMETRY,
            "multipoint", "MULTIPOINT"),
    MULTILINESTRING(DefaultJavaType.JAVA_GEOMETRY, CATEGORY.GEOMETRY,
            "multilinestring", "MULTILINESTRING"),
    MULTIPOLYGON(DefaultJavaType.JAVA_GEOMETRY, CATEGORY.GEOMETRY,
            "multipolygon", "MULTIPOLYGON"),
    GEOMETRYCOLLECTION(DefaultJavaType.JAVA_GEOMETRY, CATEGORY.GEOMETRY,
            "geometrycollection", "GEOMETRYCOLLECTION"),

    // 其他
    BIT("bit", "BIT", DefaultJavaType.JAVA_BYTE, CATEGORY.INT),

    // 布尔（TINYINT(1) 在 MySQL 中经常表示布尔）
    BOOLEAN(DefaultJavaType.JAVA_BOOLEAN, CATEGORY.BOOLEAN,
            "boolean", "BOOL", "tinyint");

    private final List<String> udtNames;
    private final String standardName;
    private final DefaultJavaType javaType;
    private final CATEGORY category;

    private static final Map<String, MysqlType> UDT_NAME_MAP = new HashMap<>();

    static {
        for (MysqlType type : values()) {
            for (String name : type.udtNames) {
                UDT_NAME_MAP.put(name.toLowerCase(), type);
            }
        }
    }

    /**
     * 单一 udtName 的构造器
     */
    MysqlType(String udtName, String standardName, DefaultJavaType javaType, CATEGORY category) {
        this.udtNames = Arrays.asList(udtName);
        this.standardName = standardName;
        this.javaType = javaType;
        this.category = category;
    }

    /**
     * 多个 udtName 变体的构造器
     */
    MysqlType(DefaultJavaType javaType, CATEGORY category, String... udtNames) {
        this.udtNames = Arrays.asList(udtNames);
        this.standardName = this.name();
        this.javaType = javaType;
        this.category = category;
    }

    public static MysqlType getByUdtName(String udtName) {
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
    public CATEGORY getCategory() {
        return category;
    }

    @Override
    public CATEGORY_GROUP getCategoryGroup() {
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
