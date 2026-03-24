package cn.geoair.map.dynamic.adv.spring;

import javax.sql.DataSource;

import cn.geoair.comp.dynamic.ds.IAdvDataSourceHelper;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.map.dynamic.adv.IAdvExecutorAdapter;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.tools.GirService;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 15:08 @description： 通用的获取器
 */
public class CommonAdvExecutorAdapter implements IAdvExecutorAdapter {

    @Override
    public IAdvExecutor getIAdvExecutor(String dataSourceId, String schema) {
        IAdvDataSourceHelper pxyBeanC = GirService.getPxyBeanC(IAdvDataSourceHelper.class);
        if (pxyBeanC == null) {
            throw new RuntimeException("无法找到AdvDataSourceHelper的实现");
        }
        DataSourceApo dataSourceApoById = pxyBeanC.getDataSourceApoById(dataSourceId);
        dataSourceApoById.setSchemaName(schema);
        // 这里进行区分数据库执行器
         DataSource dbDataSourceByApo = pxyBeanC.getDbDataSourceByApo(dataSourceApoById);
        return AdvExecutorFactory.getAdvExecutorByDataSource(dbDataSourceByApo, dataSourceId);
    }

    @Override
    public <T extends IAdvExecutor> T getIAdvExecutor(String dataSourceId, String schema, Class<T> clazz) {
        return (T) getIAdvExecutor(dataSourceId, schema);
    }

}
