package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.comp.dynamic.ds.AdvDynamicDataSourceStorage;
import cn.geoair.comp.dynamic.ds.DynamicDataSourceManager;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceHelper;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.geoair.map.dynamic.adv.IAdvExecutorAdapter;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.tools.GirService;

import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 15:08 @description： 通用的获取器
 */
public class CommonAdvExecutorAdapter implements IAdvExecutorAdapter {

    @Override
    public IAdvExecutor getIAdvExecutor(String dataSourceId, String schema) {
        DynamicDataSourceManager instance = AdvDynamicDataSourceStorage.getInstance();
        AdvDataSourceWrapper dataSource = instance.getDataSource(dataSourceId);
        return AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, dataSourceId);
    }

    @Override
    public <T extends IAdvExecutor> T getIAdvExecutor(
            String dataSourceId, String schema, Class<T> clazz) {
        return (T) getIAdvExecutor(dataSourceId, schema);
    }
}
