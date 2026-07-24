package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.dialect.dm.AdvExecutorDm;
import cn.hutool.extra.spring.SpringUtil;
import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： Spring环境下的达梦高级查询执行器
 */
public class GirSpringDmAdvExecutor extends AdvExecutorDm implements IAdvExecutor {

    public static GirSpringDmAdvExecutor newInstanceBySpring() {
        DataSource dataSourceBySpring = getDataSourceBySpring();
        return newInstance(dataSourceBySpring);
    }

    public static GirSpringDmAdvExecutor newInstance(DataSource dataSource) {
        GirSpringDmAdvExecutor advExecutor = new GirSpringDmAdvExecutor();
        advExecutor.initByDataSource(dataSource);
        return advExecutor;
    }

    public static GirSpringDmAdvExecutor newInstance(DataSource dataSource, String dataSourceName) {
        GirSpringDmAdvExecutor advExecutor = new GirSpringDmAdvExecutor();
        advExecutor.initByDataSource(dataSource, dataSourceName);
        return advExecutor;
    }

    public static DataSource getDataSourceBySpring() {
        return SpringUtil.getBean(DataSource.class);
    }

    public GirSpringDmAdvExecutor() {}
}
