package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.comp.dynamic.ds.DataSourceGetter;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.tx.IDsTxTemplate;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvExecutor;
import cn.geoair.map.dynamic.adv.query.dialect.pg.*;

import java.sql.Connection;
import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 15:36 @description： PostgreSQL数据库的动态高级查询执行器
 */
public class AdvExecutorMysql extends AbstractPxyAdvExecutor {

    public AdvExecutorMysql(DataSourceApo dataSourceApo) {
        super(dataSourceApo);
    }

    public AdvExecutorMysql(DataSource dataSource) {
        super(dataSource);
    }

    public AdvExecutorMysql(DataSource dataSource, String dataSourceName) {
        super(dataSource, dataSourceName);
    }

    public AdvExecutorMysql() {
    }

    public AdvExecutorMysql(Connection connection) {
        super(connection);
    }

    @Override
    protected IDataSourceGetter getDataSourceGetter() {
        if (dataSourceGetter == null) {
            synchronized (this) {
                if (dataSourceGetter == null) {
                    dataSourceGetter = new DataSourceGetter();
                }
            }
        }
        return dataSourceGetter;
    }

    @Override
    protected IDsTxTemplate getAdvTxTemplate() {
        return getDataSourceGetter();
    }

    private volatile IAdvBaseOpt advBaseOpt;

    private volatile IDataSourceGetter dataSourceGetter;

    private volatile IAdvDDLOpt advDDLOpt;

    private volatile IAdvWhereSelectOpt iAdvWhereSelectOpt;

    private volatile IAdvSimplePageOpt simplePageOpt;

    private volatile IAdvGeoPreOpt geoOpt;

    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        if (advBaseOpt == null) {
            synchronized (this) {
                if (advBaseOpt == null) {
                    advBaseOpt = new MysqlAdvBaseOpt(getDataSourceGetter(), this::getConfig);
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
                    advDDLOpt = new MysqlAdvDDLOpt(getDataSourceGetter(), getAdvBaseOpt());
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
                    simplePageOpt = new MysqlAdvSimplePageOpt(getDataSourceGetter(), getAdvBaseOpt(), getGeoOpt(), getAdvDDLOpt());
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
                    geoOpt = new MysqlAdvGeoOpt(getDataSourceGetter(), getAdvBaseOpt(), getAdvDDLOpt());
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
                    iAdvWhereSelectOpt = new MysqlAdvWhereSelectOpt(getDataSourceGetter(), getAdvBaseOpt(), getSimplePageOpt(), getGeoOpt());
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
