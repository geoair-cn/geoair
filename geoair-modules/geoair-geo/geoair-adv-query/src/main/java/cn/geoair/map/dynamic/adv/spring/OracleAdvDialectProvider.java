package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.db.dialect.DialectName;

import javax.sql.DataSource;

/** Oracle Spatial 原生方言提供者。 */
public class OracleAdvDialectProvider extends AbstractAdvDialectProvider {

    public OracleAdvDialectProvider() {
        super("oracle", DialectName.ORACLE, "ORACLE");
    }

    @Override
    public IAdvExecutor create(DataSource dataSource, String dataSourceName) {
        return GirSpringOracleAdvExecutor.newInstance(dataSource, dataSourceName);
    }
}
