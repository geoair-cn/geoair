package cn.geoair.comp.dynamic.ds.utils;

import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

/**
 * 创建人: 张逢吉 创建时间: 2025/9/30 09:28 描述: 将JDBC URL拆分为各个组件，支持解析参数为Map（适配PostgreSQL/MySQL等）
 * 支持URL格式：jdbc:postgresql://host:port/db?param1=value1&param2=value2 或
 * jdbc:derby://host:port/db;param1=value1;param2=value2 或
 * jdbc:oracle:thin:@//host:port/service?param=value
 */
public class AdvJdbcUrlUtil {

    /**
     * 为 JDBC URL 追加 schema 参数
     *
     * @param jdbcUrl 原始 JDBC URL
     * @param schema  要设置的 schema 名称
     * @return 包含 schema 参数的 JDBC URL
     */
    public static String appendSchema(String jdbcUrl, String schema) {
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            throw new IllegalArgumentException("jdbcUrl cannot be null or empty");
        }
        if (schema == null || schema.isEmpty()) {
            return jdbcUrl;
        }

        // URL 编码 schema 名称（防止特殊字符）
        String encodedSchema = URLEncoder.encode(schema);

        // 检查是否已经包含 schema 参数
        String existingSchema = extractSchema(jdbcUrl);
        if (existingSchema != null) {
            // 如果已存在，替换
            return replaceSchemaParameter(jdbcUrl, encodedSchema);
        }

        String separator = jdbcUrl.contains("?") ? "&" : "?";
        String paramName = detectSchemaParamName(jdbcUrl);

