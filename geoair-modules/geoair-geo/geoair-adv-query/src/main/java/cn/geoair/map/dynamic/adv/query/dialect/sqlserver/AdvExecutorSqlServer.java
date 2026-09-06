package cn.geoair.map.dynamic.adv.query.dialect.sqlserver;

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

/** SQL Server 动态高级查询执行器。@author 张逢吉 */
public class AdvExecutorSqlServer extends AbstractPxyAdvExecutor {
    private volatile IDataSourceGetter dataSourceGetter;
    private volatile IAdvBaseOpt baseOpt;
    private volatile IAdvDDLOpt ddlOpt;
    private volatile IAdvGeoPreOpt geoOpt;
    private volatile IAdvSimplePageOpt pageOpt;
    private volatile IAdvWhereSelectOpt whereSelectOpt;
    private AdvQueryGlobalConfig config = AdvQueryGlobalConfig.of();

    public AdvExecutorSqlServer() { }
    public AdvExecutorSqlServer(DataSourceApo source) { super(source); }
    public AdvExecutorSqlServer(DataSource source) { super(source); }
    public AdvExecutorSqlServer(DataSource source, String sourceName) { super(source, sourceName); }
    public AdvExecutorSqlServer(Connection connection) { super(connection); }

    @Override protected IDataSourceGetter getDataSourceGetter() {
        if (dataSourceGetter == null) synchronized (this) {
            if (dataSourceGetter == null) dataSourceGetter = new GirDsTransactionManager(new RealDataSourceOpt());
        }
        return dataSourceGetter;
    }
    @Override protected IAdvBaseOpt getAdvBaseOpt() {
        if (baseOpt == null) synchronized (this) { if (baseOpt == null) baseOpt = new SqlServerAdvBaseOpt(getDataSourceGetter(), this::getConfig); }
        return baseOpt;
    }
    @Override protected IAdvDDLOpt getAdvDDLOpt() {
        if (ddlOpt == null) synchronized (this) { if (ddlOpt == null) ddlOpt = new SqlServerAdvDDLOpt(getDataSourceGetter(), getAdvBaseOpt()); }
        return ddlOpt;
    }
    @Override protected IAdvGeoPreOpt getGeoOpt() {
        if (geoOpt == null) synchronized (this) { if (geoOpt == null) geoOpt = new SqlServerAdvGeoOpt(getDataSourceGetter(), getAdvBaseOpt(), getAdvDDLOpt()); }
        return geoOpt;
    }
    @Override protected IAdvSimplePageOpt getSimplePageOpt() {
        if (pageOpt == null) synchronized (this) { if (pageOpt == null) pageOpt = new SqlServerAdvSimplePageOpt(getDataSourceGetter(), getAdvBaseOpt(), getGeoOpt(), getAdvDDLOpt()); }
        return pageOpt;
    }
    @Override public IAdvWhereSelectOpt getWhereSelectOpt() {
        if (whereSelectOpt == null) synchronized (this) { if (whereSelectOpt == null) whereSelectOpt = new SqlServerAdvWhereSelectOpt(getDataSourceGetter(), getAdvBaseOpt(), getSimplePageOpt(), getGeoOpt()); }
        return whereSelectOpt;
    }
    @Override protected DialectTableNameProcessor getDialectTableNameProcessor() { return SqlServerDialectTableNameUtil.getInstance(); }
    @Override public AdvQueryGlobalConfig getConfig() { return config == null ? (config = AdvQueryGlobalConfig.of()) : config; }
}
