package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.hutool.core.util.StrUtil;

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
 *
 * <h2>两种构造方式</h2>
 * <pre>
 * // 1. 从协议字符串解析
 * ProtocolUrl url = new ProtocolUrl("#jdbc:postgresql://postgres#secret/10.0.0.1:5432/mydb/public/tile_cache");
 *
 * // 2. 从零散参数构建
 * ProtocolUrl url = ProtocolUrl.builder()
 *     .subProtocol("postgresql")
 *     .username("postgres").password("secret")
 *     .host("10.0.0.1").port(5432)
 *     .database("mydb").schema("public").tableName("tile_cache")
 *     .build();
 * </pre>
 *
 * <h2>示例</h2>
 * <pre>
 * #jdbc:postgresql://postgres#secret/10.0.0.1:5432/mydb/public/tile_cache
 * #jdbc:mysql://root#secret/10.0.0.1:3306/gisdb//tile_cache
 * #jdbc:sqlserver://sa#secret/10.0.0.1:1433/mydb/dbo/tile_cache
 * </pre>
 *
 * @author refactored from PgUrl / PgConnectInfoWithTable
 */
public final class ProtocolUrl implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PROTOCOL_PREFIX = "#jdbc:";

    private final String subProtocol;
    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String database;
    private final String schema;
    private final String tableName;
    /** true = 从 builder 构建，toJdbcUrl() 用字段拼接；false = 从 URL 字符串解析，toJdbcUrl() 从原始 URL 还原 */
    private final boolean builtFromBuilder;

    // ===================== 从协议字符串解析 =====================

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

        this.builtFromBuilder = false;

        // 去掉前缀 #jdbc:，找到 "://" 分隔前缀和路径
        String afterPrefix = url.substring(PROTOCOL_PREFIX.length());
        int schemeEnd = afterPrefix.indexOf("://");
        if (schemeEnd < 0) {
            throw new IllegalArgumentException(
                    "缺少 '://' 分隔符，格式示例: #jdbc:postgresql://user#pass/host:port/db");
        }

        this.subProtocol = afterPrefix.substring(0, schemeEnd);
        String path = afterPrefix.substring(schemeEnd + 3);

        // 按 / 拆分，过滤空段
        String[] parts = path.split("/");
        parts = java.util.Arrays.stream(parts)
                .filter(StrUtil::isNotBlank)
                .toArray(String[]::new);

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

        // [3] schema（可选）
        this.schema = parts.length > 3 && StrUtil.isNotBlank(parts[3]) ? parts[3] : null;

        // [4] tableName（可选）
        this.tableName = parts.length > 4 && StrUtil.isNotBlank(parts[4]) ? parts[4] : null;
    }

    // ===================== 从 builder 构建 =====================

    private ProtocolUrl(Builder builder) {
        this.builtFromBuilder = true;
        this.subProtocol = builder.subProtocol;
        this.username = builder.username;
        this.password = builder.password;
        this.host = builder.host;
        this.port = builder.port;
        this.database = builder.database;
        this.schema = builder.schema;
        this.tableName = builder.tableName;
    }

    // ===================== Builder =====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String subProtocol = "postgresql";
        private String username;
        private String password;
        private String host;
        private int port = 5432;
        private String database;
        private String schema;
        private String tableName;

        public Builder subProtocol(String subProtocol) {
            this.subProtocol = subProtocol;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder database(String database) {
            this.database = database;
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public ProtocolUrl build() {
            if (StrUtil.isBlank(username)) {
                throw new IllegalArgumentException("username 不能为空");
            }
            if (StrUtil.isBlank(host)) {
                throw new IllegalArgumentException("host 不能为空");
            }
            if (StrUtil.isBlank(database)) {
                throw new IllegalArgumentException("database 不能为空");
            }
            return new ProtocolUrl(this);
        }
    }

    // ===================== 查询方法 =====================

    public String getSubProtocol() {
        return subProtocol;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getSchema() {
        return schema;
    }

    public String getTableName() {
        return tableName;
    }

    public boolean hasTable() {
        return StrUtil.isNotBlank(tableName);
    }

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

    // ===================== 输出 =====================

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
        if (builtFromBuilder) {
            // builder 构建的：从 schema 字段拼接 currentSchema 参数
            if (schema != null
                    && ("postgresql".equals(subProtocol) || "postgis".equals(subProtocol))) {
                sb.append("?currentSchema=").append(schema);
            }
        } else {
            // 解析的：从原始 URL 中还原 ? 参数部分（如 currentSchema）
            String remainder = getOriginalPathAfterDatabase();
            int qIdx = remainder.indexOf('?');
            if (qIdx >= 0) {
                sb.append(remainder.substring(qIdx));
            }
        }
        return sb.toString();
    }

    /**
     * 获取原始 URL 中 database 之后、table 之前的路径部分（含 ? 参数）。
     */
    private String getOriginalPathAfterDatabase() {
        String afterPrefix = toString().substring(PROTOCOL_PREFIX.length());
        int schemeEnd = afterPrefix.indexOf("://");
        String path = afterPrefix.substring(schemeEnd + 3);
        String[] rawParts = path.split("/", 4);
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
            sb.append('/');
        }
        if (tableName != null) {
            sb.append('/').append(tableName);
        }
        return sb.toString();
    }
}
