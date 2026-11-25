package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.dialect.pg.AdvExecutorPG;
import cn.hutool.extra.spring.SpringUtil;

import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/11/20 09:40
 * @description：Spring环境下的高级查询执行器
 */
public class GirSpringPGAdvExecutor extends AdvExecutorPG implements IAdvExecutor {

    public static GirSpringPGAdvExecutor newInstanceBySpring() {
        DataSource dataSourceBySpring = getDataSourceBySpring();
        return newInstance(dataSourceBySpring);
    }

    public static GirSpringPGAdvExecutor newInstance(DataSource dataSource) {
        return newInstance(dataSource, null);
    }

    public static GirSpringPGAdvExecutor newInstance(DataSource dataSource, String dataSourceName) {
        GirSpringPGAdvExecutor advExecutor = new GirSpringPGAdvExecutor();
        advExecutor.initByDataSource(dataSource, dataSourceName);
        return advExecutor;
    }

    public static DataSource getDataSourceBySpring() {
        return SpringUtil.getBean(DataSource.class);
    }

    public GirSpringPGAdvExecutor() {}
}
