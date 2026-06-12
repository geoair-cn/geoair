package cn.geoair.comp.db.service.core.basic.util;

import cn.geoair.comp.db.service.core.basic.apo.DsDataSourceApo;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;

import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @program: api
 * @description:
 * @author: 武汉刘德华
 * @create: 2020-12-11 10:51
 */
@Slf4j
public class PoolManager {

    // 所有数据源的连接池存在map里
    static ConcurrentHashMap<String, DruidDataSource> map = new ConcurrentHashMap<>();

    public static DruidDataSource getJdbcConnectionPool(DsDataSourceApo ds) {
        if (map.containsKey(ds.getId())) {
            return map.get(ds.getId());
        } else {
            DruidDataSource druidDataSource = new DruidDataSource();
            druidDataSource.setName(ds.getName());
            druidDataSource.setUrl(ds.getUrl());
            druidDataSource.setUsername(ds.getUsername());
            druidDataSource.setRemoveAbandoned(true);
            druidDataSource.setRemoveAbandonedTimeout(300); // 5分钟自动回收
            // druidDataSource.setPassword(ds.getPassword());
            druidDataSource.setDriverClassName(ds.getDriver());
            druidDataSource.setConnectionErrorRetryAttempts(3); // 失败后重连次数
            druidDataSource.setBreakAfterAcquireFailure(true);

            try {
                druidDataSource.setPassword(DESUtils.decrypt(ds.getPassword()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            map.put(ds.getId(), druidDataSource);
            log.info("create druid datasource：{}", ds.getName());
            return map.get(ds.getId());
        }
    }

    // 删除数据库连接池
    public static void removeJdbcConnectionPool(String id) {
        if (map.containsKey(id)) {
            DruidDataSource old = map.get(id);
            map.remove(id);
            old.close();
            log.info("remove druid datasource: {}", old.getName());
        }
    }

    public static DruidPooledConnection getPooledConnection(DsDataSourceApo ds)
            throws SQLException {
        DruidDataSource pool = PoolManager.getJdbcConnectionPool(ds);
        DruidPooledConnection connection = pool.getConnection();
        log.debug("获取连接成功");
        return connection;
    }
}
