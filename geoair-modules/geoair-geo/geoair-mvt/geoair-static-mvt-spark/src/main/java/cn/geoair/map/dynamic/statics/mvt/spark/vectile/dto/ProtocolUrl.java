package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.geoair.comp.jdbc.url.GirJdbcUrlCodecs;
import cn.geoair.comp.jdbc.url.JdbcUrlCodec;
import cn.geoair.comp.jdbc.url.beans.JdbcUrl;
import cn.geoair.comp.jdbc.url.enums.DatabaseType;
import cn.hutool.core.util.StrUtil;
import java.io.Serializable;
import java.util.Locale;

/**
 * 静态切片任务约定的数据库连接协议解析器和值对象。
 *
 * <p>协议格式由外部约定，必须保持为： {@code #jdbc:{subprotocol}://用户名#密码/主机:端口/数据库名[/模式名[/表名]]}。
 * 本类只负责该任务协议中的用户名、密码、schema 和表名；标准 JDBC URL 的构建委托给 {@link JdbcUrlCodec}，避免在此重复维护各数据库的 URL 方言。
 *
 * @author refactored from PgUrl / PgConnectInfoWithTable
 */
public final class ProtocolUrl implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PROTOCOL_PREFIX = "#jdbc:";

    /** 第三方协议中声明的子协议，保留原始值以便无损输出。 */
    private final String subProtocol;
    /** 数据库用户名。 */
    private final String username;
    /** 数据库密码。 */
    private final String password;
    /** 主机名或方括号包裹的 IPv6 地址。 */
    private final String host;
    /** 端口号。 */
    private final int port;
    /** 数据库名称。 */
    private final String database;
    /** 标准 JDBC URL 中数据库名后的原始参数，例如 {@code ?sslmode=require}。 */
    private final String jdbcProperties;
    /** 协议中的可选 schema 段。 */
    private final String schema;
    /** 协议中的可选表名段。 */
    private final String tableName;

    /**
     * 解析第三方约定的任务协议。
     *
     * @param url 协议字符串：{@code #jdbc:{subprotocol}://user#password/host:port/db[/schema[/table]]}
     */
    public ProtocolUrl(String url) {
        if (StrUtil.isBlank(url) || !url.startsWith(PROTOCOL_PREFIX)) {
            throw new IllegalArgumentException("URL 必须以 " + PROTOCOL_PREFIX + " 开头，实际值: " + url);
        }

        String afterPrefix = url.substring(PROTOCOL_PREFIX.length());
        int schemeEnd = afterPrefix.indexOf("://");
        if (schemeEnd <= 0) {
            throw new IllegalArgumentException(
                    "缺少子协议或 '://' 分隔符，格式示例: "
                            + "#jdbc:postgresql://user#password/host:port/database");
        }
        this.subProtocol = afterPrefix.substring(0, schemeEnd);

        String[] parts = afterPrefix.substring(schemeEnd + 3).split("/", -1);
        if (parts.length < 3 || parts.length > 5) {
            throw new IllegalArgumentException(
                    "路径应为 user#password/host:port/database[/schema[/table]]，实际: " + url);
        }

        String auth = parts[0];
        int passwordSeparator = auth.indexOf('#');
        if (passwordSeparator < 0) {
            throw new IllegalArgumentException("用户名和密码之间缺少 '#' 分隔符");
        }
        this.username = auth.substring(0, passwordSeparator);
        this.password = auth.substring(passwordSeparator + 1);
        if (StrUtil.isBlank(this.username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        HostPort hostPort = parseHostPort(parts[1]);
        this.host = hostPort.host;
        this.port = hostPort.port;

        DatabaseAndProperties databaseAndProperties = splitDatabaseProperties(parts[2]);
        if (StrUtil.isBlank(databaseAndProperties.database)) {
            throw new IllegalArgumentException("数据库名不能为空");
        }
        this.database = databaseAndProperties.database;
        this.jdbcProperties = databaseAndProperties.properties;
        this.schema = parts.length >= 4 && StrUtil.isNotBlank(parts[3]) ? parts[3] : null;
        this.tableName = parts.length == 5 && StrUtil.isNotBlank(parts[4]) ? parts[4] : null;
    }

    private ProtocolUrl(Builder builder) {
        this.subProtocol = builder.subProtocol;
        this.username = builder.username;
        this.password = builder.password;
        this.host = builder.host;
        this.port = builder.port;
        this.database = builder.database;
        this.jdbcProperties = "";
        this.schema = builder.schema;
        this.tableName = builder.tableName;
        validateState();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 第三方协议的构造器，字段顺序与字符串协议保持一致。 */
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
            return new ProtocolUrl(this);
        }
    }

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

    /** 获取 SQL 使用的表名；schema 存在时返回 {@code schema.table}。 */
    public String getTableForSql() {
        if (StrUtil.isNotBlank(tableName) && schema != null) {
            return schema + "." + tableName;
        }
        return StrUtil.isNotBlank(tableName) ? tableName : schema;
    }

    /**
     * 将第三方协议的连接部分转换为标准 JDBC URL，不输出用户名、密码和任务表名。
     *
     * <p>{@code postgis} 是任务协议允许的 PostgreSQL 别名，输出时会归一化为 JDBC 驱动实际使用的 {@code postgresql}。其它 URL 细节由
     * {@code geoair-jdbc-url} 的方言实现负责。
     */
    public String toJdbcUrl() {
        JdbcUrlCodec codec = GirJdbcUrlCodecs.defaultCodec();
        DatabaseType databaseType = resolveDatabaseType();
        JdbcUrl jdbcUrl = codec.create(databaseType, host, Integer.valueOf(port), database);
        String result = codec.format(jdbcUrl) + jdbcProperties;
        if (StrUtil.isNotBlank(schema) && supportsSchemaRewrite(databaseType)) {
            return codec.rewriteSchema(result, schema);
        }
        return result;
    }

    /** 按约定还原协议字符串；空 schema 段会保留，确保 {@code //table} 不丢失表名语义。 */
    @Override
    public String toString() {
        StringBuilder result =
                new StringBuilder(PROTOCOL_PREFIX)
                        .append(subProtocol)
                        .append("://")
                        .append(username)
                        .append('#')
                        .append(password)
                        .append('/')
                        .append(host)
                        .append(':')
                        .append(port)
                        .append('/')
                        .append(database)
                        .append(jdbcProperties);
        if (schema != null) {
            result.append('/').append(schema);
        } else if (tableName != null) {
            result.append('/');
        }
        if (tableName != null) {
            result.append('/').append(tableName);
        }
        return result.toString();
    }

    private void validateState() {
        if (StrUtil.isBlank(subProtocol)) {
            throw new IllegalArgumentException("子协议不能为空");
        }
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (StrUtil.isBlank(host)) {
            throw new IllegalArgumentException("主机不能为空");
        }
        if (StrUtil.isBlank(database)) {
            throw new IllegalArgumentException("数据库名不能为空");
        }
        validatePort(port);
        validateHost(host);
    }

    private DatabaseType resolveDatabaseType() {
        String jdbcDriverName =
                "postgis".equalsIgnoreCase(subProtocol) ? "postgresql" : subProtocol;
        DatabaseType databaseType =
                DatabaseType.fromJdbcDriverName(jdbcDriverName.toLowerCase(Locale.ROOT));
        if (databaseType == DatabaseType.UNKNOWN) {
            throw new IllegalArgumentException("不支持生成 JDBC URL 的子协议: " + subProtocol);
        }
        return databaseType;
    }

    private static boolean supportsSchemaRewrite(DatabaseType databaseType) {
        return databaseType == DatabaseType.POSTGRESQL
                || databaseType == DatabaseType.ORACLE
                || databaseType == DatabaseType.SQLSERVER
                || databaseType == DatabaseType.H2;
    }

    private static HostPort parseHostPort(String value) {
        int separator = value == null ? -1 : value.lastIndexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            throw new IllegalArgumentException("host:port 格式错误，实际: " + value);
        }
        String host = value.substring(0, separator);
        validateHost(host);
        int port;
        try {
            port = Integer.parseInt(value.substring(separator + 1));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("端口必须是数字，实际: " + value.substring(separator + 1), ex);
        }
        validatePort(port);
        return new HostPort(host, port);
    }

    private static DatabaseAndProperties splitDatabaseProperties(String value) {
        int queryIndex = value == null ? -1 : value.indexOf('?');
        if (queryIndex < 0) {
            return new DatabaseAndProperties(value, "");
        }
        return new DatabaseAndProperties(
                value.substring(0, queryIndex), value.substring(queryIndex));
    }

    private static void validateHost(String host) {
        if (StrUtil.isBlank(host)) {
            throw new IllegalArgumentException("主机不能为空");
        }
        boolean containsColon = host.indexOf(':') >= 0;
        boolean bracketedIpv6 = host.startsWith("[") && host.endsWith("]");
        if (containsColon && !bracketedIpv6) {
            throw new IllegalArgumentException("IPv6 主机必须使用方括号，例如 [2001:db8::1]");
        }
    }

    private static void validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("端口必须在 1 到 65535 之间，实际: " + port);
        }
    }

    private static final class HostPort {
        private final String host;
        private final int port;

        private HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    private static final class DatabaseAndProperties {
        private final String database;
        private final String properties;

        private DatabaseAndProperties(String database, String properties) {
            this.database = database;
            this.properties = properties;
        }
    }
}
