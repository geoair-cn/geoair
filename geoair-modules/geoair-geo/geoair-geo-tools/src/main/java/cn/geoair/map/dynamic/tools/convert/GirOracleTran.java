package cn.geoair.map.dynamic.tools.convert;

/**
 * Oracle 环境判断工具类
 * <p>负责判断当前 Java 环境是否支持 Oracle 相关功能</p>
 *
 * @author zhangjun
 */
public class GirOracleTran {

    // Oracle Spatial 相关类名
    private static final String STRUCT_CLASS_NAME = "oracle.sql.STRUCT";
    private static final String JGEOMETRY_CLASS_NAME = "oracle.spatial.geometry.JGeometry";
    private static final String OracleJsonObject_CLASS_NAME = "oracle.sql.json.OracleJsonObject";

    // 缓存判断结果
    private static Boolean isOracleSpatialAvailable;
    private static Boolean isOracleJsonObjectAvailable;
    private static Boolean isStructClassAvailable;
    private static Boolean isJGeometryClassAvailable;

    /**
     * 判断 Oracle Spatial 是否可用（完整支持）
     *
     * @return true=可用，false=不可用
     */
    public static boolean isOracleSpatialAvailable() {
        if (isOracleSpatialAvailable == null) {
            isOracleSpatialAvailable = isStructClassAvailable() && isJGeometryClassAvailable();
        }
        return isOracleSpatialAvailable;
    }

    /**
     * 判断 OracleJsonObject 类是否可用 旧版本驱动是肯定没有这个类的
     *
     * @return true=可用，false=不可用
     */
    public static boolean isOracleJsonObjectAvailable() {
        if (isOracleJsonObjectAvailable == null) {
            try {
                Class.forName(OracleJsonObject_CLASS_NAME);
                isOracleJsonObjectAvailable = true;
            } catch (ClassNotFoundException e) {
                isOracleJsonObjectAvailable = false;
            }
        }
        return isOracleJsonObjectAvailable;
    }

    /**
     * 判断 STRUCT 类是否可用
     *
     * @return true=可用，false=不可用
     */
    public static boolean isStructClassAvailable() {
        if (isStructClassAvailable == null) {
            try {
                Class.forName(STRUCT_CLASS_NAME);
                isStructClassAvailable = true;
            } catch (ClassNotFoundException e) {
                isStructClassAvailable = false;
            }
        }
        return isStructClassAvailable;
    }

    /**
     * 判断 JGeometry 类是否可用
     *
     * @return true=可用，false=不可用
     */
    public static boolean isJGeometryClassAvailable() {
        if (isJGeometryClassAvailable == null) {
            try {
                Class.forName(JGEOMETRY_CLASS_NAME);
                isJGeometryClassAvailable = true;
            } catch (ClassNotFoundException e) {
                isJGeometryClassAvailable = false;
            }
        }
        return isJGeometryClassAvailable;
    }

    /**
     * 判断值是否为 Oracle STRUCT 类型（通过类名判断，避免直接引用）
     *
     * @param value 待检查的值
     * @return true=是 STRUCT 类型，false=不是
     */
    public static boolean isStructType(Object value) {
        if (value == null || !isStructClassAvailable()) {
            return false;
        }
        return STRUCT_CLASS_NAME.equals(value.getClass().getName());
    }

    /**
     * 判断值是否为 Oracle SDO_GEOMETRY 类型
     *
     * @param value 待检查的值
     * @return true=是 SDO_GEOMETRY 类型，false=不是
     */
    public static boolean isSdoGeometryType(Object value) {
        if (!isStructType(value)) {
            return false;
        }

        try {
            // 使用反射获取 SQL 类型名称
            Class<?> structClass = Class.forName(STRUCT_CLASS_NAME);
            java.lang.reflect.Method getSQLTypeNameMethod = structClass.getMethod("getSQLTypeName");
            String sqlTypeName = (String) getSQLTypeNameMethod.invoke(value);
            return "MDSYS.SDO_GEOMETRY".equalsIgnoreCase(sqlTypeName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取当前环境信息
     *
     * @return 环境信息字符串
     */
    public static String getEnvironmentInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Oracle 环境信息:\n");
        sb.append("  - STRUCT 类: ").append(isStructClassAvailable() ? "可用" : "不可用").append("\n");
        sb.append("  - JGeometry 类: ").append(isJGeometryClassAvailable() ? "可用" : "不可用").append("\n");
        sb.append("  - Oracle Spatial: ").append(isOracleSpatialAvailable() ? "可用" : "不可用");
        return sb.toString();
    }
}
