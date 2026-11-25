package cn.geoair.comp.dynamic.ds.readwrite;

import cn.geoair.comp.dynamic.ds.readwrite.enums.SQLType;
import cn.geoair.comp.dynamic.ds.readwrite.log.RdLog;
import cn.geoair.comp.dynamic.ds.readwrite.proxy.ReadWritePxyConnection;
import cn.geoair.comp.dynamic.ds.readwrite.proxy.ReadWriteSplitConnection;
import cn.geoair.comp.dynamic.ds.readwrite.utils.SQLParserUtil;

import lombok.Getter;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * 读写分离数据源 职责：根据SQL类型自动路由
 *
 * @author 张俊
 * @date Created in 2026/5/28
 */
@Getter
public class GirReadWriteDataSource implements DataSource {

    private final DataSource masterDataSource; // 主库
    private final GirGroupSource slaveGroup; // 从库组

    public GirReadWriteDataSource(DataSource masterDataSource, GirGroupSource slaveGroup) {
        this.masterDataSource = masterDataSource;
        this.slaveGroup = slaveGroup;
        RdLog.getInstance()
                .trace("读写分离数据源初始化完成，主库: {}, 从库数量: {}", masterDataSource, slaveGroup.size());
    }

    @Override
    public Connection getConnection() throws SQLException {
        return new ReadWriteSplitConnection(masterDataSource, slaveGroup);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return new ReadWriteSplitConnection(masterDataSource, slaveGroup, username, password);
    }

    /** 根据SQL获取对应的数据源 */
    public DataSource getDataSourceBySQL(String sql) {
        SQLType sqlType = SQLParserUtil.getSQLType(sql);

        if (sqlType == SQLType.WRITE) {
            RdLog.getInstance().debug("写操作，路由到主库: {}", sql);
            return masterDataSource;
        } else if (sqlType == SQLType.READ) {
            RdLog.getInstance().debug("读操作，路由到从库组: {}", sql);
            return slaveGroup;
        } else {
            RdLog.getInstance().debug("SQL类型未知，默认路由到主库: {}", sql);
            return masterDataSource;
        }
    }

    // 代理其他方法
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return masterDataSource.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        masterDataSource.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        masterDataSource.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return masterDataSource.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return masterDataSource.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return masterDataSource.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || masterDataSource.isWrapperFor(iface);
    }

    public Connection getMasterConnection() throws SQLException {
        DataSource masterDataSource = this.getMasterDataSource();
        return new ReadWritePxyConnection(
                masterDataSource.getConnection(), false, masterDataSource);
    }

    public Connection getMasterConnection(String username, String password) throws SQLException {
        DataSource masterDataSource = this.getMasterDataSource();
        return new ReadWritePxyConnection(
                masterDataSource.getConnection(username, password), false, masterDataSource);
    }

    public Connection getSlaveConnection() throws SQLException {
        return this.getSlaveGroup().getConnection();
    }

    public Connection getSlaveConnection(String username, String password) throws SQLException {
        return this.getSlaveGroup().getConnection(username, password);
    }
}
