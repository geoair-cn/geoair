package cn.geoair.comp.dynamic.ds;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.simple.AdvSimpleDataSource;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/10/9 10:38 @description： 数据源获取器
 */
public class DataSourceGetter implements IDataSourceGetter {

	private static final GiLogger log = GirLogger.getLoger();

	private DataSource dataSource = null;

//	protected DataStore dataStore = null;

	protected String schemaName = null;

	protected String dataSourceId = null;

	@Override
	public String getSchemaName() {
		return schemaName;
	}

	@Override
	public String getDataSourceId() {
		return dataSourceId;
	}

	protected DataSourceApo dataSourceApo = null;

	@Override
	public void initByDataSourceApo(DataSourceApo dataSourceApo) {
		this.dataSourceApo = dataSourceApo;
		this.dataSourceId = dataSourceApo.getId();
		schemaName = dataSourceApo.getSchemaName();
		if (AdvDynamicDataSourceStorage.getInstance().containsDataSource(dataSourceId)) {
			dataSource = AdvDynamicDataSourceStorage.getInstance().getDataSource(dataSourceId);
		}
		else {
			dataSource = AdvDynamicDataSourceStorage.getInstance().getDruidDataSourceByDataSourceApo(dataSourceApo);
		}
	}

	@Override
	public void initByDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.dataSourceId = "";
		this.schemaName = "";
		this.dataSourceApo = null;
	}

	@Override
	public void initByConnection(Connection connection) {
		AdvSimpleDataSource simpleDataSource = new AdvSimpleDataSource(connection);
		initByDataSource(simpleDataSource);
	}

	@Override
	public Connection getConnection() {
		try {
			return dataSource.getConnection();
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

//	@Override
//	public DataStore getGeoToolsDataStore() {
//		return dataStore;
//	}

	@Override
	public void connectionClose(Connection connection) {
		IoUtil.close(connection);
	}

	@Override
	public DataSourceApo getDataSourceApo() {
		if (dataSourceApo == null) {
			return null;
		}
		DataSourceApo apo = new DataSourceApo();
		BeanUtil.copyProperties(dataSourceApo, apo);
		return apo;
	}

	/**
	 * 关闭数据库资源
	 */
	@Override
	public void closeResources(ResultSet rs, Statement stmt, Connection conn) {
		IoUtil.close(rs);
		IoUtil.close(stmt);
		IoUtil.close(conn);
	}

}
