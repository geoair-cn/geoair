package cn.geoair.map.dynamic.adv.query.dialect.sqlite;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.base.RealDataSourceOpt;
import cn.geoair.comp.dynamic.ds.tx.GirDsTransactionManager;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.IAdvSimplePageOpt;
import cn.geoair.map.dynamic.adv.query.IAdvWhereSelectOpt;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvExecutor;

import java.sql.Connection;
import javax.sql.DataSource;

/** SQLite Core 的动态高级查询执行器。 */
public class AdvExecutorSqlite extends AbstractPxyAdvExecutor {

    private volatile IDataSourceGetter dataSourceGetter;
    private volatile IAdvBaseOpt baseOpt;
    private volatile IAdvDDLOpt ddlOpt;
    private volatile IAdvGeoPreOpt geoOpt;
    private volatile IAdvSimplePageOpt pageOpt;
    private volatile IAdvWhereSelectOpt whereSelectOpt;

    private AdvQueryGlobalConfig config = AdvQueryGlobalConfig.of();

    public AdvExecutorSqlite(DataSourceApo dataSourceApo) {
        super(dataSourceApo);
    }

    public AdvExecutorSqlite(DataSource dataSource) {
        super(dataSource);
    }

    public AdvExecutorSqlite(DataSource dataSource, String dataSourceName) {
        super(dataSource, dataSourceName);
    }

    public AdvExecutorSqlite() {}

    public AdvExecutorSqlite(Connection connection) {
        super(connection);
    }

    @Override
    protected IDataSourceGetter getDataSourceGetter() {
        if (dataSourceGetter == null) {
            synchronized (this) {
                if (dataSourceGetter == null) {
                    dataSourceGetter =
                            new GirDsTransactionManager(new RealDataSourceOpt());
                }
            }
        }
        return dataSourceGetter;
    }

    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        if (baseOpt == null) {
            synchronized (this) {
                if (baseOpt == null) {
                    baseOpt = new SqliteAdvBaseOpt(getDataSourceGetter(), this::getConfig);
                }
            }
        }
        return baseOpt;
    }

    @Override
    protected IAdvDDLOpt getAdvDDLOpt() {
        if (ddlOpt == null) {
            synchronized (this) {
                if (ddlOpt == null) {
                    ddlOpt = new SqliteAdvDDLOpt(getDataSourceGetter(), getAdvBaseOpt());
                }
            }
        }
        return ddlOpt;
    }

    @Override
    protected IAdvGeoPreOpt getGeoOpt() {
        if (geoOpt == null) {
            synchronized (this) {
                if (geoOpt == null) {
                    geoOpt = new SqliteAdvGeoOpt(
                            getDataSourceGetter(), getAdvBaseOpt(), getAdvDDLOpt());
                }
            }
        }
        return geoOpt;
    }

    @Override
    protected IAdvSimplePageOpt getSimplePageOpt() {
        if (pageOpt == null) {
            synchronized (this) {
                if (pageOpt == null) {
                    pageOpt = new SqliteAdvSimplePageOpt(
                            getDataSourceGetter(), getAdvBaseOpt(), getGeoOpt(), getAdvDDLOpt());
                }
            }
        }
        return pageOpt;
    }

    @Override
    public IAdvWhereSelectOpt getWhereSelectOpt() {
        if (whereSelectOpt == null) {
            synchronized (this) {
                if (whereSelectOpt == null) {
                    whereSelectOpt = new SqliteAdvWhereSelectOpt(
                            getDataSourceGetter(), getAdvBaseOpt(), getSimplePageOpt(), getGeoOpt());
                }
            }
        }
        return whereSelectOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return SqliteDialectTableNameUtil.getInstance();
    }

    @Override
    public AdvQueryGlobalConfig getConfig() {
        if (config == null) {
            config = AdvQueryGlobalConfig.of();
        }
        return config;
    }
}
