package cn.geoair.comp.dynamic.ds.base;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.AdvDynamicDataSourceStorage;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.base.supplier.GirSysSupplierGetter;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.geoair.comp.dynamic.ds.dswrapper.DataSourceWrapperRegistry;
import cn.geoair.comp.dynamic.ds.simple.AdvSimpleDataSource;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;

import java.sql.Connection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.sql.DataSource;

/**
 * 真实的数据源管理器实现
 */
public class RealDataSourceOpt implements IDsDataSourceOpt {

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


    public RealDataSourceOpt() {

    }

    @Override
    public String getSchemaName() {

        if (schemaName != null) {
            return schemaName;
        }
        if (schemaNameGetterFunction != null && !(schemaNameGetterFunction instanceof GirSysSupplierGetter)) {
            String userGetter = schemaNameGetterFunction.get();   // 用户指定的getter 优先级最高
            schemaName = GutilObject.isEmpty(userGetter) ? "" : userGetter;
            return schemaName;
        }
        // 尝试从缓存中获取
        if (GutilObject.isNotEmpty(dataSourceName) && schemaNameMap.containsKey(dataSourceName)) {
            schemaName = schemaNameMap.get(dataSourceName);
            return schemaName;
        }
    // 这应该是系统的getter
        if (schemaNameGetterFunction != null) {
            String name = schemaNameGetterFunction.get();
            if (name != null) {
                schemaName = name;
                if (GutilObject.isNotEmpty(dataSourceName)) {
                    schemaNameMap.put(dataSourceName, schemaName);
                }
            } else {
                schemaName = "";
            }
            return schemaName;
        }
        return "";

    }

    @Override
    public String getDatabaseName() {
        if (databaseName != null) {
            return databaseName;
        }
        if (databaseNameGetterFunction != null && !(databaseNameGetterFunction instanceof GirSysSupplierGetter)) {
            String userGetter = databaseNameGetterFunction.get();
            databaseName = GutilObject.isEmpty(userGetter) ? "" : userGetter;
            return databaseName;
        }
        if (GutilObject.isNotEmpty(dataSourceName) && dataBaseNameMap.containsKey(dataSourceName)) {
            databaseName = dataBaseNameMap.get(dataSourceName);
            return databaseName;
        }
        if (databaseNameGetterFunction != null) {
            String name = databaseNameGetterFunction.get();
            if (name != null) {
                databaseName = name;
                if (GutilObject.isNotEmpty(dataSourceName)) {
                    dataBaseNameMap.put(dataSourceName, databaseName);
                }
            } else {
                databaseName = "";
            }
            return databaseName;
        }
        return "";
    }

    @Override
    public void setSchemaNameGetterFunction(Supplier<String> schemaNameGetterFunction) {
        if (schemaNameGetterFunction != null) {
            this.schemaNameGetterFunction = schemaNameGetterFunction;
        }


    }

    public void setDatabaseNameGetterFunction(Supplier<String> databaseNameGetterFunction) {
        if (databaseNameGetterFunction != null) {
            this.databaseNameGetterFunction = databaseNameGetterFunction;
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
        this.dataSourceName = dataSourceApo.getId() + "_" + dataSourceApo.getSchemaName();
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
    public DataSource getDataSource() {
        return dataSource;
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

}
