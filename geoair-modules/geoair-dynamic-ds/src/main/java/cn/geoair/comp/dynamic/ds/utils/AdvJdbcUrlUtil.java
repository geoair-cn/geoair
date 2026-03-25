package cn.geoair.comp.dynamic.ds.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

/**
 * 创建人: 张逢吉 创建时间: 2025/9/30 09:28 描述: 将JDBC URL拆分为各个组件，支持解析参数为Map（适配PostgreSQL/MySQL等）
 * 支持URL格式：jdbc:postgresql://host:port/db?param1=value1&param2=value2 或
 * jdbc:derby://host:port/db;param1=value1;param2=value2
 */
public class AdvJdbcUrlUtil {

    public static AdvJdbcUrlUtil splitter(String jdbcUrl) {
        return new AdvJdbcUrlUtil(jdbcUrl);
    }

    // 基础组件
    @Getter public String driverName; // 驱动名（如postgresql、mysql、derby）

    @Getter public String host; // 主机地址

    @Getter public String port; // 端口号

    @Getter public String database; // 数据库名/实例名

    // 参数Map（key=参数名，value=参数值，保留原始顺序）
    @Getter public Map<String, String> params;

    /**
     * 解析JDBC URL
     *
     * @param jdbcUrl JDBC连接URL，例如：
     *     jdbc:postgresql://10.11.14.182:5432/bdh?currentSchema=onemap_tile_builder&characterEncoding=utf8&reWriteBatchedInserts=true
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

        // 3. 拆分连接URI和参数部分（处理?和;两种分隔符）
        String connUri; // 连接核心部分（//host:port/db）
        int paramSeparatorPos = -1;
        // 优先处理?分隔（PostgreSQL/MySQL主流格式），再处理;分隔（Derby等）
        if (jdbcUrl.contains("?")) {
            paramSeparatorPos = jdbcUrl.indexOf('?');
            connUri = jdbcUrl.substring(driverEndPos + 1, paramSeparatorPos);
            // 解析?后的参数
            parseParams(jdbcUrl.substring(paramSeparatorPos + 1), "&");
        } else if (jdbcUrl.contains(";")) {
            paramSeparatorPos = jdbcUrl.indexOf(';');
            connUri = jdbcUrl.substring(driverEndPos + 1, paramSeparatorPos);
            // 解析;后的参数
            parseParams(jdbcUrl.substring(paramSeparatorPos + 1), ";");
        } else {
            // 无参数的情况
            connUri = jdbcUrl.substring(driverEndPos + 1);
        }

        // 4. 解析连接URI（//host:port/db 或 本地路径/实例名）
        parseConnUri(connUri);
    }

    /**
     * 解析连接核心部分（//host:port/db 或 本地数据库路径）
     *
     * @param connUri 连接核心部分，例如：//10.11.14.182:5432/bdh
     */
    private void parseConnUri(String connUri) {
        // 处理带//的网络连接格式（主流数据库）
        if (connUri.startsWith("//")) {
            // 拆分主机+端口 和 数据库名（找第一个/）
            int dbStartPos = connUri.indexOf('/', 2);
            if (dbStartPos == -1) {
                throw new IllegalArgumentException("无效的JDBC URL：连接URI中未找到数据库名，格式应为//host:port/db");
            }

            // 提取主机+端口部分（如10.11.14.182:5432）
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
     * @param paramStr 参数字符串（如currentSchema=onemap_tile_builder&characterEncoding=utf8）
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

        // 处理带host/port的网络型数据库（PostgreSQL/MySQL/Derby网络版等）
        if (host != null) {
            sb.append("//").append(host);
            // 有端口则拼接端口
            if (port != null && !port.isEmpty()) {
                sb.append(":").append(port);
            }
            // 拼接数据库名
            if (database != null && !database.isEmpty()) {
                sb.append("/").append(database);
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
    public static void main(String[] args) {}
}
