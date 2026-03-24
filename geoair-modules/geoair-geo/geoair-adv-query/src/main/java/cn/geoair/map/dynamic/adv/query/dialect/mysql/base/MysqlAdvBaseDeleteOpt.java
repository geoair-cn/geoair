package cn.geoair.map.dynamic.adv.query.dialect.mysql.base;

import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvBaseDeleteOpt;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;

/**
 * MySQL删除操作实现类 仅实现MySQL专属的差异化语法，复用父类所有通用逻辑
 */
public class MysqlAdvBaseDeleteOpt extends AbstractExecAdvBaseDeleteOpt {

	public MysqlAdvBaseDeleteOpt() {
		// 绑定MySQL专属的表名处理器
		this.dialectTableNameProcessor = MysqlDialectTableNameUtil.getInstance();
	}

	// MySQL专属常量
	private static final int MYSQL_MAX_IN_PARAMS = 1000;

	// ========== 实现差异化抽象方法 ==========
	@Override
	protected int getMaxInParams() {
		return MYSQL_MAX_IN_PARAMS;
	}

}
