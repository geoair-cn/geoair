package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.dialect.sqlserver.AdvExecutorSqlServer;
import cn.hutool.extra.spring.SpringUtil;
import javax.sql.DataSource;

/** Spring 环境下的 SQL Server 高级查询执行器。@author 张逢吉 */
public class GirSpringSqlServerAdvExecutor extends AdvExecutorSqlServer {
    public static GirSpringSqlServerAdvExecutor newInstanceBySpring() { return newInstance(SpringUtil.getBean(DataSource.class)); }
    public static GirSpringSqlServerAdvExecutor newInstance(DataSource source) { return newInstance(source, null); }
    public static GirSpringSqlServerAdvExecutor newInstance(DataSource source, String sourceName) {
        GirSpringSqlServerAdvExecutor executor = new GirSpringSqlServerAdvExecutor();
        executor.initByDataSource(source, sourceName);
        return executor;
    }
}
