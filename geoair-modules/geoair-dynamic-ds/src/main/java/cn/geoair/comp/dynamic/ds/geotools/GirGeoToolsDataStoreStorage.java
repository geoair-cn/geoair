package cn.geoair.comp.dynamic.ds.geotools;

import cn.geoair.base.Gir;
import cn.geoair.comp.dynamic.ds.utils.AdvJdbcUrlUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.druid.pool.DruidDataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;

public class GirGeoToolsDataStoreStorage implements GtDataStoreGetter {

    static GirGeoToolsDataStoreStorage girGeoToolsDataStoreStorage;

    public static GirGeoToolsDataStoreStorage getInstance() {
        if (girGeoToolsDataStoreStorage == null) {
            girGeoToolsDataStoreStorage = new GirGeoToolsDataStoreStorage();
        }
        return girGeoToolsDataStoreStorage;
    }

    @Override
    public DataStore getGeotoolsDataStore(DruidDataSource druidDataSource, String schema) {
        Map<String, Object> params = new HashMap<>();
        params.put(
                PostgisNGDataStoreFactory.DBTYPE.key,
                (String) PostgisNGDataStoreFactory.DBTYPE.sample);

        if (ObjectUtil.isNotEmpty(schema)) {
            params.put(PostgisNGDataStoreFactory.SCHEMA.key, schema);
        }

        // 解析JDBC URL获取主机和端口信息
        String rawJdbcUrl = druidDataSource.getRawJdbcUrl();
        AdvJdbcUrlUtil jdbcUrlSplitter = new AdvJdbcUrlUtil(rawJdbcUrl);
        params.put(PostgisNGDataStoreFactory.HOST.key, jdbcUrlSplitter.host);
        params.put(PostgisNGDataStoreFactory.PORT.key, jdbcUrlSplitter.port);
        params.put(PostgisNGDataStoreFactory.USER.key, druidDataSource.getUsername());
        params.put(PostgisNGDataStoreFactory.DATASOURCE.key, druidDataSource);

        try {
            return DataStoreFinder.getDataStore(params);
        } catch (IOException e) {
            Gir.log.error("初始化pg连接失败：{}", e.getMessage(), e);
            return null;
        }
    }
}
