package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.comp.dynamic.ds.DataSourceGetter;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractAdvExecutor;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author ：张逢吉
 * @date ：Created in 15:36 @description： PostgreSQL数据库的动态高级查询执行器
 */
public class AdvExecutorPG extends AbstractAdvExecutor {

    public AdvExecutorPG(DataSourceApo dataSourceApo) {
        super(dataSourceApo);
    }

    public AdvExecutorPG(DataSource dataSource) {
        super(dataSource);
    }
    public AdvExecutorPG(DataSource dataSource,String dataSourceName) {
        super(dataSource,dataSourceName);
    }

    public AdvExecutorPG() {
    }

    public AdvExecutorPG(Connection connection) {
        super(connection);
    }

    @Override
    protected IDataSourceGetter getDataSourceGetterPxy() {
        if (dataSourceGetterPxy == null) {
            dataSourceGetterPxy = new DataSourceGetter();
        }
        return dataSourceGetterPxy;
    }

    private volatile IAdvBaseOpt advBaseOpt;

    private volatile IAdvDDLOpt advDDLOpt;

    private volatile IAdvSimplePagePreOpt simplePageOpt;

    private volatile IAdvGeoPreOpt geoOpt;

    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        if (advBaseOpt == null) {
            synchronized (this) {
                if (advBaseOpt == null) {
                    advBaseOpt = new PgAdvBaseOpt(this);
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
                    advDDLOpt = new PgAdvDDLOpt(this);
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
                    simplePageOpt = new PgAdvSimplePageOpt(this);
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
                    geoOpt = new PgAdvGeoOpt(this);
                }
            }
        }
        return geoOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return PgDialectTableNameUtil.getInstance();
    }


}
