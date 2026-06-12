package cn.geoair.comp.dynamic.ds.dswrapper.wrapper;

import cn.geoair.base.Gir;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;

/** HikariCP数据源包装器 */
@Slf4j
public class HikariDataSourceWrapper extends GirAbstractDataSourceWrapper {

    public HikariDataSourceWrapper(DataSource targetDataSource) {
        super(targetDataSource);
    }

    static Boolean canInit = null;

    public static boolean canInit() {
        if (canInit != null) {
            return canInit;
        }
        try {
            Class.forName("com.zaxxer.hikari.HikariDataSource");
            canInit = true;
        } catch (ClassNotFoundException e) {
            return false;
        }
        return canInit;
    }

    @Override
    public boolean close() {
        HikariDataSource dataSource = (HikariDataSource) targetDataSource;
        try {
            dataSource.close();
        } catch (Exception e) {
            Gir.log.error(e);
        }
        return true;
    }

    @Override
    protected Class<? extends DataSource> getTargetDataSourceClass() {
        return HikariDataSource.class;
    }

    @Override
    public String getSimpleDataSourceName() {
        HikariDataSource dataSource = (HikariDataSource) targetDataSource;
        return dataSource.getJdbcUrl();
    }

    @Override
    public String getJdbcUrl() {
        HikariDataSource dataSource = (HikariDataSource) targetDataSource;
        return dataSource.getJdbcUrl();
    }

    @Override
    public Integer getActiveCount() {
        HikariDataSource hikari = getHikariDataSource();
        if (hikari == null) {
            return null;
        }
        try {
            // HikariCP: 通过 MXBean 获取
            return hikari.getHikariPoolMXBean().getActiveConnections();
        } catch (Exception e) {
            log.error("获取HikariCP活跃连接数失败", e);
            return null;
        }
    }

    public HikariDataSource getHikariDataSource() {
        if (isSupport()) {
            return (HikariDataSource) super.targetDataSource;
        }
        throw new IllegalArgumentException("当前数据源不是Hikari数据源");
    }
}
