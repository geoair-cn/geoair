package cn.geoair.map.dynamic.ds;


import cn.geoair.gtc.base.Gir;
import cn.geoair.map.dynamic.ds.apo.DataSourceApo;
import cn.geoair.map.dynamic.ds.utils.AdvJdbcUrlUtil;
import cn.hutool.core.util.ObjectUtil;

import com.alibaba.druid.pool.DruidDataSource;



import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据源的存储器实现类
 * 实现动态数据源的管理功能，包括添加、获取、移除和缓存清空等操作
 */

public class AdvDynamicDataSourceStorage implements DynamicDataSourceManager {


    static DynamicDataSourceManager dataSourceManager;
    IAdvDataSourceHelper iAdvDataSourceHelper;

    /**
     * 全局只有一个存储器实例
     *
     * @return
     */
    public static DynamicDataSourceManager getInstance() {
        if (dataSourceManager == null) {
            dataSourceManager = new AdvDynamicDataSourceStorage();
        }
        return dataSourceManager;
    }


    private AdvDynamicDataSourceStorage() {
        iAdvDataSourceHelper = Gir.beans.getBean(IAdvDataSourceHelper.class);
    }

    // 数据源映射
    private final Map<String, DruidDataSource> dataSourceMap = new ConcurrentHashMap<>();

    @Override
    public void cleanCache() {
        Gir.log.info("执行清空数据源缓存并释放数据库链接操作！");
        if (ObjectUtil.isNotEmpty(dataSourceMap)) {
            Set<Map.Entry<String, DruidDataSource>> entries = dataSourceMap.entrySet();
            entries.forEach(entry -> {
                // 关闭数据源，释放连接
                entry.getValue().close();
            });
            dataSourceMap.clear();
        }
    }

    @Override
    public boolean containsDataSource(String dataSourceId) {
        return dataSourceMap.containsKey(dataSourceId);
    }

    @Override
    public DruidDataSource getDataSource(String dataSourceId) {
        if (containsDataSource(dataSourceId)) {
            return dataSourceMap.get(dataSourceId);
        } else {
            throw new RuntimeException("数据源不存在: " + dataSourceId);
        }
    }

    @Override
    public void addDataSource(DruidDataSource druidDataSource, String dataSourceId) {
        // 只有当数据源不存在时才添加
        DruidDataSource existingDataSource = dataSourceMap.get(dataSourceId);
        if (existingDataSource == null) {
            dataSourceMap.put(dataSourceId, druidDataSource);
            Gir.log.debug("已添加数据源: {}", dataSourceId);
        } else {
            Gir.log.debug("数据源已存在，不执行添加操作: {}", dataSourceId);
        }
    }

    /**
     * 这里由于只有postgresql，故这里简化
     *
     * @param dataSource 数据源APO对象
     * @return 创建的Druid数据源
     */
    @Override
    public DruidDataSource getDruidDataSourceByDataSourceApo(DataSourceApo dataSource) {
        return iAdvDataSourceHelper.getDbDataSourceByApo(dataSource);

//        try {
//            // 创建新的Druid数据源
//            DruidDataSource dataSourceNew = new DruidDataSource();
//            dataSourceNew.setName("by-ds-" + dataSource.getId());
//
//            // 构建JDBC连接URL
//            String url = "jdbc:postgresql://" + dataSource.getAddress() + ":" + dataSource.getPort() + "/" + dataSource.getDbName();
//            dataSourceNew.setUrl(url);
//            dataSourceNew.setUsername(dataSource.getUsername());
//            dataSourceNew.setPassword(dataSource.getPassword());
//            dataSourceNew.setDriverClassName(DriverNamePool.DRIVER_POSTGRESQL);
//
//            // 设置连接池参数
//            dataSourceNew.setInitialSize(5);        // 初始连接数0
//            dataSourceNew.setMaxActive(300);         // 最大连接数：
//            dataSourceNew.setMinIdle(5);            // 最小空闲连接：避免闲置连接过多
//
//            dataSourceNew.setValidationQuery("SELECT 1"); // 连接校验SQL（轻量查询）
//            dataSourceNew.setTestOnBorrow(true);    // 获取连接时校验（确保拿到的连接可用）
//            dataSourceNew.setTestOnReturn(false);   // 归还连接时不校验（减少开销）
//            dataSourceNew.setTestWhileIdle(true);   // 空闲时校验（后台剔除失效连接）
//            dataSourceNew.setTimeBetweenEvictionRunsMillis(60000); // 空闲连接检测间隔：60秒
//
//            dataSourceNew.setConnectionErrorRetryAttempts(0); // 连接失败不重试
//            dataSourceNew.setRemoveAbandoned(true);           // 自动回收超时未关闭的连接
//            dataSourceNew.setRemoveAbandonedTimeout(300);      // 连接超时回收时间：5min
//            dataSourceNew.setLogAbandoned(true);              // 记录连接泄露日志
//
//            dataSourceNew.setMaxWait(3000);
//            // 初始化数据源
//            dataSourceNew.init();
//            return dataSourceNew;
//        } catch (Exception e) {
//           Gir.log.error("加载动态连接池错误", e);
//            return null;
//        }
    }

    @Override
    public DataStore getGeotoolsDataStore(DruidDataSource druidDataSource, String schema) {
        Map<String, Object> params = new HashMap<>();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, (String) PostgisNGDataStoreFactory.DBTYPE.sample);

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

    @Override
    public boolean removeDataSource(String dataSourceId) {
        if (containsDataSource(dataSourceId)) {
            DruidDataSource dataSource = dataSourceMap.remove(dataSourceId);
            // 关闭数据源，释放资源
            if (dataSource != null) {
                dataSource.close();
                Gir.log.info("已移除并关闭数据源: {}", dataSourceId);
                return true;
            }
        }
        Gir.log.debug("数据源不存在，移除失败: {}", dataSourceId);
        return false;
    }
}
