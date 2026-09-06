package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.db.dialect.DialectName;

import javax.sql.DataSource;

/** PostgreSQL/PostGIS 原生方言提供者。 */
public class PostgresqlAdvDialectProvider extends AbstractAdvDialectProvider {

    public PostgresqlAdvDialectProvider() {
        this("postgresql", "POSTGRESQL", "POSTGRES");
    }

    protected PostgresqlAdvDialectProvider(String dialectId, String... productNameKeywords) {
        super(dialectId, DialectName.POSTGRESQL, productNameKeywords);
    }

    @Override
    public IAdvExecutor create(DataSource dataSource, String dataSourceName) {
        return GirSpringPGAdvExecutor.newInstance(dataSource, dataSourceName);
    }
}
