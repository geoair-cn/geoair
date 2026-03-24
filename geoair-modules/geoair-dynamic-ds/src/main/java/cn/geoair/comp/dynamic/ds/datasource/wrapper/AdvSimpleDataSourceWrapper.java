package cn.geoair.comp.dynamic.ds.datasource.wrapper;

import javax.sql.DataSource;

import cn.geoair.comp.dynamic.ds.simple.AdvSimpleDataSource;

/**
 * HikariCP数据源包装器
 */
public class AdvSimpleDataSourceWrapper extends AbstractDataSourceWrapper {

	public AdvSimpleDataSourceWrapper(DataSource targetDataSource) {
		super(targetDataSource);
	}

	public static boolean canInit() {

		return true;
	}

	@Override
	public boolean close() {
		return true;
	}

	protected Class<? extends DataSource> getTargetDataSourceClass() {
		return AdvSimpleDataSource.class;
	}

	@Override
	public String getSimpleDataSourceName() {

		return null;
	}

	@Override
	public String getJdbcUrl() {

		return null;
	}

}
