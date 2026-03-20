package cn.geoair.comp.dynamic.ds;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.simple.AdvSimpleDataSource;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.IoUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/10/9 10:38 @description： 数据源获取器
 */
public class DataSourceGetter implements IDataSourceGetter {

    private static final GiLogger log = GirLogger.getLoger();

    private DataSource dataSource = null;

    private String databaseName = null;


    protected String schemaName = null;

    Supplier<String> schemaNameGetterFunction;
    Supplier<String> databaseNameGetterFunction;

    protected String dataSourceId = null;

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
        if (schemaNameGetterFunction != null) {
            schemaName = schemaNameGetterFunction.get();
        }
    }

    public void setDatabaseNameGetterFunction(Supplier<String> databaseNameGetterFunction) {
        this.databaseNameGetterFunction = databaseNameGetterFunction;
        if (databaseName != null) {
            return;
        }
        if (databaseNameGetterFunction != null) {
            databaseName = databaseNameGetterFunction.get();
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
        schemaName = dataSourceApo.getSchemaName();
        databaseName = dataSourceApo.getDbName();
        if (AdvDynamicDataSourceStorage.getInstance().containsDataSource(dataSourceId)) {
            dataSource = AdvDynamicDataSourceStorage.getInstance().getDataSource(dataSourceId);
        } else {
            dataSource = AdvDynamicDataSourceStorage.getInstance().getDruidDataSourceByDataSourceApo(dataSourceApo);

        }
    }

    @Override
    public void initByDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        this.dataSourceId = "";
        this.dataSourceApo = null;
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

//	@Override
//	public DataStore getGeoToolsDataStore() {
//		return dataStore;
//	}

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

}
