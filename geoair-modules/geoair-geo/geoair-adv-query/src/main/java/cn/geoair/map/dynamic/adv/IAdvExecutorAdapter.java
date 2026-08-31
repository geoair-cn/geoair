package cn.geoair.map.dynamic.adv;

import cn.geoair.map.dynamic.adv.query.IAdvExecutor;

/**
 * IAdvExecutor 的获取适配器，隔离数据源查找与执行器创建逻辑。
 *
 * <p>调用方无需关心数据源如何查找、方言如何检测、Schema 如何设置， 只需传入数据源标识即可获取现成的 {@link IAdvExecutor}。
 *
 * <p>{@code IAdvExecutorAdapter} 是可扩展接口：如果默认实现 {@link
 * cn.geoair.map.dynamic.adv.spring.CommonAdvExecutorAdapter} 不满足需求， 调用方可以实现自己的适配器来定制数据源查找方式。
 *
 * <p>典型用法：
 *
 * <pre>{@code
 * IAdvExecutorAdapter adapter = GirService.getPxyBeanC(IAdvExecutorAdapter.class);
 * IAdvExecutor executor = adapter.getIAdvExecutor("master", "public");
 * }</pre>
 *
 * @author zhangjun
 * @date 2025/10/9 15:03
 * @see cn.geoair.map.dynamic.adv.spring.CommonAdvExecutorAdapter 默认实现
 */
public interface IAdvExecutorAdapter {

    /**
     * 通过数据源 ID + Schema 获取执行器
     *
     * <p>适配器内部负责：
     *
     * <ol>
     *   <li>根据 dataSourceId 查找对应的 DataSource
     *   <li>自动检测数据库方言，创建匹配的 Executor
     *   <li>设置默认 Schema
     * </ol>
     *
     * @param dataSourceId 数据源标识（对应多数据源配置中的名称）
     * @param schema 默认 Schema（如 PostgreSQL 的 "public"），可为空
     * @param <T> IAdvExecutor 子类型
     * @return 匹配数据库方言的执行器实例
     */
    <T extends IAdvExecutor> T getIAdvExecutor(String dataSourceId, String schema);

    /**
     * 通过数据源 ID + Schema + 实现类型获取执行器
     *
     * <p>与 {@link #getIAdvExecutor(String, String)} 功能相同， 额外允许指定返回类型，方便直接获取方言特定的 Executor 子类。
     *
     * @param dataSourceId 数据源标识
     * @param schema 默认 Schema
     * @param clazz 期望的 Executor 实现类型
     * @param <T> IAdvExecutor 子类型
     * @return 指定类型的执行器实例
     */
    <T extends IAdvExecutor> T getIAdvExecutor(String dataSourceId, String schema, Class<T> clazz);
}
