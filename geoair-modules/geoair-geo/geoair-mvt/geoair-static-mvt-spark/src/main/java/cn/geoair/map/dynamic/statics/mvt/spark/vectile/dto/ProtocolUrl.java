package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.io.Serializable;

/**
 * 自定义数据库连接协议的解析器和值对象。
 *
 * <h2>协议格式</h2>
 * <pre>
 * #jdbc:{subprotocol}://用户名#密码/主机:端口/数据库名[/模式名[/表名]]
 * </pre>
 * <p>
 * 以 {@code #jdbc:} 开头标识自定义协议，区别于标准 JDBC URL。
 * 路径结构与原 {@code pg://} 协议完全一致，仅前缀不同。
 *
 * <h2>各段说明</h2>
 * <table>
 *   <tr><th>段</th><th>必填</th><th>说明</th><th>示例</th></tr>
 *   <tr><td>#jdbc:{subprotocol}://</td><td>是</td><td>协议前缀，{@code #} 标识自定义协议，后面是标准 JDBC 子协议</td><td>{@code #jdbc:postgresql://}</td></tr>
 *   <tr><td>用户名#密码</td><td>是</td><td>{@code #} 分隔用户名和密码</td><td>{@code postgres#secret}</td></tr>
 *   <tr><td>主机:端口</td><td>是</td><td>数据库地址</td><td>{@code 10.0.0.1:5432}</td></tr>
 *   <tr><td>数据库名</td><td>是</td><td>数据库名称</td><td>{@code mydb}</td></tr>
 *   <tr><td>模式名</td><td>否</td><td>schema 名称。无 schema 的数据库（如 MySQL）使用空段 {@code //}</td><td>{@code public} 或留空</td></tr>
 *   <tr><td>表名</td><td>否</td><td>仅输出端使用，省略时仅连接不指定表</td><td>{@code tile_cache}</td></tr>
 * </table>
 *
 * <h2>示例</h2>
 * <pre>
 * #jdbc:postgresql://postgres#secret/10.0.0.1:5432/mydb/public/tile_cache
 * #jdbc:mysql://root#secret/10.0.0.1:3306/gisdb//tile_cache
 * #jdbc:sqlserver://sa#secret/10.0.0.1:1433/mydb/dbo/tile_cache
 * </pre>
 *
 * <h2>分段规则</h2>
 * <p>
 * 以 {@code //} 分隔前缀和路径，路径按 {@code /} 拆分后过滤空段：
 * </p>
 * <pre>
 * [0] = "user#pass"    (认证信息)
 * [1] = "host:port"    (地址)
 * [2] = "database"     (数据库名)
 * [3] = "schema"       (模式名，可选，空段表示无 schema)
 * [4] = "table"        (表名，可选)
 * </pre>
 *
 * @author refactored from PgUrl / PgConnectInfoWithTable
 */
