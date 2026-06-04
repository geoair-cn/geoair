package cn.geoair.comp.dynamic.ds.spring;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceInitHelper;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
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

            DataSourceDruidFastCreate fastCreate = new DataSourceDruidFastCreate();

            // 设置基本连接信息
            fastCreate.setUrl(dataSourceApo.getJdbcUrl());
            fastCreate.setUsername(dataSourceApo.getUsername());
            fastCreate.setPassword(dataSourceApo.getPassword());


            if (GutilObject.isNotEmpty(dataSourceApo.getInitialSize())) {
                fastCreate.setInitialSize(dataSourceApo.getInitialSize());
            }
            if (GutilObject.isNotEmpty(dataSourceApo.getMaxActive())) {
                fastCreate.setMaxActive(dataSourceApo.getMaxActive());
            }
            if (GutilObject.isNotEmpty(dataSourceApo.getMinIdle())) {
                fastCreate.setMinIdle(dataSourceApo.getMinIdle());
            }
            if (GutilObject.isNotEmpty(dataSourceApo.getMaxWait())) {
                fastCreate.setMaxWait(dataSourceApo.getMaxWait());
            }
            if (GutilObject.isNotEmpty(dataSourceApo.getQueryTimeout())) {
                fastCreate.setQueryTimeout(dataSourceApo.getQueryTimeout());
            } else {
                fastCreate.setQueryTimeout(15); // 默认15秒
            }

            // 其他 Druid 特定配置（如果 DataSourceApo 中有的话）
            if (GutilObject.isNotEmpty(dataSourceApo.getRemoveAbandonedTimeout())) {
                fastCreate.setRemoveAbandonedTimeout(dataSourceApo.getRemoveAbandonedTimeout());
            }

            if (GutilObject.isNotEmpty(dataSourceApo.getConnectionErrorRetryAttempts())) {
                fastCreate.setConnectionErrorRetryAttempts(dataSourceApo.getConnectionErrorRetryAttempts());
            }

            // 创建数据源
            DruidDataSource dataSource = (DruidDataSource) fastCreate.toDataSource();
            dataSource.setBreakAfterAcquireFailure(true);
            dataSource.setValidationQuery(DataSourceApo.getValidationQuery(dataSourceApo.getDbType()));

            log.info("数据源创建成功 - url: {}, username: {}, maxActive: {}",
                    dataSourceApo.getJdbcUrl(), dataSourceApo.getUsername(), dataSource.getMaxActive());

            return dataSource;

        } catch (Exception e) {
            log.error("加载动态连接池错误", e);
            return null;
        }
    }


}
