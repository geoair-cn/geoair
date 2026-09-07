package cn.geoair.map.dynamic.file.postgis;

import cn.geoair.map.dynamic.file.jdbc.JdbcGeoDataStoreParams;
import cn.geoair.map.dynamic.file.jdbc.JdbcGeoDialect;
import cn.geoair.map.dynamic.file.jdbc.link.JdbcGeoLinkInfo;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;

import java.util.Map;

/** PostGIS 的 JDBC 空间方言。 */
public class PostgisJdbcGeoDialect implements JdbcGeoDialect {

    @Override
    public String getName() {
        return "PostGIS";
    }

    @Override
    public DataStore createDataStore(JdbcGeoLinkInfo linkInfo) throws Exception {
        Map<String, Object> params = JdbcGeoDataStoreParams.create("postgis", linkInfo);
        params.put(PostgisNGDataStoreFactory.PREPARED_STATEMENTS.key, true);
        DataStore dataStore = DataStoreFinder.getDataStore(params);
        if (dataStore == null) {
            throw new IllegalStateException("无法创建 PostGIS GeoTools DataStore");
        }
        return dataStore;
    }
}
