package cn.geoair.comp.dynamic.ds.simple;

import cn.hutool.core.io.IoUtil;
import cn.hutool.db.ds.simple.AbstractDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/26 16:19 @description： 简单包装一个数据源对象
 */
public class AdvSimpleDataSource extends AbstractDataSource {

	Connection connection;

	public AdvSimpleDataSource(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void close() throws IOException {
		IoUtil.close(connection);
	}

	@Override
	public Connection getConnection() throws SQLException {
		if (connection.isClosed()) {
			throw new SQLException("Connection is closed!");
		}
		return connection;
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return connection;
	}

}
