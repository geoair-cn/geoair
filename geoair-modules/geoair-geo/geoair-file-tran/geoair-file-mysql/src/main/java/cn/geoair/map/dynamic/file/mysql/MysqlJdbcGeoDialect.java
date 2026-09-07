package cn.geoair.map.dynamic.file.mysql;

import cn.geoair.map.dynamic.file.jdbc.JdbcGeoDataStoreParams;
import cn.geoair.map.dynamic.file.jdbc.JdbcGeoDialect;
import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoLinkInfo;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.data.mysql.MySQLDataStoreFactory;

import java.util.Map;

/** MySQL 8 Spatial JDBC 方言，不兼容 MySQL 5.7。 */
public class MysqlJdbcGeoDialect implements JdbcGeoDialect {

    @Override
    public String getName() {
        return "MySQL 8 Spatial";
    }

    @Override
    public DataStore createDataStore(JdbcGeoLinkInfo linkInfo) throws Exception {
        Map<String, Object> params = JdbcGeoDataStoreParams.create("mysql", linkInfo);
        params.put(MySQLDataStoreFactory.ENHANCED_SPATIAL_SUPPORT.key, true);
        params.put(MySQLDataStoreFactory.STORAGE_ENGINE.key, "InnoDB");
        DataStore dataStore = DataStoreFinder.getDataStore(params);
        if (dataStore == null) {
            throw new IllegalStateException("无法创建 MySQL 8 Spatial GeoTools DataStore");
        }
        return dataStore;
    }
}