        return jdbcUrl + separator + paramName + "=" + encodedSchema;
    }

    /**
     * 检测应该使用的 schema 参数名
     */
    private static String detectSchemaParamName(String jdbcUrl) {
        String lowerUrl = jdbcUrl.toLowerCase();

        if (lowerUrl.startsWith("jdbc:postgresql:")) {
            return "currentSchema";
        }
        if (lowerUrl.startsWith("jdbc:oracle:")) {
            return "defaultSchema";
        }
        if (lowerUrl.startsWith("jdbc:sqlserver:")) {
            return "schemaName";
        }
        if (lowerUrl.startsWith("jdbc:h2:")) {
            return "schema";
        }
        // 默认
        return "currentSchema";
    }

    /**
     * 从 JDBC URL 中提取已有的 schema 参数
     */
    private static String extractSchema(String jdbcUrl) {
        String[] paramNames = {"currentSchema", "schema", "schemaName", "defaultSchema"};
        for (String paramName : paramNames) {
            String pattern = paramName + "=([^&]*)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(jdbcUrl);
            if (m.find()) {
                return m.group(1);
            }
        }
        return null;
    }

    /**
     * 替换已有的 schema 参数
     */
    private static String replaceSchemaParameter(String jdbcUrl, String newSchema) {
        String[] paramNames = {"currentSchema", "schema", "schemaName", "defaultSchema"};
        for (String paramName : paramNames) {
            String pattern = paramName + "=[^&]*";
            if (jdbcUrl.matches(".*" + pattern + ".*")) {
                return jdbcUrl.replaceAll(pattern, paramName + "=" + newSchema);
            }
        }
        return jdbcUrl;
    }


    public static AdvJdbcUrlUtil splitter(String jdbcUrl) {
        return new AdvJdbcUrlUtil(jdbcUrl);
    }

    // 基础组件
    @Getter
    public String driverName; // 驱动名（如postgresql、mysql、derby）

    @Getter
    public String subProtocol; // 子协议前缀（如 oracle 的 thin、oci 等）

    @Getter
    public boolean oracleServiceNameFormat; // Oracle Service Name 格式（@//host:port/service）vs SID 格式（@host:port:sid）

    @Getter
    public String host; // 主机地址

    @Getter
    public String port; // 端口号

    @Getter
    public String database; // 数据库名/实例名

    // 参数Map（key=参数名，value=参数值，保留原始顺序）
    @Getter
    public Map<String, String> params;

    /**
     * 解析JDBC URL
     *
     * @param jdbcUrl JDBC连接URL，例如：
     *                jdbc:postgresql://10.11.14.182:5432/bdh?currentSchema=onemap_tile_builder&characterEncoding=utf8&reWriteBatchedInserts=true
     */
    public AdvJdbcUrlUtil(String jdbcUrl) {
        // 初始化参数Map
        this.params = new LinkedHashMap<>();

        // 1. 校验URL合法性
        if (jdbcUrl == null || !jdbcUrl.startsWith("jdbc:")) {
            throw new IllegalArgumentException("无效的JDBC URL：URL必须以jdbc:开头");
        }

        // 2. 提取驱动名（jdbc:xxx:... 中的xxx）
        int driverEndPos = jdbcUrl.indexOf(':', 5); // 从jdbc:后第1位开始找第一个:
        if (driverEndPos == -1) {
            throw new IllegalArgumentException(
                    "无效的JDBC URL：无法提取驱动名，格式应为jdbc:driver://host:port/db?params");
        }
        this.driverName = jdbcUrl.substring(5, driverEndPos);

        // 3. 处理 Oracle 子协议前缀（如 thin、oci）
        String afterDriver = jdbcUrl.substring(driverEndPos + 1);
        this.subProtocol = null;
        this.oracleServiceNameFormat = false;
        if ("oracle".equalsIgnoreCase(driverName) && afterDriver.indexOf(':') > 0) {
            // Oracle URL 格式: jdbc:oracle:thin:@//host:port/service 或 jdbc:oracle:thin:@host:port:sid
            int subProtoEnd = afterDriver.indexOf(':');
            this.subProtocol = afterDriver.substring(0, subProtoEnd);
            // 截掉子协议，让后续逻辑处理 "@//host:port/db" 部分
            afterDriver = afterDriver.substring(subProtoEnd + 1);
        }

        // 4. 拆分连接URI和参数部分（处理?和;两种分隔符）
        String connUri; // 连接核心部分（//host:port/db）
        int paramSeparatorPos = -1;
        // 优先处理?分隔（PostgreSQL/MySQL主流格式），再处理;分隔（Derby等）
        if (afterDriver.contains("?")) {
            paramSeparatorPos = afterDriver.indexOf('?');
            connUri = afterDriver.substring(0, paramSeparatorPos);
            // 解析?后的参数
            parseParams(afterDriver.substring(paramSeparatorPos + 1), "&");
        } else if (afterDriver.contains(";")) {
            paramSeparatorPos = afterDriver.indexOf(';');
            connUri = afterDriver.substring(0, paramSeparatorPos);
            // 解析;后的参数
            parseParams(afterDriver.substring(paramSeparatorPos + 1), ";");
        } else {
            // 无参数的情况
            connUri = afterDriver;
        }

        // 5. 解析连接URI（//host:port/db 或 @//host:port/db 等Oracle格式）
        parseConnUri(connUri);
    }

    /**
     * 解析连接核心部分（//host:port/db 或 本地数据库路径）
     *
     * @param connUri 连接核心部分，例如：//10.11.14.182:5432/bdh 或 @//192.168.0.39:1522/XEPDB1
     */
    private void parseConnUri(String connUri) {
        // 处理 Oracle @ 前缀格式：@//host:port/service 或 @host:port:sid
        if (connUri.startsWith("@//")) {
            this.oracleServiceNameFormat = true;
            connUri = connUri.substring(1); // 去掉 @，剩下 //host:port/service
        } else if (connUri.startsWith("@")) {
            this.oracleServiceNameFormat = false;
            connUri = connUri.substring(1); // 去掉 @，剩下 host:port:sid
        }

        // 处理 Oracle SID 格式: host:port:sid（无 // 前缀，用 : 分隔且不含 /）
        if (!connUri.startsWith("//") && connUri.contains(":") && !connUri.contains("/")) {
            String[] parts = connUri.split(":");
            if (parts.length >= 3) {
                this.host = parts[0];
                this.port = parts[1];
                this.database = parts[2];
                return;
            }
        }

        // 处理带//的网络连接格式（主流数据库 + Oracle Service Name）
        if (connUri.startsWith("//")) {
            // 拆分主机+端口 和 数据库名（找第一个/）
            int dbStartPos = connUri.indexOf('/', 2);
            if (dbStartPos == -1) {
                throw new IllegalArgumentException("无效的JDBC URL：连接URI中未找到数据库名，格式应为//host:port/db");
            }

            // 提取主机+端口部分
            String hostPort = connUri.substring(2, dbStartPos);
            // 提取数据库名（如bdh）
            this.database = connUri.substring(dbStartPos + 1);

            // 拆分主机和端口
            int portSeparatorPos = hostPort.indexOf(':');
            if (portSeparatorPos != -1) {
                this.host = hostPort.substring(0, portSeparatorPos);
                this.port = hostPort.substring(portSeparatorPos + 1);
            } else {
                // 无端口时，主机为完整hostPort，端口设为null
                this.host = hostPort;
                this.port = null;
            }
        } else {
            // 无//的格式（如本地数据库、Derby嵌入式）
            this.database = connUri;
            this.host = null;
            this.port = null;
        }
    }

    /**
     * 解析参数字符串为Map
     *
     * @param paramStr  参数字符串（如currentSchema=onemap_tile_builder&characterEncoding=utf8）
     * @param separator 参数分隔符（&或;）
     */
    private void parseParams(String paramStr, String separator) {
        if (paramStr == null || paramStr.isEmpty()) {
            return;
        }
        // 按分隔符拆分参数
        String[] paramArray = paramStr.split(separator);
        for (String param : paramArray) {
            if (param.isEmpty()) {
                continue; // 跳过空参数
            }
            // 拆分参数名和值（处理无值的情况，如?useSSL=true&autoReconnect）
            int eqPos = param.indexOf('=');
            if (eqPos == -1) {
                this.params.put(param.trim(), ""); // 无值时value设为空字符串
            } else {
                String key = param.substring(0, eqPos).trim();
                String value = param.substring(eqPos + 1).trim();
                this.params.put(key, value);
            }
        }
    }

    /**
     * 新增方法：获取不带参数的核心JDBC URL字符串 示例：jdbc:postgresql://10.11.14.182:5432/bdh
     *
     * @return 仅包含驱动、主机、端口、数据库名的JDBC URL（无任何参数）
     */
    public String getJdbcUrlWithoutParams() {
        // 拼接核心URL的基础部分
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:").append(driverName).append(":");

        // Oracle 特殊处理：需要把子协议前缀拼回去
        boolean isOracle = "oracle".equalsIgnoreCase(driverName);
        if (isOracle && subProtocol != null) {
            sb.append(subProtocol).append(":");
        }

        // 处理带host/port的网络型数据库（PostgreSQL/MySQL/Derby网络版等）
        if (host != null) {
            // Oracle 需要 @ 前缀
            if (isOracle) {
                sb.append("@");
                if (oracleServiceNameFormat) {
                    sb.append("//"); // Service Name 格式: @//host:port/service
                }
            } else {
                sb.append("//");
            }
            sb.append(host);
            // 有端口则拼接端口
            if (port != null && !port.isEmpty()) {
                sb.append(":").append(port);
            }
            // 拼接数据库名
            if (database != null && !database.isEmpty()) {
                if (isOracle && !oracleServiceNameFormat) {
                    // SID 格式用 : 分隔
                    sb.append(":").append(database);
                } else {
                    sb.append("/").append(database);
                }
            }
        } else {
            // 处理无host/port的嵌入式数据库（如Derby嵌入式、SQLite等）
            if (database != null && !database.isEmpty()) {
                sb.append(database);
            }
        }

        return sb.toString();
    }

    // 测试方法
    public static void main(String[] args) {

        System.out.println(appendSchema("jdbc:postgresql://localhost:5432/mydb", "public"));
        System.out.println(appendSchema("jdbc:postgresql://localhost:5432/mydb?currentSchema=old", "public"));

        // Oracle URL 解析测试
        testUrl("jdbc:oracle:thin:@//192.168.0.39:1522/XEPDB1");
        testUrl("jdbc:oracle:thin:@192.168.0.39:1522:XEPDB1");
        testUrl("jdbc:oracle:thin:@//192.168.0.39:1522/XEPDB1?param=value");
    }

    private static void testUrl(String jdbcUrl) {
        System.out.println("=== " + jdbcUrl);
        AdvJdbcUrlUtil u = splitter(jdbcUrl);
        System.out.println("  driverName=" + u.driverName + " subProtocol=" + u.subProtocol +
                " oracleSvcFormat=" + u.oracleServiceNameFormat);
        System.out.println("  host=" + u.host + " port=" + u.port + " db=" + u.database);
        System.out.println("  params=" + u.params);
        System.out.println("  withoutParams=" + u.getJdbcUrlWithoutParams());
    }
}
