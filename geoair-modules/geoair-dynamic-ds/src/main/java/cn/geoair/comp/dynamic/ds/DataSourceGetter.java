package cn.geoair.comp.dynamic.ds;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.geoair.comp.dynamic.ds.dswrapper.DataSourceWrapperRegistry;
import cn.geoair.comp.dynamic.ds.simple.AdvSimpleDataSource;
import cn.geoair.comp.dynamic.ds.tx.*;
import cn.geoair.comp.dynamic.ds.tx.enums.IsolationLevel;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.sql.DataSource;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/10/9 10:38 @description： 数据源获取器
 */
public class DataSourceGetter implements IDataSourceGetter {

    private static final GiLogger log = GirLoggerFactory.getLogger();

    static ConcurrentHashMap<String, String> dataBaseNameMap = new ConcurrentHashMap<>();
    static ConcurrentHashMap<String, String> schemaNameMap = new ConcurrentHashMap<>();

    private DataSource dataSource = null;

    private String databaseName = null;

    private String dataSourceName = null;

    protected String schemaName = null;

    Supplier<String> schemaNameGetterFunction;

    Supplier<String> databaseNameGetterFunction;

    protected String dataSourceId = null;


    GirDefaultIDsTxTemplate jdbcTxTemplate = new GirDefaultIDsTxTemplate(this);


    @Override
    public String getSchemaName() {
        return schemaName;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public void setSchemaNameGetterFunction(Supplier<String> schemaNameGetterFunction) {
        this.schemaNameGetterFunction = schemaNameGetterFunction;
        if (schemaName != null) {
            return;
        }
        if (GutilObject.isNotEmpty(dataSourceName) && schemaNameMap.containsKey(dataSourceName)) {
            schemaName = schemaNameMap.get(dataSourceName);
            return;
        }
        if (schemaNameGetterFunction != null) {
            schemaName = schemaNameGetterFunction.get();
            if (schemaName == null) {
                schemaName = "";
            }
            if (GutilObject.isNotEmpty(dataSourceName)) {
                schemaNameMap.put(dataSourceName, schemaName);
            }
        }
    }

    public void setDatabaseNameGetterFunction(Supplier<String> databaseNameGetterFunction) {
        this.databaseNameGetterFunction = databaseNameGetterFunction;
        if (databaseName != null) {
            return;
        }
        if (GutilObject.isNotEmpty(dataSourceName) && dataBaseNameMap.containsKey(dataSourceName)) {
            databaseName = dataBaseNameMap.get(dataSourceName);
            return;
        }
        if (databaseNameGetterFunction != null) {
            databaseName = databaseNameGetterFunction.get();
            if (databaseName == null) {
                databaseName = "";
            }
            if (GutilObject.isNotEmpty(dataSourceName)) {
                dataBaseNameMap.put(dataSourceName, databaseName);
            }
        }
    }

    @Override
    public String getDataSourceId() {
        return dataSourceId;
    }

    protected DataSourceApo dataSourceApo = null;

    @Override
    public void initByDataSourceApo(DataSourceApo dataSourceApo) {
        this.dataSourceApo = dataSourceApo;
        this.dataSourceId = dataSourceApo.getId();
        this.dataSourceName = dataSourceApo.getId();
        schemaName = dataSourceApo.getSchemaName();
        databaseName = dataSourceApo.getDbName();
        if (AdvDynamicDataSourceStorage.getInstance().containsDataSource(dataSourceId)) {
            dataSource = AdvDynamicDataSourceStorage.getInstance().getOrCreateDataSource(dataSourceId);
        } else {
            dataSource =
                    AdvDynamicDataSourceStorage.getInstance()
                            .getDataSourceByDataSourceApo(dataSourceApo);
        }
    }

    @Override
    public void initByDataSource(DataSource dataSource) {
        initByDataSource(dataSource, null);
    }

    @Override
    public void initByDataSource(DataSource dataSource, String dataSourceName) {
        if (StrUtil.isEmpty(dataSourceName)) {
            Optional<AdvDataSourceWrapper> wrapper =
                    DataSourceWrapperRegistry.getWrapper(dataSource);
            if (wrapper.isPresent()) {
                dataSourceName = wrapper.get().getJdbcUrl();
            }
        }
        this.dataSource = dataSource;
        this.dataSourceId = dataSourceName;
        this.dataSourceApo = null;
        if (GutilObject.isNotEmpty(dataSourceName)) {
            this.dataSourceName = dataSourceName;
        }
    }

    @Override
    public void initByConnection(Connection connection) {
        AdvSimpleDataSource simpleDataSource = new AdvSimpleDataSource(connection);
        initByDataSource(simpleDataSource);
    }

    @Override
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    // @Override
    // public DataStore getGeoToolsDataStore() {
    // return dataStore;
    // }

    @Override
    public void connectionClose(Connection connection) {
        IoUtil.close(connection);
    }

    @Override
    public DataSourceApo getDataSourceApo() {
        if (dataSourceApo == null) {
            return null;
        }
        DataSourceApo apo = new DataSourceApo();
        BeanUtil.copyProperties(dataSourceApo, apo);
        return apo;
    }

    /**
     * 关闭数据库资源
     */
    @Override
    public void closeResources(ResultSet rs, Statement stmt, Connection conn) {
        IoUtil.close(rs);
        IoUtil.close(stmt);
        IoUtil.close(conn);
    }

    @Override
    public void setJdbcTxHolder(IDsTxHolder jdbcTxHolder) {
        jdbcTxTemplate.setJdbcTxHolder(jdbcTxHolder);
    }

    @Override
    public void tx(Runnable action) {
        jdbcTxTemplate.tx(action);
    }

    @Override
    public void tx(IsolationLevel level, Runnable action) {
        jdbcTxTemplate.tx(level, action);
    }

    @Override
    public <T> T txReturn(Supplier<T> supplier) {
        return jdbcTxTemplate.txReturn(supplier);
    }

    @Override
    public <T> T txReturn(IsolationLevel level, Supplier<T> supplier) {
        return jdbcTxTemplate.txReturn(level, supplier);
    }

    @Override
    public <P> void tx(TxAction<P> action, P param) {
        jdbcTxTemplate.tx(action, param);
    }

    @Override
    public <P> void tx(IsolationLevel level, TxAction<P> action, P param) {
        jdbcTxTemplate.tx(level, action, param);
    }

    @Override
    public <P, R> R txReturn(TxFunc<P, R> func, P param) {
        return jdbcTxTemplate.txReturn(func, param);
    }

    @Override
    public <P, R> R txReturn(IsolationLevel level, TxFunc<P, R> func, P param) {
        return jdbcTxTemplate.txReturn(level, func, param);
    }

    @Override
    public GirDsJdbcTxBuilder builder() {
        return jdbcTxTemplate.builder();
    }
}
