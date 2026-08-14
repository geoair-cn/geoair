package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.DataSourceGetterFunction;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.DefaultDataSourceGetterFunction;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.sql.DataSource;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 统一的数据源配置。
 * <p>
 * 替代原有的 {@code PgConnectInfoSimple}、{@code PgConnectInfoWithTable}、{@code PgConnectInfo} 三个类。
 * 支持两种构造方式：
 * <ul>
 *   <li>JDBC 直连：通过 {@link #jdbcUrl}、{@link #username}、{@link #password} 构造</li>
 *   <li>pg:// 协议：通过 {@link #pgUrl} 字符串构造，自动解析出各字段</li>
 * </ul>
 *
 * @author refactored from PgConnectInfoSimple / PgConnectInfoWithTable
 */
@Data
@Accessors(chain = true)
public class DataSourceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===================== 连接信息 =====================

    /** JDBC URL，如 jdbc:postgresql://host:5432/dbname?currentSchema=schema */
    @JSONField(name = "url")
    private String jdbcUrl;

    @JSONField(name = "username")
    private String username;

    @JSONField(name = "password")
    private String password;

    @JSONField(name = "host")
    private String host;

    @JSONField(name = "port")
    private String port;

    @JSONField(name = "database")
    private String database;

    @JSONField(name = "schemaName")
    private String schemaName;

    // ===================== 表信息（输出端使用）=====================

    /** 目标表名（可选，仅输出端使用） */
    @JSONField(name = "tableName")
    private String tableName;

    // ===================== pg:// 协议 =====================

    /**
     * pg:// 协议 URL 字符串。设置此字段后，其他连接字段会自动从解析结果中填充。
     */
    @JSONField(name = "pgUrl")
    private transient String pgUrlStr;

    /** 解析后的 PgUrl 对象（缓存，不参与 JSON 序列化） */
    @com.alibaba.fastjson2.annotation.JSONField(serialize = false, deserialize = false)
    private transient PgUrl pgUrl;

    // ===================== DataSource 工厂 =====================

    /**
     * 可插拔的 DataSource 创建工厂。默认使用 {@link DefaultDataSourceGetterFunction}。
     */
    @com.alibaba.fastjson2.annotation.JSONField(serialize = false, deserialize = false)
    private DataSourceGetterFunction dataSourceFactory = new DefaultDataSourceGetterFunction();

    // ===================== 构造方式 =====================

    public DataSourceConfig() {
    }

    /**
     * 从 pg:// 协议 URL 构造。
     *
     * @param pgUrl pg:// 格式的 URL
     */
    public DataSourceConfig(String pgUrl) {
        this.pgUrlStr = pgUrl;
        applyPgUrl(new PgUrl(pgUrl));
    }

    /**
     * 从 JDBC 参数直接构造。
     */
    public DataSourceConfig(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    // ===================== pg:// 协议应用 =====================

    /**
     * 应用 PgUrl 解析结果到当前配置。
     */
    private void applyPgUrl(PgUrl parsed) {
        this.pgUrl = parsed;
        this.jdbcUrl = parsed.toJdbcUrl();
        this.username = parsed.getUsername();
        this.password = parsed.getPassword();
        this.host = parsed.getHost();
        this.port = String.valueOf(parsed.getPort());
        this.database = parsed.getDatabase();
        this.schemaName = parsed.getSchema();
        this.tableName = parsed.getTableName();
    }

    /**
     * 设置 pg:// 协议 URL（同时更新所有连接字段）。
     */
    public DataSourceConfig setPgUrl(String pgUrlStr) {
        this.pgUrlStr = pgUrlStr;
        if (pgUrlStr != null) {
            applyPgUrl(new PgUrl(pgUrlStr));
        }
        return this;
    }

    /**
     * 获取解析后的 PgUrl 对象（懒加载）。
     */
    public PgUrl getPgUrl() {
        if (pgUrl == null && pgUrlStr != null) {
            pgUrl = new PgUrl(pgUrlStr);
        }
        return pgUrl;
    }

    // ===================== DataSource =====================

    /**
     * 创建 DataSource（委托给可插拔的 {@link #dataSourceFactory}）。
     */
    public DataSource toDataSource() {
        return dataSourceFactory.apply(this);
    }

    // ===================== 参数导出 =====================

    /**
     * 导出为 Map 格式的参数，兼容 Spark JDBC 写入和内部工具使用。
     * <p>
     * 返回的 Map 包含：url, user, password, table, tableName。
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
     * <p>
     * 有 tableName 时返回 "schema.table"，无 tableName 时返回 schemaName，
     * 两者都为空时返回 null。
     */
    public String getTableNameForSql() {
        if (tableName != null && schemaName != null) {
            return schemaName + "." + tableName;
        }
        if (tableName != null) {
            return tableName;
        }
        return schemaName;
    }
}
