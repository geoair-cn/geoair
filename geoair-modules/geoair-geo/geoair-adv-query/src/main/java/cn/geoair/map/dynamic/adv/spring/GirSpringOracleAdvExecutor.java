package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.dialect.oracle.AdvExecutorOracle;
import cn.hutool.extra.spring.SpringUtil;
import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/11/20 09:40
 * @description：Spring环境下的高级查询执行器
 */
public class GirSpringOracleAdvExecutor extends AdvExecutorOracle implements IAdvExecutor {

    public static GirSpringOracleAdvExecutor newInstanceBySpring() {
        DataSource dataSourceBySpring = getDataSourceBySpring();
        return newInstance(dataSourceBySpring);
    }

    public static GirSpringOracleAdvExecutor newInstance(DataSource dataSource) {
        GirSpringOracleAdvExecutor advExecutor = new GirSpringOracleAdvExecutor();
        advExecutor.initByDataSource(dataSource);
        return advExecutor;
    }

    public static GirSpringOracleAdvExecutor newInstance(
            DataSource dataSource, String dataSourceName) {
        GirSpringOracleAdvExecutor advExecutor = new GirSpringOracleAdvExecutor();
        advExecutor.initByDataSource(dataSource, dataSourceName);
        return advExecutor;
    }

    public static DataSource getDataSourceBySpring() {
        return SpringUtil.getBean(DataSource.class);
    }

    public GirSpringOracleAdvExecutor() {}
}
