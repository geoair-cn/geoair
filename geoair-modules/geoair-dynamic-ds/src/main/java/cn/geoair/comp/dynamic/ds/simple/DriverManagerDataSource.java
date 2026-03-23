package cn.geoair.comp.dynamic.ds.simple;

import cn.hutool.core.lang.Assert;
import cn.hutool.db.ds.simple.AbstractDataSource;
import lombok.Data;
import lombok.Getter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author ：张逢吉
 * @date ：Created in 2022/1/7 16:21 @description： 从spring里面抄的
 */
@Data
public class DriverManagerDataSource extends AbstractDataSource {

	@Getter
	private String url;

	private String username;

	private String password;

	private String catalog;

	private String schema;

	private Properties connectionProperties;

	public DriverManagerDataSource(String url, String username, String password) {
		this.setUrl(url);
		this.setUsername(username);
		this.setPassword(password);
	}

	@Override
	public void close() throws IOException {

	}

	public Connection getConnection() throws SQLException {
		return this.getConnectionFromDriver(this.getUsername(), this.getPassword());
	}

	public Connection getConnection(String username, String password) throws SQLException {
		return this.getConnectionFromDriver(username, password);
	}

	protected Connection getConnectionFromDriver(String username, String password) throws SQLException {
		Properties mergedProps = new Properties();
		Properties connProps = this.getConnectionProperties();
		if (connProps != null) {
			mergedProps.putAll(connProps);
		}

		if (username != null) {
			mergedProps.setProperty("user", username);
		}

		if (password != null) {
			mergedProps.setProperty("password", password);
		}

		Connection con = this.getConnectionFromDriver(mergedProps);
		if (this.catalog != null) {
			con.setCatalog(this.catalog);
		}

		if (this.schema != null) {
			con.setSchema(this.schema);
		}

		return con;
	}

	protected Connection getConnectionFromDriver(Properties props) throws SQLException {
		String url = this.getUrl();
		Assert.state(url != null, "'url' not set");

		return this.getConnectionFromDriverManager(url, props);
	}

	protected Connection getConnectionFromDriverManager(String url, Properties props) throws SQLException {
		return DriverManager.getConnection(url, props);
	}

}
