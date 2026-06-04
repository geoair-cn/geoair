package cn.geoair.comp.dynamic.ds;

import cn.hutool.core.io.IoUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/4 12:12
 * @description： 链接管理
 */
public class RealConnectionOpt implements IDsConnectionOpt {

    IDsDataSourceOpt dataSourceManger;

    public RealConnectionOpt(IDsDataSourceOpt sourceManger) {
        this.dataSourceManger = sourceManger;
    }

    @Override
    public Connection getConnection() {
        try {
            return dataSourceManger.getDataSource().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void connectionClose(Connection connection) {
        IoUtil.close(connection);
    }

    @Override
    public void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        IoUtil.close(rs);
        IoUtil.close(stmt);
        IoUtil.close(conn);
    }
}
