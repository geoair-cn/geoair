package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractAdvSimplePagePreOpt;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;
import cn.hutool.core.util.StrUtil;

/**
 * MySQL 带参数分页实现类
 */
public class MysqlAdvSimplePageOpt extends AbstractAdvSimplePagePreOpt {

	// MySQL专属依赖
	protected MysqlAdvGeoOpt mysqlAdvGeoPreOpt;

	protected MysqlAdvBaseOpt baseOpt;

	protected MysqlAdvDDLOpt mysqlAdvDDLOpt;

	public MysqlAdvSimplePageOpt(IDataSourceGetter dataSourceGetter) {
		super(dataSourceGetter);
		baseOpt = new MysqlAdvBaseOpt(dataSourceGetter);
		mysqlAdvDDLOpt = new MysqlAdvDDLOpt(dataSourceGetter);
		mysqlAdvGeoPreOpt = new MysqlAdvGeoOpt(dataSourceGetter);
	}

	@Override
	protected DialectTableNameProcessor getDialectTableNameProcessor() {
		return MysqlDialectTableNameUtil.getInstance();
	}

	@Override
	protected IAdvBaseOpt getAdvBaseOpt() {
		return baseOpt;
	}

	@Override
	protected IAdvDDLOpt getAdvDDLOpt() {
		return mysqlAdvDDLOpt;
	}

	@Override
	protected IAdvGeoPreOpt getAdvGeoPreOpt() {
		return mysqlAdvGeoPreOpt;
	}

	@Override
	protected String buildPageSql(String noPageSql, int pageSize, long offset) {
		return StrUtil.format("{} LIMIT {}, {}", noPageSql, offset, pageSize);
	}

	@Override
	protected String getTempTableAlias() {
		return "t_mysql_page_temp";
	}

}
