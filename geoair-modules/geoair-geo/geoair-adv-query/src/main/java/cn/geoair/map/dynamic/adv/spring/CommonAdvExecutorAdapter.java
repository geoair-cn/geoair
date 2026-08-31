package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.AdvDynamicDataSourceStorage;
import cn.geoair.comp.dynamic.ds.DynamicDataSourceManager;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.geoair.map.dynamic.adv.IAdvExecutorAdapter;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import javax.sql.DataSource;

/**
 * {@link IAdvExecutorAdapter} 的默认实现，基于动态数据源存储查找 DataSource。
 *
 * <p>实现逻辑：
 *
 * <ol>
 *   <li>通过 {@link AdvDynamicDataSourceStorage} 和 {@link DynamicDataSourceManager} 根据 dataSourceId
 *       获取或创建对应的 {@link AdvDataSourceWrapper}
 *   <li>调用 {@link AdvExecutorFactory#getAdvExecutorByDataSource(DataSource, String)}
 *       自动检测数据库方言并创建匹配的 Executor
 *   <li>如果 schema 不为空，设置 Executor 的 Schema 名称获取函数
 * </ol>
 *
 * <p>此实现通过 {@link cn.geoair.base.Gir} SPI 机制暴露为服务， 调用方可通过 {@code
 * GirService.getPxyBeanC(IAdvExecutorAdapter.class)} 获取。
 *
 * @author 张逢吉
 * @date 2025/10/9 15:08
 */
public class CommonAdvExecutorAdapter implements IAdvExecutorAdapter {

    @Override
    public IAdvExecutor getIAdvExecutor(String dataSourceId, String schema) {
        DynamicDataSourceManager instance = AdvDynamicDataSourceStorage.getInstance();
        AdvDataSourceWrapper dataSource = instance.getOrCreateDataSource(dataSourceId);
        String dataSourceName = dataSourceId + "_" + schema;
        IAdvExecutor advExecutorByDataSource =
                AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, dataSourceName);
        if (GutilObject.isNotEmpty(schema)) {
            advExecutorByDataSource.setSchemaNameGetterFunction(() -> schema);
        }
        return advExecutorByDataSource;
    }

    @Override
    public <T extends IAdvExecutor> T getIAdvExecutor(
            String dataSourceId, String schema, Class<T> clazz) {
        return (T) getIAdvExecutor(dataSourceId, schema);
    }
}
