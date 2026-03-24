package cn.geoair.map.dynamic.adv.query.dialect.pg.base;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseDeleteOpt;

/**
 * PostgreSQL删除操作实现类
 */
public class PgAdvBaseDeleteOpt extends AbstractExecAdvBaseDeleteOpt {

	// PG专属常量
	private static final int PG_MAX_IN_PARAMS = 1000;

	// ========== 实现差异化抽象方法 ==========
	@Override
	protected int getMaxInParams() {
		return PG_MAX_IN_PARAMS;
	}

}
