package cn.geoair.comp.code.generator.multi.utils;

/**
 * 代码生成通用常量
 *
 * @author ray
 */
public class GenConstants {

    /**
     * 数据库时间类型
     */
    public static final String[] COLUMNTYPE_TIME = {"datetime" , "time" , "date" , "timestamp"};

    /**
     * 数据库数字类型
     */
    public static final String[] COLUMNTYPE_NUMBER = {"tinyint" ,
            "smallint" , "mediumint" , "int" , "number" , "integer" , "int4" , "int8" ,
            "bit" , "bigint" , "float" , "double" , "decimal"};


    /**
     * 字符串类型
     */
    public static final String TYPE_STRING = "String" ;

    /**
     * 整型
     */
    public static final String TYPE_INTEGER = "Integer" ;

    /**
     * 整型
     */
    public static final String TYPE_BYTE = "byte[]" ;

    /**
     * 长整型
     */
    public static final String TYPE_LONG = "Long" ;

    /**
     * 浮点型
     */
    public static final String TYPE_DOUBLE = "Double" ;

    /**
     * 高精度计算类型
     */
    public static final String TYPE_BIGDECIMAL = "BigDecimal" ;

    /**
     * 时间类型
     */
    public static final String TYPE_DATE = "Date" ;


}
