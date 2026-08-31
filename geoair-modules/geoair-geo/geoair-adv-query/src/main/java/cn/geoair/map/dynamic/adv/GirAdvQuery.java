package cn.geoair.map.dynamic.adv;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsTypeGeom;
import cn.geoair.map.dynamic.adv.spring.AdvExecutorFactory;
import cn.geoair.map.dynamic.tools.GirService;
import cn.hutool.db.dialect.DialectName;
import javax.sql.DataSource;

/**
 * 高级查询快捷入口，提供静态方法快速获取 {@link IAdvExecutor} 实例。
 *
 * <p>日常编码中不需要手动构造执行器，直接调用本类的静态方法即可：
 *
 * <pre>{@code
 * // Spring 环境，按数据源 ID 获取
 * IAdvExecutor executor = GirAdvQuery.getIAdvExecutor("master", "public");
 *
 * // 已知 DataSource
 * IAdvExecutor executor = GirAdvQuery.getIAdvExecutor(dataSource);
 *
 * // 已知数据库方言，跳过 JDBC 探测
 * IAdvExecutor executor = GirAdvQuery.getIAdvExecutor(DialectName.MYSQL, dataSource, "myDs");
 * }</pre>
 *
 * @author 张逢吉
 * @date 2025/10/9 11:02
 */
public class GirAdvQuery {

    /**
     * 通过数据源 ID + Schema 获取执行器（Spring 环境）
     *
     * <p>内部通过 {@link IAdvExecutorAdapter} 查找对应数据源， 自动检测数据库方言创建匹配的 Executor，并设置 Schema。
     *
     * @param dataSourceId 数据源标识（对应多数据源配置中的名称）
     * @param schema 默认 Schema（如 PostgreSQL 的 "public"），可为空
     * @return 匹配数据库方言的 IAdvExecutor 实例
     */
    public static IAdvExecutor getIAdvExecutor(String dataSourceId, String schema) {
        IAdvExecutorAdapter pxyBeanC = GirService.getPxyBeanC(IAdvExecutorAdapter.class);
        return pxyBeanC.getIAdvExecutor(dataSourceId, schema);
    }

    /**
     * 通过数据源 ID + Schema + 实现类型获取执行器（Spring 环境）
     *
     * <p>与 {@link #getIAdvExecutor(String, String)} 功能相同，但允许指定返回类型， 方便直接获取方言特定的 Executor 子类。
     *
     * @param dataSourceId 数据源标识
     * @param schema 默认 Schema
     * @param clazz 期望的 Executor 实现类型
     * @param <T> IAdvExecutor 子类型
     * @return 指定类型的 Executor 实例
     */
    public static <T extends IAdvExecutor> T getIAdvExecutor(
            String dataSourceId, String schema, Class<T> clazz) {
        IAdvExecutorAdapter pxyBeanC = GirService.getPxyBeanC(IAdvExecutorAdapter.class);
        return pxyBeanC.getIAdvExecutor(dataSourceId, schema, clazz);
    }

    /**
     * 通过 DataSource 快速获取执行器（自动检测数据库方言）
     *
     * <p>不依赖 Spring 上下文，直接从 DataSource 获取 JDBC 连接探测数据库类型。 适用于非 Spring 环境或手动管理 DataSource 的场景。
     *
     * @param dataSource 数据源对象
     * @return 匹配数据库方言的 IAdvExecutor 实例
     */
    public static IAdvExecutor getIAdvExecutor(DataSource dataSource) {
        return AdvExecutorFactory.getAdvExecutorByDataSource(dataSource);
    }

    /**
     * 通过 DataSource + 名称获取执行器（自动检测数据库方言）
     *
     * <p>与 {@link #getIAdvExecutor(DataSource)} 功能相同，额外指定数据源名称用于日志和追踪。
     *
     * @param dataSource 数据源对象
     * @param dataSourceName 数据源名称（用于日志标识，可为 null）
     * @return 匹配数据库方言的 IAdvExecutor 实例
     */
    public static IAdvExecutor getIAdvExecutor(DataSource dataSource, String dataSourceName) {
        return AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, dataSourceName);
    }

    /**
     * 通过 DataSource + 方言直接获取执行器（跳过 JDBC 连接探测）
     *
     * <p>当调用方已经明确知道数据库类型时，使用此方法可以完全跳过 {@link
     * AdvExecutorFactory#getAdvExecutorByDataSource(DataSource, String)} 中的 JDBC
     * 连接探测，消除获取/释放连接的性能开销。
     *
     * <pre>{@code
     * // 从配置文件读取数据库类型后直接指定
     * IAdvExecutor executor = GirAdvQuery.getIAdvExecutor(
     *         DialectName.MYSQL, dataSource, "master");
     * }</pre>
     *
     * @param dialectName 数据库方言（不能为 null）
     * @param dataSource 数据源对象
     * @param dataSourceName 数据源名称（用于日志标识，可为 null）
     * @return 匹配指定方言的 IAdvExecutor 实例
     * @throws IllegalArgumentException 如果 dialectName 为 null
     */
    public static IAdvExecutor getIAdvExecutor(
            DialectName dialectName, DataSource dataSource, String dataSourceName) {
        return AdvExecutorFactory.getAdvExecutorByDialect(dialectName, dataSource, dataSourceName);
    }

    public static void main(String[] args) {
        IAdvExecutor iAdvExecutor = GirAdvQuery.getIAdvExecutor("", "");
        iAdvExecutor.bSelectOne("");
        iAdvExecutor.bSelectList("");
        String s = iAdvExecutor.eGetGeomColumnNameBySql("");
        AdvEnumsTypeGeom advEnumsTypeGeom = iAdvExecutor.eGetGeoTypeBySql("");
    }
}
