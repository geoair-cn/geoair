package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.comp.dynamic.ds.IDsDataSourceManger;
import cn.geoair.comp.dynamic.ds.tx.IDsTxTemplate;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvExecutor;
import cn.hutool.extra.spring.SpringUtil;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/11/20 09:40
 * @description：Spring环境下的高级查询执行器
 */
public class GirSpringAdvExecutor extends AbstractPxyAdvExecutor implements IAdvExecutor {

    IAdvExecutor iAdvExecutorPxy;

    public static GirSpringAdvExecutor getInstance() {
        return SpringUtil.getBean(GirSpringAdvExecutor.class);
    }

    @Override
    protected IDsDataSourceManger getDataSourceGetter() {
        return iAdvExecutorPxy;
    }

    @Override
    protected IDsTxTemplate getAdvTxTemplate() {
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
    protected IAdvSimplePageOpt getSimplePageOpt() {
        return iAdvExecutorPxy;
    }

    @Override
    protected IAdvGeoPreOpt getGeoOpt() {
        return iAdvExecutorPxy;
    }

    @Override
    public IAdvWhereSelectOpt getWhereSelectOpt() {
        return iAdvExecutorPxy;
    }

    @Override
    public AdvQueryGlobalConfig getConfig() {
        return getAdvBaseOpt().getConfig();
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
