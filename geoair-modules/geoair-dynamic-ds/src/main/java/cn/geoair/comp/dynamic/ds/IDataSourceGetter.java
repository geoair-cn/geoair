package cn.geoair.comp.dynamic.ds;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.function.Supplier;

import javax.sql.DataSource;

import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;

/**
 * 数据源获取器接口
 * <p>
 * 该接口定义了数据源的初始化、获取和管理方法，提供了多种初始化方式和资源管理功能。
 * 实现该接口的类可以通过不同的方式初始化数据源，并提供获取数据库连接、关闭资源等操作。
 * </p>
 *
 * @author zhangjun
 * @date Created in 2025/10/9 10:38
 */
public interface IDataSourceGetter {

    /**
     * 通过数据源描述对象初始化
     * <p>
     * 使用 {@link DataSourceApo} 对象初始化数据源获取器，包含数据源的配置信息。
     * </p>
     *
     * @param dataSourceApo 数据源描述对象，包含数据源的配置信息
     */
    void initByDataSourceApo(DataSourceApo dataSourceApo);

    /**
     * 通过数据源对象初始化
     * <p>
     * 使用已有的 {@link DataSource} 对象初始化数据源获取器。
     * </p>
     *
     * @param dataSource 数据源对象
     */
    void initByDataSource(DataSource dataSource);

    /**
     * 通过数据源对象初始化
     * <p>
     * 使用已有的 {@link DataSource} 对象初始化数据源获取器。
     * </p>
     *
     * @param dataSource 数据源对象
     * @param dataSourceName 数据源的名称
     */
    void initByDataSource(DataSource dataSource,String dataSourceName);

    /**
     * 通过数据库连接初始化
     * <p>
     * 使用已有的数据库连接对象初始化数据源获取器。
     * </p>
     *
     * @param connection 数据库连接对象
     */
    void initByConnection(Connection connection);

    /**
     * 获取数据库模式名称
     * <p>
     * 返回当前数据源的数据库模式名称。
     * </p>
     *
     * @return 数据库模式名称
     */
    String getSchemaName();
    /**
     * 获取数据库名称
     * <p>
     * 返回当前数据源的数据库名称。
     * </p>
     *
     * @return 数据库名称
     */
    String getDatabaseName();

    /**
     * 设置模式名称获取函数
     * <p>
     * 通过 Supplier 函数动态获取模式名称，提供灵活的模式名称获取方式。
     * </p>
     *
     * @param schemaNameGetterFunction 模式名称获取函数
     */
    void setSchemaNameGetterFunction(Supplier<String> schemaNameGetterFunction);

    /**
     * 设置数据库名称获取函数
     * <p>
     * 通过 Supplier 函数动态获取数据库名称，提供灵活的数据库名称获取方式。
     * </p>
     *
     * @param databaseNameGetterFunction 数据库名称获取函数
     */
    void setDatabaseNameGetterFunction(Supplier<String> databaseNameGetterFunction);

    /**
     * 获取数据源ID
     * <p>
     * 返回当前数据源的唯一标识符。
     * </p>
     *
     * @return 数据源ID
     */
    String getDataSourceId();

    /**
     * 获取数据库连接
     * <p>
     * 从数据源中获取一个数据库连接对象。
     * </p>
     *
     * @return 数据库连接对象
     */
    Connection getConnection();

    /**
     * 获取数据源的描述对象
     * <p>
     * 返回当前数据源的描述对象，包含数据源的配置信息。
     * </p>
     *
     * @return 数据源描述对象
     */
    DataSourceApo getDataSourceApo();

    /**
     * 获取数据源
     * <p>
     * 返回当前使用的数据源对象。
     * </p>
     *
     * @return 数据源对象
     */
    DataSource getDataSource();

//	/**
//	 * 获取geotools封装的dataStore
//	 * @return
//	 */
//	DataStore getGeoToolsDataStore();

    /**
     * 关闭数据库连接
     * <p>
     * 关闭指定的数据库连接，释放资源。
     * </p>
     *
     * @param connection 需要关闭的数据库连接
     */
    void connectionClose(Connection connection);

    /**
     * 关闭数据库资源
     * <p>
     * 关闭结果集、语句和连接等数据库资源，释放系统资源。
     * </p>
     *
     * @param rs   结果集对象
     * @param stmt 语句对象
     * @param conn 连接对象
     */
    void closeResources(ResultSet rs, Statement stmt, Connection conn);

}
