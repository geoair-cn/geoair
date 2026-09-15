package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.dialect.sqlite.AdvExecutorSqlite;
import cn.hutool.extra.spring.SpringUtil;

import javax.sql.DataSource;

/** Spring 环境下的 SQLite Core 执行器。 */
public class GirSpringSqliteAdvExecutor extends AdvExecutorSqlite implements IAdvExecutor {

    public static GirSpringSqliteAdvExecutor newInstanceBySpring() {
        return newInstance(SpringUtil.getBean(DataSource.class));
    }

    public static GirSpringSqliteAdvExecutor newInstance(DataSource dataSource) {
        GirSpringSqliteAdvExecutor executor = new GirSpringSqliteAdvExecutor();
        executor.initByDataSource(dataSource);
        return executor;
    }

    public static GirSpringSqliteAdvExecutor newInstance(
            DataSource dataSource, String dataSourceName) {
        GirSpringSqliteAdvExecutor executor = new GirSpringSqliteAdvExecutor();
        executor.initByDataSource(dataSource, dataSourceName);
        return executor;
    }

    public GirSpringSqliteAdvExecutor() {}
}
