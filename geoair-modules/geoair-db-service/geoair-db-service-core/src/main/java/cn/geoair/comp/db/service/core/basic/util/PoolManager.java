package cn.geoair.comp.db.service.core.basic.util;

import cn.geoair.comp.db.service.core.basic.apo.DsDataSourceApo;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import com.alibaba.druid.pool.DruidDataSource;

import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

/**
 * @program: api
 * @description:
 * @author: 武汉刘德华
 * @create: 2020-12-11 10:51
 */
@Slf4j
public class PoolManager {

    // 所有数据源的连接池存在map里
    static ConcurrentHashMap<String, DruidDataSource> map = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, IAdvExecutor> advExecutorConcurrentHashMap = new ConcurrentHashMap<>();


    // 删除数据库连接池
    public static void removeExecutor(String id) {
        if (advExecutorConcurrentHashMap.containsKey(id)) {
            IAdvExecutor iAdvExecutor = advExecutorConcurrentHashMap.get(id);
            DataSource dataSource = iAdvExecutor.getDataSource();
            if (dataSource instanceof DruidDataSource) {
                ((DruidDataSource) dataSource).close();
            }
            advExecutorConcurrentHashMap.remove(id);
        }
    }


    public static IAdvExecutor getIAdvExecutor(DsDataSourceApo ds) {
        if (advExecutorConcurrentHashMap.containsKey(ds.getId())) {
            return advExecutorConcurrentHashMap.get(ds.getId());
        } else {
            DataSource dataSource = DsDataSourceApo.toDataSource(ds);
            IAdvExecutor advExecutorByDataSource = AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, "by_ds_api" + ds.getId());
            advExecutorConcurrentHashMap.put(ds.getId(), advExecutorByDataSource);
        }
        return advExecutorConcurrentHashMap.get(ds.getId());
    }

}
