package cn.geoair.map.dynamic.adv.query.dialect.oracle;

import cn.geoair.comp.dynamic.ds.DataSourceGetter;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.map.dynamic.adv.config.ConfigAdvQuery;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractPxyAdvExecutor;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author ：张逢吉
 * @date ：Created in 15:36 @description： Oracle数据库的动态高级查询执行器
 */
public class AdvExecutorOracle extends AbstractPxyAdvExecutor {



    public AdvExecutorOracle(DataSourceApo dataSourceApo) {
        super(dataSourceApo);
    }

    public AdvExecutorOracle(DataSource dataSource) {
        super(dataSource);
    }

    public AdvExecutorOracle(DataSource dataSource, String dataSourceName) {
        super(dataSource, dataSourceName);
    }

    public AdvExecutorOracle() {
    }

    public AdvExecutorOracle(Connection connection) {
        super(connection);
    }


    private volatile IAdvBaseOpt advBaseOpt;
    private volatile IAdvDDLOpt advDDLOpt;
    private volatile IAdvWhereSelectOpt iAdvWhereSelectOpt;
    private volatile IDataSourceGetter dataSourceGetter;
    private volatile IAdvSimplePagePreOpt simplePageOpt;

    private volatile IAdvGeoPreOpt geoOpt;

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
    protected IAdvBaseOpt getAdvBaseOpt() {
        if (advBaseOpt == null) {
            synchronized (this) {
                if (advBaseOpt == null) {
                    advBaseOpt = new OracleAdvBaseOpt(getDataSourceGetter(),this::getConfig);
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
                    advDDLOpt = new OracleAdvDDLOpt(getDataSourceGetter(), getAdvBaseOpt());
                }
            }
        }
        return advDDLOpt;
    }

    @Override
    protected IAdvSimplePagePreOpt getSimplePageOpt() {
        if (simplePageOpt == null) {
            synchronized (this) {
                if (simplePageOpt == null) {
                    simplePageOpt = new OracleAdvSimplePageOpt(getDataSourceGetter(), getAdvBaseOpt(), getGeoOpt(), getAdvDDLOpt());
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
                    geoOpt = new OracleAdvGeoOpt(getDataSourceGetter(), getAdvBaseOpt(), getAdvDDLOpt());
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
                    iAdvWhereSelectOpt = new OracleAdvWhereSelectOpt(getDataSourceGetter(), getAdvBaseOpt(), getSimplePageOpt(), getGeoOpt());
                }
            }
        }
        return iAdvWhereSelectOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return OracleDialectTableNameUtil.getInstance();
    }
    ConfigAdvQuery configAdvQuery = ConfigAdvQuery.of();
    @Override
    public ConfigAdvQuery getConfig() {
        if(configAdvQuery==null){
            configAdvQuery = ConfigAdvQuery.of();
        }
        return configAdvQuery;
    }
}
