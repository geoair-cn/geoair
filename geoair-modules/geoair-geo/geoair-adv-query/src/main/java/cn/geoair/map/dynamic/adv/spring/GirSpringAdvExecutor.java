package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractAdvExecutor;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.hutool.extra.spring.SpringUtil;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/11/20 09:40
 * @description：Spring环境下的高级查询执行器
 */
public class GirSpringAdvExecutor extends AbstractAdvExecutor implements IAdvExecutor {

	IAdvExecutor iAdvExecutorPxy;

	public static GirSpringAdvExecutor getInstance() {
		return SpringUtil.getBean(GirSpringAdvExecutor.class);
	}

	@Override
	protected IDataSourceGetter getDataSourceGetterPxy() {
		return iAdvExecutorPxy;
	}

	@Override
	protected IAdvBaseOpt getAdvBaseOpt() {
		return iAdvExecutorPxy;
	}

	@Override
	protected IAdvDDLOpt getAdvDDLOpt() {
		return iAdvExecutorPxy;
	}

	@Override
	protected IAdvSimplePagePreOpt getSimplePageOpt() {
		return iAdvExecutorPxy;
	}

	@Override
	protected IAdvGeoPreOpt getGeoOpt() {
		return iAdvExecutorPxy;
	}

	@Override
	protected DialectTableNameProcessor getDialectTableNameProcessor() {
		return iAdvExecutorPxy;
	}

	public GirSpringAdvExecutor(IAdvExecutor iAdvExecutorPxy) {
		this.iAdvExecutorPxy = iAdvExecutorPxy;
		this.initProxyObjects();
	}

}
