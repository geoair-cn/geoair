package cn.geoair.map.dynamic.file.oracle;

import cn.geoair.map.dynamic.file.jdbc.JdbcGeoDataStoreParams;
import cn.geoair.map.dynamic.file.jdbc.JdbcGeoDialect;
import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoLinkInfo;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.data.oracle.OracleNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.SQLDialect;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Oracle Spatial（SDO_GEOMETRY）JDBC 方言。 */
public class OracleJdbcGeoDialect implements JdbcGeoDialect {

    @Override
    public String getName() {
        return "Oracle Spatial";
    }

    @Override
    public DataStore createDataStore(JdbcGeoLinkInfo linkInfo) throws Exception {
        Map<String, Object> params =
                new HashMap<>(JdbcGeoDataStoreParams.create("oracle", linkInfo));
        preserveServiceNameUrl(linkInfo, params);
        DataStore dataStore = new CaseSensitiveOracleDataStoreFactory().createDataStore(params);
        if (dataStore == null) {
            throw new IllegalStateException("无法创建 Oracle GeoTools DataStore");
        }
        return dataStore;
    }

    /**
     * GeoTools 的 Oracle 工厂将普通 database 参数按 SID 重组为 {@code host:port:database}。
     * 对 {@code @//host:port/serviceName} 形式的 JDBC 地址，需要将 database 参数加上前导斜杠，
     * 才会按 Service Name 重组为 {@code @//host:port/serviceName}。
     */
    private void preserveServiceNameUrl(JdbcGeoLinkInfo linkInfo, Map<String, Object> params) {
        String jdbcUrl = linkInfo.getJdbcUrl();
        if (jdbcUrl == null
            || !jdbcUrl.trim()
                .toLowerCase(Locale.ROOT)
                .startsWith("jdbc:oracle:thin:@//")) {
            return;
        }
        Object database = params.get("database");
        if (database == null) {
            return;
        }
        String databaseName = database.toString();
        if (!databaseName.startsWith("/")) {
            params.put("database", "/" + databaseName);
        }
    }

    /**
     * OracleNGDataStoreFactory 默认会将字段名强制转换为大写，不能读取由双引号创建的小写列，
     * 例如 {@code "road_name"} 会被错误地生成成 {@code ROAD_NAME}。这里保留 JDBC 元数据返回的原始列名并加双引号。
     */
    private static final class CaseSensitiveOracleDataStoreFactory
            extends OracleNGDataStoreFactory {

        @Override
        protected SQLDialect createSQLDialect(JDBCDataStore dataStore) {
            return new CaseSensitiveOracleDialect(dataStore);
        }
    }

    /** 支持 Oracle 双引号标识符的 SQL 方言。 */
    private static final class CaseSensitiveOracleDialect extends OracleDialect {

        private CaseSensitiveOracleDialect(JDBCDataStore dataStore) {
            super(dataStore);
        }

        @Override
        public void encodeColumnName(String prefix, String columnName, StringBuffer sql) {
            if (prefix != null && !prefix.isEmpty()) {
                sql.append(prefix).append('.');
            }
            sql.append('"')
                    .append(columnName.replace("\"", "\"\""))
                    .append('"');
        }
    }
}
