package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.db.dialect.DialectName;

import javax.sql.DataSource;

/** 达梦空间数据库原生方言提供者。 */
public class DmAdvDialectProvider extends AbstractAdvDialectProvider {

    public DmAdvDialectProvider() {
        super("dm", DialectName.DM, "DAMENG", "DM DBMS", "DM");
    }

    @Override
    public IAdvExecutor create(DataSource dataSource, String dataSourceName) {
        return GirSpringDmAdvExecutor.newInstance(dataSource, dataSourceName);
    }
}
