package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.reader.ObjectReader;

import java.lang.reflect.Type;

/**
 * DataSourceConfig 的自定义反序列化器，用于兼容旧版 JSON 格式。
 * <p>
 * 旧版 JSON 使用 PgConnectInfoSimple / PgConnectInfoWithTable 的字段名：
 * <pre>
 * {"ip":"10.0.0.1","port":"5432","userName":"postgres","passwd":"secret","dbName":"mydb","schemaName":"public","tableName":"tile_cache"}
 * </pre>
 * <p>
 * 新版 JSON 使用 DataSourceConfig 的字段名：
 * <pre>
 * {"jdbcUrl":"jdbc:postgresql://...","username":"postgres","password":"secret","host":"10.0.0.1","port":"5432","database":"mydb","schemaName":"public","tableName":"tile_cache"}
 * </pre>
 * <p>
 * 也支持 pg:// 协议格式：
 * <pre>
 * {"pgUrl":"pg://postgres#secret/10.0.0.1:5432/mydb/public/tile_cache"}
 * </pre>
 */
public class DataSourceConfigDeserializer implements ObjectReader<DataSourceConfig> {

    @Override
    public DataSourceConfig readObject(
            com.alibaba.fastjson2.JSONReader reader,
            Type fieldType,
            Object fieldName, long features) {

        JSONObject json = reader.readJSONObject();
        if (json == null) {
            return null;
        }

        DataSourceConfig config = new DataSourceConfig();

        // 优先级1：pg:// 协议 URL（最高优先级，解析后覆盖所有字段）
        String pgUrl = json.getString("pgUrl");
        if (StrUtil.isNotBlank(pgUrl)) {
            config.setPgUrl(pgUrl);
        }

        // 优先级2：新版字段名（jdbcUrl, username, password, host, port, database）
        String jdbcUrl = json.getString("jdbcUrl");
        if (jdbcUrl != null) {
            config.setJdbcUrl(jdbcUrl);
        }
        if (json.getString("username") != null) {
            config.setUsername(json.getString("username"));
        }
        if (json.getString("password") != null) {
            config.setPassword(json.getString("password"));
        }
        if (json.getString("host") != null) {
            config.setHost(json.getString("host"));
        }
        if (json.getString("port") != null) {
            config.setPort(json.getString("port"));
        }
        if (json.getString("database") != null) {
            config.setDatabase(json.getString("database"));
        }
        if (json.getString("schemaName") != null) {
            config.setSchemaName(json.getString("schemaName"));
        }
        if (json.getString("tableName") != null) {
            config.setTableName(json.getString("tableName"));
        }

        // 优先级3：旧版字段名（仅当新版字段未设置时回退）
        if (config.getUsername() == null && json.getString("userName") != null) {
            config.setUsername(json.getString("userName"));
        }
        if (config.getPassword() == null && json.getString("passwd") != null) {
            config.setPassword(json.getString("passwd"));
        }
        if (config.getHost() == null && json.getString("ip") != null) {
            config.setHost(json.getString("ip"));
        }
        if (config.getPort() == null && json.getString("port") != null) {
            config.setPort(json.getString("port"));
        }
        if (config.getDatabase() == null && json.getString("dbName") != null) {
            config.setDatabase(json.getString("dbName"));
        }

        // 如果有 host 但没有 jdbcUrl，自动构建
        if (config.getJdbcUrl() == null && config.getHost() != null) {
            String port = config.getPort() != null ? config.getPort() : "5432";
            String db = config.getDatabase() != null ? config.getDatabase() : "";
            StringBuilder sb = new StringBuilder("jdbc:postgresql://")
                    .append(config.getHost()).append(':').append(port).append('/').append(db);
            if (config.getSchemaName() != null) {
                sb.append("?currentSchema=").append(config.getSchemaName());
            }
            config.setJdbcUrl(sb.toString());
        }

        return config;
    }

    @Override
    public DataSourceConfig createInstance(long features) {
        return new DataSourceConfig();
    }
}
