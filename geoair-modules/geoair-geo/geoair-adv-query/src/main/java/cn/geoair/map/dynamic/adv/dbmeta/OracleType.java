package cn.geoair.map.dynamic.adv.dbmeta;

import java.util.*;

/**
 * Oracle 数据类型与Java类型映射枚举
 */
public enum OracleType implements DataBaseFieldType {

    // 字符串类
    VARCHAR2("varchar2", "VARCHAR2", DefaultJavaType.JAVA_STRING, CategoryEnum.CHAR),
    NVARCHAR2("nvarchar2", "NVARCHAR2", DefaultJavaType.JAVA_STRING, CategoryEnum.CHAR),
    CHAR("char", "CHAR", DefaultJavaType.JAVA_CHAR, CategoryEnum.CHAR),
    NCHAR("nchar", "NCHAR", DefaultJavaType.JAVA_CHAR, CategoryEnum.CHAR),
    CLOB("clob", "CLOB", DefaultJavaType.JAVA_TEXT, CategoryEnum.TEXT),
    NCLOB("nclob", "NCLOB", DefaultJavaType.JAVA_TEXT, CategoryEnum.TEXT),
    LONG("long", "LONG", DefaultJavaType.JAVA_TEXT, CategoryEnum.TEXT),
    ROWID("rowid", "ROWID", DefaultJavaType.JAVA_STRING, CategoryEnum.OTHER),
    UROWID("urowid", "UROWID", DefaultJavaType.JAVA_STRING, CategoryEnum.OTHER),

    // 数值类
    NUMBER("number", "NUMBER", DefaultJavaType.JAVA_NUMERIC, CategoryEnum.FLOAT),
    FLOAT("float", "FLOAT", DefaultJavaType.JAVA_FLOAT, CategoryEnum.FLOAT),
    BINARY_FLOAT("binary_float", "BINARY_FLOAT", DefaultJavaType.JAVA_FLOAT, CategoryEnum.FLOAT),
    BINARY_DOUBLE("binary_double", "BINARY_DOUBLE", DefaultJavaType.JAVA_DOUBLE, CategoryEnum.FLOAT),

    // 日期时间类
    DATE("date", "DATE", DefaultJavaType.JAVA_DATE, CategoryEnum.DATE),
    TIMESTAMP("timestamp", "TIMESTAMP", DefaultJavaType.JAVA_SQL_TIMESTAMP, CategoryEnum.TIMESTAMP),
    TIMESTAMP_WITH_TIME_ZONE("timestamp with time zone", "TIMESTAMP WITH TIME ZONE",
            DefaultJavaType.JAVA_OFFSET_DATE_TIME, CategoryEnum.TIMESTAMP),
    TIMESTAMP_WITH_LOCAL_TIME_ZONE("timestamp with local time zone", "TIMESTAMP WITH LOCAL TIME ZONE",
            DefaultJavaType.JAVA_OFFSET_DATE_TIME, CategoryEnum.TIMESTAMP),
    INTERVAL_YEAR_TO_MONTH("interval year to month", "INTERVAL YEAR TO MONTH",
            DefaultJavaType.JAVA_INTERVAL, CategoryEnum.INTERVAL),
    INTERVAL_DAY_TO_SECOND("interval day to second", "INTERVAL DAY TO SECOND",
            DefaultJavaType.JAVA_INTERVAL, CategoryEnum.INTERVAL),

    // 二进制类
    BLOB("blob", "BLOB", DefaultJavaType.JAVA_BLOB, CategoryEnum.BLOB),
    RAW("raw", "RAW", DefaultJavaType.JAVA_BYTES, CategoryEnum.BYTES),
    LONG_RAW("long raw", "LONG RAW", DefaultJavaType.JAVA_BYTES, CategoryEnum.BYTES),
    BFILE("bfile", "BFILE", DefaultJavaType.JAVA_BYTES, CategoryEnum.OTHER),

    // XML
    XMLTYPE("xmltype", "XMLTYPE", DefaultJavaType.JAVA_STRING, CategoryEnum.TEXT),

    // 空间类型（多个变体名都指向同一个逻辑类型）
    SDO_GEOMETRY(DefaultJavaType.JAVA_GEOMETRY, CategoryEnum.GEOMETRY,
            "sdo_geometry", "SDO_GEOMETRY", "MDSYS.SDO_GEOMETRY", "mdsys.sdo_geometry");

    private final List<String> udtNames;
    private final String standardName;
    private final DefaultJavaType javaType;
    private final CategoryEnum category;

    private static final Map<String, OracleType> UDT_NAME_MAP = new HashMap<>();

    static {
        for (OracleType type : values()) {
            for (String name : type.udtNames) {
                UDT_NAME_MAP.put(name.toLowerCase(), type);
            }
        }
    }

    /**
     * 单一 udtName 的构造器
     */
    OracleType(String udtName, String standardName, DefaultJavaType javaType, CategoryEnum category) {
        this.udtNames = Arrays.asList(udtName);
        this.standardName = standardName;
        this.javaType = javaType;
        this.category = category;
    }

    /**
     * 多个 udtName 变体的构造器
     */
    OracleType(DefaultJavaType javaType, CategoryEnum category, String... udtNames) {
        this.udtNames = Arrays.asList(udtNames);
        this.standardName = this.name();
        this.javaType = javaType;
        this.category = category;
    }

    public static OracleType getByUdtName(String udtName) {
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
