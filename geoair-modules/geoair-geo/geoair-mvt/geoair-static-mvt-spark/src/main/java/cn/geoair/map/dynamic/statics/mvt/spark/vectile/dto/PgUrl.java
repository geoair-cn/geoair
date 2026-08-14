package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.io.Serializable;

/**
 * pg:// 自定义数据库连接协议的解析器和值对象。
 *
 * <h2>协议格式</h2>
 * <pre>
 * pg://用户名#密码/主机:端口/数据库名[/模式名[/表名]]
 * </pre>
 *
 * <h2>各段说明</h2>
 * <table>
 *   <tr><th>段</th><th>必填</th><th>说明</th><th>示例</th></tr>
 *   <tr><td>pg://</td><td>是</td><td>协议前缀</td><td>{@code pg://}</td></tr>
 *   <tr><td>用户名#密码</td><td>是</td><td>{@code #} 分隔用户名和密码</td><td>{@code postgres#secret}</td></tr>
 *   <tr><td>主机:端口</td><td>是</td><td>数据库地址</td><td>{@code 10.0.0.1:5432}</td></tr>
 *   <tr><td>数据库名</td><td>是</td><td>PostgreSQL database 名称</td><td>{@code mydb}</td></tr>
 *   <tr><td>模式名</td><td>否</td><td>schema 名称，省略时使用数据库默认 schema</td><td>{@code public}</td></tr>
 *   <tr><td>表名</td><td>否</td><td>仅输出端使用，省略时仅连接不指定表</td><td>{@code tile_cache}</td></tr>
 * </table>
 *
 * <h2>完整示例</h2>
 * <pre>
 * pg://postgres#secret/10.0.0.1:5432/mydb
 * pg://postgres#secret/10.0.0.1:5432/mydb/public
 * pg://postgres#secret/10.0.0.1:5432/mydb/public/tile_cache
 * pg://postgres#tcsd1234/116.198.227.117:35432/address/test1/big_mian
 * </pre>
 *
 * <h2>分段规则</h2>
 * <p>
 * 按 {@code /} 拆分后，过滤空段，各段含义如下：
 * </p>
 * <pre>
 * [0] = "pg:"          (协议标识，已在 startsWith 校验中消费)
 * [1] = "user#pass"    (认证信息)
 * [2] = "host:port"    (地址)
 * [3] = "database"     (数据库名)
 * [4] = "schema"       (模式名，可选)
 * [5] = "table"        (表名，可选)
 * </pre>
 *
 * @author refactored from PgConnectInfoWithTable
 */
@Getter
public final class PgUrl implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String SCHEME = "pg://";

    private final String username;
    private final String password;
    private final String host;
    private final int port;
    private final String database;
    private final String schema;
    private final String tableName;

    /**
     * 解析 pg:// 协议格式的 URL。
     *
     * @param url 格式：pg://user#pass/host:port/db[/schema[/table]]
     * @throws IllegalArgumentException 格式不合法时抛出
     */
    public PgUrl(String url) {
        if (url == null || !url.startsWith(SCHEME)) {
            throw new IllegalArgumentException(
                    "URL 必须以 " + SCHEME + " 开头，实际值: " + url);
        }

        // 按 / 拆分，过滤空段（处理尾部 / 或连续 / 的情况）
        String[] parts = url.split("/");
        parts = java.util.Arrays.stream(parts)
                .filter(StrUtil::isNotBlank)
                .toArray(String[]::new);

        // parts 至少需要 4 段：[0]=pg: [1]=user#pass [2]=host:port [3]=database
        if (parts.length < 4) {
            throw new IllegalArgumentException(
                    "URL 路径不完整，至少需要 pg://user#pass/host:port/database，实际: " + url);
        }

        // [1] 解析认证：user#pass
        String auth = parts[1];
        int hashIdx = auth.indexOf('#');
        if (hashIdx < 0) {
            throw new IllegalArgumentException(
                    "用户名和密码之间缺少 '#' 分隔符，格式示例: pg://user#pass/host:port/db");
        }
        this.username = auth.substring(0, hashIdx);
        this.password = auth.substring(hashIdx + 1);
        if (StrUtil.isBlank(this.username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        // [2] 解析 host:port
        String[] hostPort = parts[2].split(":");
        if (hostPort.length != 2) {
            throw new IllegalArgumentException(
                    "host:port 格式错误，期望 'host:port'，实际: " + parts[2]);
        }
        this.host = hostPort[0];
        try {
            this.port = Integer.parseInt(hostPort[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "端口必须是数字，实际: " + hostPort[1], e);
        }

        // [3] database（必填）
        this.database = parts[3];

        // [4] schema（可选）
        this.schema = parts.length > 4 ? parts[4] : null;

        // [5] tableName（可选）
        this.tableName = parts.length > 5 ? parts[5] : null;
    }

    /**
     * 是否指定了表名。
     */
    public boolean hasTable() {
        return StrUtil.isNotBlank(tableName);
    }

    /**
     * 获取用于 SQL INSERT 的表名（schema.table 格式）。
     * <p>
     * 优先返回 {@code schema.table}，无表名时返回 {@code schema}，
     * 两者都为空时返回 null。
     */
    public String getTableForSql() {
        if (StrUtil.isNotBlank(tableName) && StrUtil.isNotBlank(schema)) {
            return schema + "." + tableName;
        }
        if (StrUtil.isNotBlank(tableName)) {
            return tableName;
        }
        return schema;
    }

    /**
     * 转换为 JDBC URL。
     * <pre>
     * jdbc:postgresql://host:port/database
     * jdbc:postgresql://host:port/database?currentSchema=schema
     * </pre>
     */
    public String toJdbcUrl() {
        StringBuilder sb = new StringBuilder("jdbc:postgresql://")
                .append(host).append(':').append(port).append('/').append(database);
        if (StrUtil.isNotBlank(schema)) {
            sb.append("?currentSchema=").append(schema);
        }
        return sb.toString();
    }

    /**
     * 还原为 pg:// 协议字符串。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(SCHEME)
                .append(username).append('#').append(password)
                .append('/').append(host).append(':').append(port)
                .append('/').append(database);
        if (StrUtil.isNotBlank(schema)) {
            sb.append('/').append(schema);
        }
        if (StrUtil.isNotBlank(tableName)) {
            sb.append('/').append(tableName);
        }
        return sb.toString();
    }
}
