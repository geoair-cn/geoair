package cn.geoair.comp.dynamic.ds.datasource.wrapper;

import javax.sql.DataSource;

import cn.geoair.base.Gir;
import cn.geoair.comp.dynamic.ds.simple.DriverManagerDataSource;

public class DiverManagerSourceWrapper extends AbstractDataSourceWrapper {

	public DiverManagerSourceWrapper(DataSource targetDataSource) {
		super(targetDataSource);
	}

	public static boolean canInit() {
		return true;
	}

	@Override
	public boolean close() {
		DriverManagerDataSource dataSource = (DriverManagerDataSource) targetDataSource;
		try {
			dataSource.close();
		}
		catch (Exception e) {
			Gir.log.error(e);
		}
		return true;
	}

	@Override
	protected Class<? extends DataSource> getTargetDataSourceClass() {
		return DriverManagerDataSource.class;
	}

	@Override
	public String getSimpleDataSourceName() {
		DriverManagerDataSource dataSource = (DriverManagerDataSource) targetDataSource;
		return dataSource.getUrl();
	}

	@Override
	public String getJdbcUrl() {
		DriverManagerDataSource dataSource = (DriverManagerDataSource) targetDataSource;
		return dataSource.getUrl();
	}

}