@Getter
public final class ProtocolUrl implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PROTOCOL_PREFIX = "#jdbc:";

    private final String subProtocol;   // 如 "postgresql", "mysql", "sqlserver"
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String database;
    private final String schema;        // 可能为 null（MySQL 等无 schema 的数据库）
    private final String tableName;     // 可能为 null

    /**
     * 解析自定义协议格式的 URL。
     *
     * @param url 格式：#jdbc:{subprotocol}://user#pass/host:port/db[/schema[/table]]
     * @throws IllegalArgumentException 格式不合法时抛出
     */
    public ProtocolUrl(String url) {
        if (url == null || !url.startsWith(PROTOCOL_PREFIX)) {
            throw new IllegalArgumentException(
                    "URL 必须以 " + PROTOCOL_PREFIX + " 开头，实际值: " + url);
        }

        // 去掉前缀 #jdbc:，找到 "://" 分隔前缀和路径
        String afterPrefix = url.substring(PROTOCOL_PREFIX.length()); // "postgresql://user#pass/..."
        int schemeEnd = afterPrefix.indexOf("://");
        if (schemeEnd < 0) {
            throw new IllegalArgumentException(
                    "缺少 '://' 分隔符，格式示例: #jdbc:postgresql://user#pass/host:port/db");
        }

        this.subProtocol = afterPrefix.substring(0, schemeEnd); // "postgresql"
        String path = afterPrefix.substring(schemeEnd + 3);      // "user#pass/host:port/db/schema/table"

        // 按 / 拆分，过滤空段
        String[] parts = path.split("/");
        parts = java.util.Arrays.stream(parts)
                .filter(StrUtil::isNotBlank)
                .toArray(String[]::new);

        // 至少需要 3 段：[0]=user#pass [1]=host:port [2]=database
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "路径不完整，至少需要 user#pass/host:port/database，实际: " + url);
        }

        // [0] 认证：user#pass
        String auth = parts[0];
        int hashIdx = auth.indexOf('#');
        if (hashIdx < 0) {
            throw new IllegalArgumentException(
                    "用户名和密码之间缺少 '#' 分隔符，格式示例: #jdbc:postgresql://user#pass/...");
        }
        this.username = auth.substring(0, hashIdx);
        this.password = auth.substring(hashIdx + 1);
        if (StrUtil.isBlank(this.username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // [1] host:port
        String[] hostPort = parts[1].split(":");
        if (hostPort.length != 2) {
            throw new IllegalArgumentException(
                    "host:port 格式错误，期望 'host:port'，实际: " + parts[1]);
        }
        this.host = hostPort[0];
        try {
            this.port = Integer.parseInt(hostPort[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("端口必须是数字，实际: " + hostPort[1], e);
        }

        // [2] database
        this.database = parts[2];

        // [3] schema（可选，空字符串表示无 schema）
        this.schema = parts.length > 3 && StrUtil.isNotBlank(parts[3]) ? parts[3] : null;

        // [4] tableName（可选）
        this.tableName = parts.length > 4 && StrUtil.isNotBlank(parts[4]) ? parts[4] : null;
    }

    /**
     * 是否指定了表名。
     */
    public boolean hasTable() {
        return StrUtil.isNotBlank(tableName);
    }

    /**
     * 是否包含 schema。
     */
    public boolean hasSchema() {
        return schema != null;
    }

    /**
     * 获取用于 SQL 语句的表名（有 schema 时返回 "schema.table"）。
     */
    public String getTableForSql() {
        if (StrUtil.isNotBlank(tableName) && schema != null) {
            return schema + "." + tableName;
        }
        if (StrUtil.isNotBlank(tableName)) {
            return tableName;
        }
        return schema;
    }

    /**
     * 构建标准 JDBC URL（不含用户名密码，不含自定义表名段）。
     * <pre>
     * jdbc:postgresql://host:port/database?currentSchema=schema
     * jdbc:mysql://host:port/database
     * </pre>
     */
    public String toJdbcUrl() {
        StringBuilder sb = new StringBuilder("jdbc:").append(subProtocol)
                .append("://").append(host).append(':').append(port)
                .append('/').append(database);
        // 把原始 URL 中 database 后面的 ? 参数部分追加回来（如 currentSchema）
        String remainder = getOriginalPathAfterDatabase();
        int qIdx = remainder.indexOf('?');
        if (qIdx >= 0) {
            sb.append(remainder.substring(qIdx));
        }
        return sb.toString();
    }

    /**
     * 获取原始 URL 中 database 之后、table 之前的路径部分（含 ? 参数）。
     * <p>
     * 例如 {@code #jdbc:postgresql://u#p/h:5432/db?currentSchema=s/tbl} 返回 {@code "?currentSchema=s"}。
     */
    private String getOriginalPathAfterDatabase() {
        // 重新从原始路径中截取 database 之后的部分
        String afterPrefix = toString().substring(PROTOCOL_PREFIX.length());
        int schemeEnd = afterPrefix.indexOf("://");
        String path = afterPrefix.substring(schemeEnd + 3);

        // 找到 database 段之后的内容
        // 先按 / 拆分（不过滤空段，保留原始结构），跳过前 3 段（user#pass, host:port, database）
        String[] rawParts = path.split("/", 4); // 最多4段，第4段是 database 之后的所有内容
        if (rawParts.length > 3) {
            return rawParts[3];
        }
        return "";
    }

    /**
     * 还原为自定义协议字符串。
     * <pre>
     * #jdbc:postgresql://user#pass/host:port/database/schema/table
     * </pre>
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(PROTOCOL_PREFIX).append(subProtocol)
                .append("://").append(username).append('#').append(password)
                .append('/').append(host).append(':').append(port)
                .append('/').append(database);
        if (schema != null) {
            sb.append('/').append(schema);
        } else if (tableName != null) {
            sb.append('/'); // 无 schema 但有 table 时，用空段占位
        }
        if (tableName != null) {
            sb.append('/').append(tableName);
        }
        return sb.toString();
    }
}
