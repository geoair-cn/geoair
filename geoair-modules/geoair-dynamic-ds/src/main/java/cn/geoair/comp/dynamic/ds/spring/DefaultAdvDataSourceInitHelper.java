package cn.geoair.comp.dynamic.ds.spring;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceInitHelper;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 15:34 @description： spring默认的数据源获取器
 */
@Slf4j
public class DefaultAdvDataSourceInitHelper implements IAdvDataSourceInitHelper {


    @Override
    public DataSource getDbDataSourceByApo(DataSourceApo dataSourceApo) {
        log.info("创建全新的数据源实例！jdbcUrl:{}", dataSourceApo.getJdbcUrl());
        try {
            // 创建新的Druid数据源
            DruidDataSource dataSourceNew = new DruidDataSource();
            // 设置连接池参数
            dataSourceNew.setInitialSize(dataSourceApo.getInitialSize());

            dataSourceNew.setMaxActive(dataSourceApo.getMaxActive());

            dataSourceNew.setMinIdle(dataSourceApo.getMinIdle());

            dataSourceNew.setMaxWait(dataSourceApo.getMaxWait());

            dataSourceNew.setRemoveAbandonedTimeout(dataSourceApo.getRemoveAbandonedTimeout());
            if (GutilObject.isNotEmpty(dataSourceApo.getQueryTimeout())) {
                dataSourceNew.setQueryTimeout(dataSourceApo.getQueryTimeout());
            } else {
                dataSourceNew.setQueryTimeout(15); // 查询的超时时间，15秒钟
            }
            // 连接可用性校验（强化：避免拿到失效连接）
            dataSourceNew.setValidationQuery(
                    DataSourceApo.getValidationQuery(dataSourceApo.getDbType())); // 连接校验SQL（轻量查询，保留）
            dataSourceNew.setTestOnBorrow(false); //  获取连接时不校验
            //  TestOnBorrow=true会每次获取连接都校验，高并发下性能损耗大；改为空闲时校验更高效
            dataSourceNew.setTestOnReturn(false); //  归还连接时不校验
            dataSourceNew.setTestWhileIdle(true); //  空闲时校验
            dataSourceNew.setTimeBetweenEvictionRunsMillis(30000); // 空闲连接检测间隔
            dataSourceNew.setNumTestsPerEvictionRun(-1); // 新增：每次检测所有空闲连接（-1表示不限制）
            dataSourceNew.setMinEvictableIdleTimeMillis(1800000); // 新增：连接最小空闲时间（30分钟）
            dataSourceNew.setConnectionErrorRetryAttempts(dataSourceApo.getConnectionErrorRetryAttempts());
            dataSourceNew.setBreakAfterAcquireFailure(true);
            // 连接泄露与日志（强化监控）
            dataSourceNew.setRemoveAbandoned(true); // 保留：自动回收超时未关闭的连接
            dataSourceNew.setLogAbandoned(true); // 保留：记录连接泄露日志

            // // 构建JDBC连接URL
            String url = dataSourceApo.getJdbcUrl();
            dataSourceNew.setUrl(url);
            dataSourceNew.setUsername(dataSourceApo.getUsername());
            dataSourceNew.setPassword(dataSourceApo.getPassword());
            // 初始化数据源
//            dataSourceNew.init();
            return dataSourceNew;
        } catch (Exception e) {
            log.error("加载动态连接池错误", e);
            return null;
        }
    }


}
