package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvSimplePagePreOpt;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.hutool.core.util.StrUtil;

/**
 * PG 带参数分页实现类
 */
public class PgAdvSimplePageOpt extends AbstractExecAdvSimplePagePreOpt {

	protected static final GiLogger log = GirLogger.getLoger();

	// PG专属的依赖类（复用父类已初始化的）
	protected PgAdvGeoOpt pgAdvGeoPreOpt;

	protected PgAdvBaseOpt baseOpt;

	protected PgAdvDDLOpt pgAdvDDLOpt;

	public PgAdvSimplePageOpt(IDataSourceGetter dataSourceGetter) {
		super(dataSourceGetter);
		baseOpt = new PgAdvBaseOpt(dataSourceGetter);
		pgAdvDDLOpt = new PgAdvDDLOpt(dataSourceGetter);
		pgAdvGeoPreOpt = new PgAdvGeoOpt(dataSourceGetter);
	}

	@Override
	protected DialectTableNameProcessor getDialectTableNameProcessor() {
		return PgDialectTableNameUtil.getInstance();
	}

	@Override
	protected IAdvBaseOpt getAdvBaseOpt() {
		return baseOpt;
	}

	@Override
	protected IAdvDDLOpt getAdvDDLOpt() {
		return pgAdvDDLOpt;
	}

	@Override
	protected IAdvGeoPreOpt getAdvGeoPreOpt() {
		return pgAdvGeoPreOpt;
	}

	@Override
	protected String buildPageSql(String noPageSql, int pageSize, long offset) {
		return StrUtil.format("{} LIMIT {} OFFSET {}", noPageSql, pageSize, offset);
	}

	@Override
	protected String getTempTableAlias() {
		return "t_384_page_temp";
	}

}
