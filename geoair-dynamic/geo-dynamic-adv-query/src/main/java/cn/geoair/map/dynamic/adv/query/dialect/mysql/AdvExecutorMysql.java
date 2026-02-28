package cn.geoair.map.dynamic.adv.query.dialect.mysql;

import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractAdvExecutor;
import cn.geoair.map.dynamic.adv.query.dialect.pg.*;
import cn.geoair.map.dynamic.ds.DataSourceGetter;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.ds.apo.DataSourceApo;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author ：张逢吉
 * @date ：Created in   15:36
 * @description： PostgreSQL数据库的动态高级查询执行器
 */
public class AdvExecutorMysql extends AbstractAdvExecutor {

    public AdvExecutorMysql(DataSourceApo dataSourceApo) {
        super(dataSourceApo);
    }

    public AdvExecutorMysql(DataSource dataSource) {
        super(dataSource);
    }

    public AdvExecutorMysql() {
    }

    public AdvExecutorMysql(Connection connection) {
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
                    advBaseOpt = new MysqlAdvBaseOpt(this);
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
                    advDDLOpt = new MysqlAdvDDLOpt(this);
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
                    simplePageOpt = new MysqlAdvSimplePageOpt(this);
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
                    geoOpt = new MysqlAdvGeoOpt(this);
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
