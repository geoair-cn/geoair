package cn.geoair.comp.dynamic.ds.datasource;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;


import java.sql.SQLException;

/**
 * @author ：张俊
 * @date ：Created in 2025/1/2 18:31 @description： 数据源包装
 */
public class WrapDruidDataSource extends DruidDataSource {

    protected final GiLogger log = GirLogger.getLoger(WrapDruidDataSource.class);

    String groupName;

    public WrapDruidDataSource(String groupName) {
        this.groupName = groupName;
    }


    public String getUrl() {
//        GtcDataSourceHelper gtcDataSourceHelper = GtcDatasource.getGtcDataSourceHelper();
//        DruidDataSource dataSourceByGroupRandom =
//                (DruidDataSource) gtcDataSourceHelper.getDataSourceByGroupRandom(groupName);
//        return dataSourceByGroupRandom.getUrl();
        return null;
    }

    @Override
    public DruidPooledConnection getConnection() throws SQLException {
//        GtcDataSourceHelper gtcDataSourceHelper = GtcDatasource.getGtcDataSourceHelper();
//        DruidDataSource dataSourceByGroupRandom =
//                (DruidDataSource) gtcDataSourceHelper.getDataSourceByGroupRandom(groupName);
        // Map<String, Object> statData = dataSourceByGroupRandom.getStatData();
//        String name = dataSourceByGroupRandom.getName();
        // log.info("随机数据源名称;{}", name);
        // log.info("ActiveCount:{}", statData.get("ActiveCount"));
        // log.info("ActivePeak:{}", statData.get("ActivePeak"));
        // log.info("ActivePeakTime:{}", statData.get("ActivePeakTime"));
        // log.info("InitialSize:{}", statData.get("InitialSize"));
        // log.info("MinIdle:{}", statData.get("MinIdle"));
        // log.info("MaxActive:{}", statData.get("MaxActive"));
//        return (DruidPooledConnection) dataSourceByGroupRandom.getConnection();
        return null;
    }
}
