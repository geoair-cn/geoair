package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.db.dialect.DialectName;

import javax.sql.DataSource;

/** MySQL 原生方言提供者。 */
public class MysqlAdvDialectProvider extends AbstractAdvDialectProvider {

    public MysqlAdvDialectProvider() {
        this("mysql", "MYSQL");
    }

    protected MysqlAdvDialectProvider(String dialectId, String... productNameKeywords) {
        super(dialectId, DialectName.MYSQL, productNameKeywords);
    }

    @Override
    public IAdvExecutor create(DataSource dataSource, String dataSourceName) {
        return GirSpringMysqlAdvExecutor.newInstance(dataSource, dataSourceName);
    }
}
