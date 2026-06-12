package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.base.RealDataSourceOpt;
import cn.geoair.comp.dynamic.ds.tx.GirDsTransactionManager;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvExecutor;
import java.sql.Connection;
import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 15:36 @description： PostgreSQL数据库的动态高级查询执行器
 */
public class AdvExecutorPG extends AbstractPxyAdvExecutor {

    public AdvExecutorPG(DataSourceApo dataSourceApo) {
        super(dataSourceApo);
    }

    public AdvExecutorPG(DataSource dataSource) {
        super(dataSource);
    }

    public AdvExecutorPG(DataSource dataSource, String dataSourceName) {
        super(dataSource, dataSourceName);
    }

    public AdvExecutorPG() {}

    public AdvExecutorPG(Connection connection) {
        super(connection);
    }

    private volatile IAdvBaseOpt advBaseOpt;
    private volatile IAdvDDLOpt advDDLOpt;
    private volatile IAdvWhereSelectOpt iAdvWhereSelectOpt;
    private volatile IDataSourceGetter dataSourceGetter;
    private volatile IAdvSimplePageOpt simplePageOpt;

    private volatile IAdvGeoPreOpt geoOpt;

    @Override
    protected IDataSourceGetter getDataSourceGetter() {
        if (dataSourceGetter == null) {
            synchronized (this) {
                if (dataSourceGetter == null) {
                    dataSourceGetter = new GirDsTransactionManager(new RealDataSourceOpt());
                }
            }
        }
        return dataSourceGetter;
    }

    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        if (advBaseOpt == null) {
            synchronized (this) {
                if (advBaseOpt == null) {
                    advBaseOpt = new PgAdvBaseOpt(getDataSourceGetter(), this::getConfig);
                }
            }
        }
        return advBaseOpt;
    }

    @Override
    protected IAdvDDLOpt getAdvDDLOpt() {
        if (advDDLOpt == null) {
            synchronized (this) {
                if (advDDLOpt == null) {
                    advDDLOpt = new PgAdvDDLOpt(getDataSourceGetter(), getAdvBaseOpt());
                }
            }
        }
        return advDDLOpt;
    }

    @Override
    protected IAdvSimplePageOpt getSimplePageOpt() {
        if (simplePageOpt == null) {
            synchronized (this) {
                if (simplePageOpt == null) {
                    simplePageOpt =
                            new PgAdvSimplePageOpt(
                                    getDataSourceGetter(),
                                    getAdvBaseOpt(),
                                    getGeoOpt(),
                                    getAdvDDLOpt());
                }
            }
        }
        return simplePageOpt;
    }

    @Override
    protected IAdvGeoPreOpt getGeoOpt() {
        if (geoOpt == null) {
            synchronized (this) {
                if (geoOpt == null) {
                    geoOpt =
                            new PgAdvGeoOpt(getDataSourceGetter(), getAdvBaseOpt(), getAdvDDLOpt());
                }
            }
        }
        return geoOpt;
    }

    @Override
    public IAdvWhereSelectOpt getWhereSelectOpt() {
        if (iAdvWhereSelectOpt == null) {
            synchronized (this) {
                if (iAdvWhereSelectOpt == null) {
                    iAdvWhereSelectOpt =
                            new PgAdvWhereSelectOpt(
                                    getDataSourceGetter(),
                                    getAdvBaseOpt(),
                                    getSimplePageOpt(),
                                    getGeoOpt());
                }
            }
        }
        return iAdvWhereSelectOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return PgDialectTableNameUtil.getInstance();
    }

    AdvQueryGlobalConfig advQueryGlobalConfig = AdvQueryGlobalConfig.of();

    @Override
    public AdvQueryGlobalConfig getConfig() {
        if (advQueryGlobalConfig == null) {
            advQueryGlobalConfig = AdvQueryGlobalConfig.of();
        }
        return advQueryGlobalConfig;
    }
}
