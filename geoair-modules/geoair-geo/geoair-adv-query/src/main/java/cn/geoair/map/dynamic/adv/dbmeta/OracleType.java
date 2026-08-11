package cn.geoair.map.dynamic.adv.dbmeta;

import java.util.HashMap;
import java.util.Map;

/** Oracle 数据类型与Java类型映射枚举 */
public enum OracleType implements TypeMetadata {

    // 字符串类
    VARCHAR2("varchar2", "VARCHAR2", DefaultJavaType.JAVA_STRING, CATEGORY.CHAR),
    NVARCHAR2("nvarchar2", "NVARCHAR2", DefaultJavaType.JAVA_STRING, CATEGORY.CHAR),
    CHAR("char", "CHAR", DefaultJavaType.JAVA_CHAR, CATEGORY.CHAR),
    NCHAR("nchar", "NCHAR", DefaultJavaType.JAVA_CHAR, CATEGORY.CHAR),
    CLOB("clob", "CLOB", DefaultJavaType.JAVA_TEXT, CATEGORY.TEXT),
    NCLOB("nclob", "NCLOB", DefaultJavaType.JAVA_TEXT, CATEGORY.TEXT),
    LONG("long", "LONG", DefaultJavaType.JAVA_TEXT, CATEGORY.TEXT),
    ROWID("rowid", "ROWID", DefaultJavaType.JAVA_STRING, CATEGORY.OTHER),
    UROWID("urowid", "UROWID", DefaultJavaType.JAVA_STRING, CATEGORY.OTHER),

    // 数值类
    NUMBER("number", "NUMBER", DefaultJavaType.JAVA_NUMERIC, CATEGORY.FLOAT),
    FLOAT("float", "FLOAT", DefaultJavaType.JAVA_FLOAT, CATEGORY.FLOAT),
    BINARY_FLOAT("binary_float", "BINARY_FLOAT", DefaultJavaType.JAVA_FLOAT, CATEGORY.FLOAT),
    BINARY_DOUBLE("binary_double", "BINARY_DOUBLE", DefaultJavaType.JAVA_DOUBLE, CATEGORY.FLOAT),

    // 日期时间类
    DATE("date", "DATE", DefaultJavaType.JAVA_DATE, CATEGORY.DATE),
    TIMESTAMP("timestamp", "TIMESTAMP", DefaultJavaType.JAVA_SQL_TIMESTAMP, CATEGORY.TIMESTAMP),
    TIMESTAMP_WITH_TIME_ZONE("timestamp with time zone", "TIMESTAMP WITH TIME ZONE",
            DefaultJavaType.JAVA_OFFSET_DATE_TIME, CATEGORY.TIMESTAMP),
    TIMESTAMP_WITH_LOCAL_TIME_ZONE("timestamp with local time zone", "TIMESTAMP WITH LOCAL TIME ZONE",
            DefaultJavaType.JAVA_OFFSET_DATE_TIME, CATEGORY.TIMESTAMP),
    INTERVAL_YEAR_TO_MONTH("interval year to month", "INTERVAL YEAR TO MONTH",
            DefaultJavaType.JAVA_INTERVAL, CATEGORY.INTERVAL),
    INTERVAL_DAY_TO_SECOND("interval day to second", "INTERVAL DAY TO SECOND",
            DefaultJavaType.JAVA_INTERVAL, CATEGORY.INTERVAL),

    // 二进制类
    BLOB("blob", "BLOB", DefaultJavaType.JAVA_BLOB, CATEGORY.BLOB),
    RAW("raw", "RAW", DefaultJavaType.JAVA_BYTES, CATEGORY.BYTES),
    LONG_RAW("long raw", "LONG RAW", DefaultJavaType.JAVA_BYTES, CATEGORY.BYTES),
    BFILE("bfile", "BFILE", DefaultJavaType.JAVA_BYTES, CATEGORY.OTHER),

    // XML
    XMLTYPE("xmltype", "XMLTYPE", DefaultJavaType.JAVA_STRING, CATEGORY.TEXT),

    // 空间类型
    SDO_GEOMETRY("sdo_geometry", "SDO_GEOMETRY", DefaultJavaType.JAVA_GEOMETRY, CATEGORY.GEOMETRY);

    private final String udtName;
    private final String standardName;
    private final DefaultJavaType javaType;
    private final CATEGORY category;

    private static final Map<String, OracleType> UDT_NAME_MAP = new HashMap<>();

    static {
        for (OracleType type : values()) {
            UDT_NAME_MAP.put(type.udtName.toLowerCase(), type);
        }
    }

    OracleType(String udtName, String standardName, DefaultJavaType javaType, CATEGORY category) {
        this.udtName = udtName;
        this.standardName = standardName;
        this.javaType = javaType;
        this.category = category;
    }

    public static OracleType getByUdtName(String udtName) {
        if (udtName == null) return null;
        return UDT_NAME_MAP.get(udtName.toLowerCase());
    }

    public DefaultJavaType getJavaType() { return javaType; }

    @Override public CATEGORY getCategory() { return category; }
    @Override public CATEGORY_GROUP getCategoryGroup() { return category.group(); }
    @Override public String getName() { return standardName; }
    @Override public int ignoreLength() { return javaType.ignoreLength(); }
    @Override public int ignorePrecision() { return javaType.ignorePrecision(); }
    @Override public int ignoreScale() { return javaType.ignoreScale(); }
    @Override public boolean support() { return javaType.support(); }
    @Override public Class<?> supportClass() { return javaType.supportClass(); }
    @Override public Config config() { return category.config(); }
    @Override public String toString() { return standardName; }
}
