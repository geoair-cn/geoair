package cn.geoair.map.dynamic.file.oracle;

import cn.geoair.map.dynamic.file.jdbc.JdbcGeoDataStoreParams;
import cn.geoair.map.dynamic.file.jdbc.JdbcGeoDialect;
import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoLinkInfo;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.data.oracle.OracleNGDataStoreFactory;

import java.util.Map;

/** Oracle Spatial（SDO_GEOMETRY）JDBC 方言。 */
public class OracleJdbcGeoDialect implements JdbcGeoDialect {

    @Override
    public String getName() {
        return "Oracle Spatial";
    }

    @Override
    public DataStore createDataStore(JdbcGeoLinkInfo linkInfo) throws Exception {
        Map<String, Object> params = JdbcGeoDataStoreParams.create("oracle", linkInfo);
        params.put(OracleNGDataStoreFactory.LOOSEBBOX.key, true);
        DataStore dataStore = DataStoreFinder.getDataStore(params);
        if (dataStore == null) {
            throw new IllegalStateException("无法创建 Oracle Spatial GeoTools DataStore");
        }
        return dataStore;
    }
}
