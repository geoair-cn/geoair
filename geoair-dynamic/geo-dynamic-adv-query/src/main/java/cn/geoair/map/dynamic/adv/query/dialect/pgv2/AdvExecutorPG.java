package cn.geoair.map.dynamic.adv.query.dialect.pgv2;

import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractAdvExecutor;
import cn.geoair.map.dynamic.ds.DataSourceGetter;
import cn.geoair.map.dynamic.ds.apo.DataSourceApo;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author ：张逢吉
 * @date ：Created in   15:36
 * @description： PostgreSQL数据库的动态高级查询执行器
 */
public class AdvExecutorPG extends AbstractAdvExecutor {

    public AdvExecutorPG(DataSourceApo dataSourceApo) {
        super(dataSourceApo);
    }

    public AdvExecutorPG(DataSource dataSource) {
        super(dataSource);
    }

    public AdvExecutorPG() {
    }

    public AdvExecutorPG(Connection connection) {
        super(connection);
    }

    @Override
    protected DataSourceGetter getDataSourceGetterPxy() {
        if (dataSourceGetterPxy == null) {
            dataSourceGetterPxy = new DataSourceGetter();
        }
        return dataSourceGetterPxy;
    }


    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        return new PgAdvBaseOpt(this);
    }

    @Override
    protected IAdvDDLOpt getAdvDDLOpt() {
        return new PgAdvDDLOpt(this);
    }

    @Override
    protected IAdvSimplePagePreOpt getSimplePageOpt() {
        return new PgAdvSimplePageOpt(this);
    }

    @Override
    protected IAdvGeoPreOpt getGeoOpt() {
        return new PgAdvGeoOpt(this);
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return PgDialectTableNameUtil.getInstance();
    }
}
