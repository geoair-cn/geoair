package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.DataSourceGetterFunction;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.DefaultDataSourceGetterFunction;
import com.alibaba.fastjson2.annotation.JSONField;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 统一的数据源配置。
 *
 * <p>支持两种构造方式：
 *
 * <ul>
 *   <li>自定义协议：通过 {@link #jdbcUrl} 传入 {@code #jdbc:postgresql://user#pass/host:port/db/schema/table}
 *       格式，自动解析
 *   <li>直接配置：通过 {@link #jdbcUrl}、{@link #username}、{@link #password}、{@link #tableName} 逐项设置
 * </ul>
 *
 * @author refactored from PgConnectInfoSimple / PgConnectInfoWithTable
 */
@Data
@Accessors(chain = true)
public class DataSourceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public static DataSourceConfig of() {
        return new DataSourceConfig();
    }

    /** 直接通过 JDBC 参数构造。 */
    public static DataSourceConfig of(String jdbcUrl, String username, String password) {
        DataSourceConfig config = new DataSourceConfig();
        config.jdbcUrl = jdbcUrl;
        config.username = username;
        config.password = password;
        return config;
    }

    /** 通过自定义协议 URL 字符串构造。 */
    public static DataSourceConfig fromProtocolUrlStr(String protocolUrl) {
        DataSourceConfig config = new DataSourceConfig();
        config.setProtocolUrlStr(protocolUrl);
        return config;
    }

    /** 通过已解析的 ProtocolUrl 对象构造（支持 builder 构建的 ProtocolUrl）。 */
    public static DataSourceConfig fromProtocolUrlObj(ProtocolUrl parsed) {
        DataSourceConfig config = new DataSourceConfig();
        config.protocolUrlStr = parsed.toString();
        config.jdbcUrl = parsed.toJdbcUrl();
        config.username = parsed.getUsername();
        config.password = parsed.getPassword();
        config.tableName = parsed.getTableName();
        config.parsedUrl = parsed;
        return config;
    }

    public DataSourceConfig setProtocolUrlStr(String protocolUrlStr) {
        if (GutilObject.isNotEmpty(protocolUrlStr)) {
            ProtocolUrl parsed = new ProtocolUrl(protocolUrlStr);
            this.protocolUrlStr = protocolUrlStr;
            this.jdbcUrl = parsed.toJdbcUrl();
            this.username = parsed.getUsername();
            this.password = parsed.getPassword();
            this.tableName = parsed.getTableName();
            this.parsedUrl = parsed;
            return this;
        }
        return this;
    }

    // ===================== 连接信息 =====================

    /** 原始自定义协议 URL（序列化用，反序列化时从此重建 parsedUrl） */
    @JSONField(name = "protocolUrlStr")
    private String protocolUrlStr;

    /** JDBC URL（标准格式，不含用户名密码） */
    @JSONField(name = "jdbcUrl")
    private String jdbcUrl;

    @JSONField(name = "username")
    private String username;

    @JSONField(name = "password")
    private String password;

    // ===================== 表信息（输出端使用）=====================

    /** 目标表名（可选，仅输出端使用） */
    @JSONField(name = "tableName")
    private String tableName;

    // ===================== 缓存 =====================

    /** 解析后的 ProtocolUrl 对象（不参与 JSON 序列化，反序列化时从 protocolUrlStr 重建） */
    @JSONField(serialize = false, deserialize = false)
    private transient ProtocolUrl parsedUrl;

    // ===================== DataSource 工厂 =====================

    /** 可插拔的 DataSource 创建工厂。默认使用 {@link DefaultDataSourceGetterFunction}。 */
    @JSONField(serialize = false, deserialize = false)
    private DataSourceGetterFunction dataSourceFactory = new DefaultDataSourceGetterFunction();

    // ===================== 构造方式 =====================

    public DataSourceConfig() {}

    public DataSourceConfig(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * 反序列化后重建 parsedUrl（transient 字段在反序列化后为 null）。 优先从 protocolUrl 重建，否则从 jdbcUrl + tableName 重建。
     */
    private Object readResolve() {
        if (protocolUrlStr != null) {
            parsedUrl = new ProtocolUrl(protocolUrlStr);
        }
        if (dataSourceFactory == null) {
            dataSourceFactory = new DefaultDataSourceGetterFunction();
        }
        return this;
    }

    // ===================== ProtocolUrl 解析 =====================

    /** 获取解析后的 ProtocolUrl（如果通过 fromProtocol 构造则有值）。 */
    public ProtocolUrl getParsedUrl() {
        return parsedUrl;
    }

    /** 获取 host（仅当通过 fromProtocol 构造时可用）。 */
    public String getHost() {
        return parsedUrl != null ? parsedUrl.getHost() : null;
    }

    /** 获取 port（仅当通过 fromProtocol 构造时可用）。 */
    public int getPort() {
        return parsedUrl != null ? parsedUrl.getPort() : 0;
    }

    /** 获取 database 名称（仅当通过 fromProtocol 构造时可用）。 */
    public String getDatabase() {
        return parsedUrl != null ? parsedUrl.getDatabase() : null;
    }

    /** 获取 schema 名称（仅当通过 fromProtocol 构造且数据库支持 schema 时可用）。 */
    public String getSchemaName() {
        return parsedUrl != null ? parsedUrl.getSchema() : null;
    }

    // ===================== DataSource =====================

    /** 创建 DataSource（委托给可插拔的 {@link #dataSourceFactory}）。 */
    public DataSource toDataSource() {
        return dataSourceFactory.apply(this);
    }

    // ===================== 参数导出 =====================

    /**
     * 导出为 Map 格式的参数，兼容 Spark JDBC 写入和内部工具使用。
     *
     * <p>返回的 Map 包含：url, user, password, table, tableName。
     */
    public Map<String, String> toParams() {
        Map<String, String> params = new HashMap<>();
        params.put("url", getJdbcUrl());
        params.put("user", getUsername());
        params.put("password", getPassword());
        if (tableName != null) {
            params.put("table", tableName);
        }
        params.put("tableName", getTableNameForSql());
        return params;
    }

    /**
     * 获取用于 SQL 语句的表名。
     *
     * <p>有 tableName 且有 schema 时返回 "schema.table"， 有 tableName 无 schema 时返回 tableName， 无 tableName
     * 时返回 schema（可能为 null）。
     */
    public String getTableNameForSql() {
        if (parsedUrl != null) {
            // 有自定义协议解析结果时，用 ProtocolUrl 的逻辑（考虑 schema）
            String sqlTable = parsedUrl.getTableForSql();
            // 如果 ProtocolUrl 的 tableForSql 为 null（没有 schema 也没有 table），回退到 tableName
            return sqlTable != null ? sqlTable : tableName;
        }
        return tableName;
    }
}
