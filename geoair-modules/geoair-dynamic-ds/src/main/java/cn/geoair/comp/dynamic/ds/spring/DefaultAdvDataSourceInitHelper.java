package cn.geoair.comp.dynamic.ds.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceInitHelper;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 15:34 @description： spring默认的数据源获取器
 */
public class DefaultAdvDataSourceInitHelper implements IAdvDataSourceInitHelper {
    public static GiLogger log = GirLoggerFactory.getLogger();

    @Override
    public DataSource getDbDataSourceByApo(DataSourceApo dataSourceApo) {
        log.debug("正在使用Druid创建的数据源实例， 如果不想使用Druid，请自己实现IAdvDataSourceInitHelper接口");

        log.info("使用Druid创建全新的数据源实例！jdbcUrl:{}", dataSourceApo.getJdbcUrl());
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

            if (GutilObject.isNotEmpty(dataSourceApo.getRemoveAbandonedTimeout())) {
                fastCreate.setRemoveAbandonedTimeout(dataSourceApo.getRemoveAbandonedTimeout());
            }

            if (GutilObject.isNotEmpty(dataSourceApo.getConnectionErrorRetryAttempts())) {
                fastCreate.setConnectionErrorRetryAttempts(
                        dataSourceApo.getConnectionErrorRetryAttempts());
            }

            return fastCreate.toDataSource();

        } catch (Exception e) {
            log.error("加载动态连接池错误", e);
            return null;
        }
    }
}
