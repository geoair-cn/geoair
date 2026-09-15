package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.db.dialect.DialectName;

import javax.sql.DataSource;

/** SQLite Core 原生方言提供者；不包含 SpatiaLite。 */
public class SqliteAdvDialectProvider extends AbstractAdvDialectProvider {

    public SqliteAdvDialectProvider() {
        super("sqlite", DialectName.SQLITE3, "SQLITE");
    }

    @Override
    public IAdvExecutor create(DataSource dataSource, String dataSourceName) {
        return GirSpringSqliteAdvExecutor.newInstance(dataSource, dataSourceName);
    }
}
