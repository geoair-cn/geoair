package cn.geoair.comp.db.service.core.basic.apo;

import java.io.Serializable;
import java.util.function.Consumer;

import cn.geoair.comp.db.service.core.basic.util.DESUtils;
import cn.geoair.comp.dynamic.ds.utils.DataSourceDruidFastCreate;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.Data;

import javax.sql.DataSource;

/**
 * @program: dbApi
 * @description:
 * @author: 武汉刘德华
 * @create: 2021-01-20 09:52
 */
@Data
public class DsDataSourceApo implements Serializable {

    String id;

    String name;

    String note;

    String url;

    String username;

    String password;

    /**
     * true 修改密码 false不修改
     */
    boolean edit_password;

    String type;

    String driver;

    String tableSql;

    String createUserId;

    String createUserName;

    String createTime;

    String updateTime;


    public static DataSource toDataSource(DsDataSourceApo ds) {
        DataSourceDruidFastCreate druidFastCreate = new DataSourceDruidFastCreate();
        druidFastCreate.setUrl(ds.getUrl());
        druidFastCreate.setUsername(ds.getUsername());
        try {
            druidFastCreate.setPassword(DESUtils.decrypt(ds.getPassword()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        druidFastCreate.setConfigurator(new Consumer<DruidDataSource>() {
            @Override
            public void accept(DruidDataSource druidDataSource) {
                druidDataSource.setName(ds.getName());

                druidDataSource.setRemoveAbandoned(true);
                druidDataSource.setRemoveAbandonedTimeout(300); // 5分钟自动回收

                druidDataSource.setDriverClassName(ds.getDriver());
                druidDataSource.setConnectionErrorRetryAttempts(3); // 失败后重连次数
                druidDataSource.setBreakAfterAcquireFailure(true);

            }
        });
        return druidFastCreate.toDataSource();

    }

}
