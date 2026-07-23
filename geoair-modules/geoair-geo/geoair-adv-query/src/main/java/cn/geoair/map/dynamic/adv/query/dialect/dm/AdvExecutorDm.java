package cn.geoair.map.dynamic.adv.query.dialect.dm;

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

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 达梦数据库的动态高级查询执行器
 */
public class AdvExecutorDm extends AbstractPxyAdvExecutor {

    public AdvExecutorDm(DataSourceApo dataSourceApo) {
        super(dataSourceApo);
    }

    public AdvExecutorDm(DataSource dataSource) {
        super(dataSource);
    }

    public AdvExecutorDm(DataSource dataSource, String dataSourceName) {
        super(dataSource, dataSourceName);
    }

    public AdvExecutorDm() {
    }

    public AdvExecutorDm(Connection connection) {
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
                    advBaseOpt = new DmAdvBaseOpt(getDataSourceGetter(), this::getConfig);
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
                    advDDLOpt = new DmAdvDDLOpt(getDataSourceGetter(), getAdvBaseOpt());
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
                    simplePageOpt = new DmAdvSimplePageOpt(getDataSourceGetter(), getAdvBaseOpt(), getGeoOpt(), getAdvDDLOpt());
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
                    geoOpt = new DmAdvGeoOpt(getDataSourceGetter(), getAdvBaseOpt(), getAdvDDLOpt());
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
                    iAdvWhereSelectOpt = new DmAdvWhereSelectOpt(getDataSourceGetter(), getAdvBaseOpt(), getSimplePageOpt(), getGeoOpt());
                }
            }
        }
        return iAdvWhereSelectOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return DmDialectTableNameUtil.getInstance();
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
