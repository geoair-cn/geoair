package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.db.dialect.DialectName;
import javax.sql.DataSource;

/** Microsoft SQL Server 原生方言提供者。@author 张逢吉 */
public class SqlServerAdvDialectProvider extends AbstractAdvDialectProvider {
    public SqlServerAdvDialectProvider() { super("sqlserver", DialectName.SQLSERVER, "MICROSOFT SQL SERVER", "SQL SERVER"); }
    @Override public IAdvExecutor create(DataSource dataSource, String dataSourceName) { return GirSpringSqlServerAdvExecutor.newInstance(dataSource, dataSourceName); }
}
