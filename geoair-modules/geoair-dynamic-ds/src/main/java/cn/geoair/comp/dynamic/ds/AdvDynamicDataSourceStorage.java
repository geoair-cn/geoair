package cn.geoair.comp.dynamic.ds;

import cn.geoair.base.Gir;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.runtime.GutilShutdownHook;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import lombok.Setter;

/**
 * 动态数据源的存储器实现类，全局单例。
 *
 * <p>负责动态数据源的完整生命周期管理：
 *
 * <ul>
 *   <li>通过 {@code dataSourceId} 查找、创建、缓存 {@link AdvDataSourceWrapper}
 *   <li>通过 {@link IAdvDataSourceHelper} 从外部存储（数据库/配置中心）加载数据源配置
 *   <li>通过 {@link IAdvDataSourceInitHelper} 将配置转为实际的 DataSource 对象
 *   <li>JVM 关闭时自动清空缓存并释放所有连接（注册了 ShutdownHook）
 * </ul>
 *
 * <p>使用示例：
 *
 * <pre>{@code
 * // 获取全局实例
 * DynamicDataSourceManager manager = AdvDynamicDataSourceStorage.getInstance();
 *
 * // 按 ID 获取或创建数据源
 * AdvDataSourceWrapper ds = manager.getOrCreateDataSource("master");
 *
 * // 手动注册数据源
 * manager.registerDataSource("extra", someDataSource);
 * }</pre>
 */
public class AdvDynamicDataSourceStorage implements DynamicDataSourceManager {

    private static final GiLogger log = GirLoggerFactory.getLogger();

    /** DCL 所需的 volatile 保证可见性 */
    private static volatile AdvDynamicDataSourceStorage dataSourceManager;

    /**
     * 数据源配置的查找助手，负责根据 dataSourceId 获取对应的 {@link DataSourceApo} 配置对象。
     *
     * <p>可通过 setter 手动注入，若不注入则自动从 Spring 容器获取。
     */
    @Setter private IAdvDataSourceHelper iAdvDataSourceHelper;

    /**
     * 数据源的初始化助手，负责将 {@link DataSourceApo} 配置对象转换为实际的 {@link DataSource}。
     *
     * <p>可通过 setter 手动注入，若不注入则自动从 Spring 容器获取。
     */
    @Setter private IAdvDataSourceInitHelper iAdvDataSourceInitHelper;

    /**
     * 获取全局唯一的存储器实例（线程安全）。
     *
     * @return 单例实例
     */
    public static DynamicDataSourceManager getInstance() {
        if (dataSourceManager == null) {
            synchronized (AdvDynamicDataSourceStorage.class) {
                if (dataSourceManager == null) {
                    dataSourceManager = new AdvDynamicDataSourceStorage();
                }
            }
        }
        return dataSourceManager;
    }

    /**
     * 获取全局唯一的存储器实例，并注入数据源配置查找助手。
     *
     * <p>相比 {@link #getInstance()} 增加了 helper 注入能力， 避免在非 Spring 环境下因自动查找 Bean 而抛异常。
     *
     * @param iAdvDataSourceHelper 数据源配置查找助手
     * @return 单例实例
     */
    public static DynamicDataSourceManager getInstance(IAdvDataSourceHelper iAdvDataSourceHelper) {
        synchronized (AdvDynamicDataSourceStorage.class) {
            if (dataSourceManager == null) {
                AdvDynamicDataSourceStorage instance = new AdvDynamicDataSourceStorage();
                instance.iAdvDataSourceHelper = iAdvDataSourceHelper;
                dataSourceManager = instance;
            } else if (dataSourceManager.iAdvDataSourceHelper == null) {
                dataSourceManager.iAdvDataSourceHelper = iAdvDataSourceHelper;
            }
        }
        return dataSourceManager;
    }

    /** 设置数据源初始化助手（便捷方法，等价于 setter）。 */
    public void setAdvDataSourceInitHelper(IAdvDataSourceInitHelper helper) {
        this.iAdvDataSourceInitHelper = helper;
    }

    /**
     * 获取数据源配置查找助手。
     *
     * <p>优先使用已注入的实例（通过 setter 或 {@link #getInstance(IAdvDataSourceHelper)}）； 若未注入，则尝试从 Spring
     * 容器中自动获取。
     *
     * @return 数据源配置查找助手
     * @throws RuntimeException 当未注入且 Spring 容器中找不到实现类时抛出
     */
    public IAdvDataSourceHelper getAdvDataSourceHelper() {
        if (iAdvDataSourceHelper == null) {
            synchronized (this) {
                if (iAdvDataSourceHelper == null) {
                    try {
                        iAdvDataSourceHelper = Gir.beans.getBean(IAdvDataSourceHelper.class);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "未注入 IAdvDataSourceHelper 且 Spring 容器中找不到实现类，"
                                        + "请通过 getInstance(IAdvDataSourceHelper) 或 setter 手动注入",
                                e);
                    }
                }
            }
        }
        return iAdvDataSourceHelper;
    }

    /**
     * 获取数据源初始化助手。
     *
     * <p>优先使用已注入的实例（通过 setter）；若未注入，则尝试从 Spring 容器中自动获取。
     *
     * @return 数据源初始化助手
     * @throws RuntimeException 当未注入且 Spring 容器中找不到实现类时抛出
     */
    public IAdvDataSourceInitHelper getAdvDataSourceInitHelper() {
        if (iAdvDataSourceInitHelper == null) {
            synchronized (this) {
                if (iAdvDataSourceInitHelper == null) {
                    try {
                        iAdvDataSourceInitHelper =
                                Gir.beans.getBean(IAdvDataSourceInitHelper.class);
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "未注入 IAdvDataSourceInitHelper 且 Spring 容器中找不到实现类，"
                                        + "请通过 setAdvDataSourceInitHelper() 手动注入",
                                e);
                    }
                }
            }
        }
        return iAdvDataSourceInitHelper;
    }

    /** 私有构造，注册 JVM 关闭钩子以释放所有数据源连接。 */
    private AdvDynamicDataSourceStorage() {
        GutilShutdownHook.getInstance().registerTask(this::cleanCache);
    }

    /** 数据源缓存映射（线程安全）。key: dataSourceId, value: 包装后的数据源 */
    private final Map<String, AdvDataSourceWrapper> dataSourceMap = new ConcurrentHashMap<>();

    /** 清空所有数据源缓存并关闭连接。 通常由 JVM ShutdownHook 自动调用，也可手动调用释放资源。 */
    @Override
    public void cleanCache() {
        log.info("执行清空数据源缓存并释放数据库链接操作！");
        if (ObjectUtil.isNotEmpty(dataSourceMap)) {
            Set<Map.Entry<String, AdvDataSourceWrapper>> entries = dataSourceMap.entrySet();
            for (Map.Entry<String, AdvDataSourceWrapper> entry : entries) {
                try {
                    entry.getValue().close();
                } catch (Exception e) {
                    log.error(e, "关闭数据源失败: {}", entry.getKey());
                }
            }
            dataSourceMap.clear();
        }
    }

    @Override
    public boolean containsDataSource(String dataSourceId) {
        return dataSourceMap.containsKey(dataSourceId);
    }

    /**
     * @deprecated 命名具有误导性（"get" 暗示只读但实际会触发创建），请使用 {@link #getOrCreateDataSource(String)} 或 {@link
     *     #getDataSourceById(String)}
     */
    @Override
    @Deprecated
    public AdvDataSourceWrapper getDataSource(String dataSourceId) {
        return getDataSourceById(dataSourceId);
    }

    /** 按 ID 查找数据源，不存在则返回 {@code null}（只读，不会触发创建）。 */
    @Override
    public AdvDataSourceWrapper getDataSourceById(String dataSourceId) {
        if (containsDataSource(dataSourceId)) {
            return dataSourceMap.get(dataSourceId);
        }
        return null;
    }

    /**
     * 按 ID 查找数据源，不存在则触发创建。
     *
     * <p>创建流程：
     *
     * <ol>
     *   <li>通过 {@link IAdvDataSourceHelper#getDataSourceApoById(String)} 获取配置
     *   <li>通过 {@link IAdvDataSourceInitHelper#getDbDataSourceByApo(DataSourceApo)} 创建物理数据源
     *   <li>包装为 {@link AdvDataSourceWrapper} 并缓存
     * </ol>
     *
     * @param dataSourceId 数据源标识
     * @return 数据源包装对象
     * @throws RuntimeException 当数据源配置查找或创建失败时抛出
     */
    @Override
    public AdvDataSourceWrapper getOrCreateDataSource(String dataSourceId) {
        AdvDataSourceWrapper existing = dataSourceMap.get(dataSourceId);
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            // 双重检查：避免并发重复创建
            AdvDataSourceWrapper doubleCheck = dataSourceMap.get(dataSourceId);
            if (doubleCheck != null) {
                return doubleCheck;
            }
            try {
                DataSourceApo dataSourceApoById =
                        getAdvDataSourceHelper().getDataSourceApoById(dataSourceId);
                AdvDataSourceWrapper dataSourceByDataSourceApo =
                        getDataSourceByDataSourceApo(dataSourceApoById);
                dataSourceMap.put(dataSourceId, dataSourceByDataSourceApo);
                return dataSourceByDataSourceApo;
            } catch (Exception e) {
                String msg = StrUtil.format("无法获取或创建数据源 [{}]: {}", dataSourceId, e.getMessage());
                log.error(e, msg);
                throw new RuntimeException(msg, e);
            }
        }
    }

    /** @deprecated 命名不规范，请使用 {@link #registerDataSource(String, DataSource)} */
    @Override
    @Deprecated
    public void putDataSource(DataSource dataSource, String dataSourceId) {
        registerDataSource(dataSourceId, dataSource);
    }

    /**
     * 手动注册一个已创建好的数据源到管理器。
     *
     * <p>如果相同 ID 的数据源已存在，不会覆盖（幂等操作）。
     *
     * @param dataSourceId 数据源标识
     * @param dataSource 数据源对象
     */
    @Override
    public void registerDataSource(String dataSourceId, DataSource dataSource) {
        AdvDataSourceWrapper existingDataSource = dataSourceMap.get(dataSourceId);
        if (existingDataSource == null) {
            dataSourceMap.put(dataSourceId, AdvDataSourceWrapper.wrap(dataSource));
            log.debug("已添加数据源: {}", dataSourceId);
        } else {
            log.debug("数据源已存在，不执行添加操作: {}", dataSourceId);
        }
    }

    /**
     * 根据配置对象创建并包装数据源。
     *
     * <p>实际创建工作委托给 {@link IAdvDataSourceInitHelper#getDbDataSourceByApo(DataSourceApo)}，
     * 本方法只负责将结果包装为 {@link AdvDataSourceWrapper}。
     *
     * @param dataSourceApo 数据源配置对象
     * @return 包装后的数据源
     */
    @Override
    public AdvDataSourceWrapper getDataSourceByDataSourceApo(DataSourceApo dataSourceApo) {
        return AdvDataSourceWrapper.wrap(
                getAdvDataSourceInitHelper().getDbDataSourceByApo(dataSourceApo));
    }

    /**
     * 移除指定 ID 的数据源并关闭其连接。
     *
     * @param dataSourceId 数据源标识
     * @return 移除成功返回 {@code true}，数据源不存在返回 {@code false}
     */
    @Override
    public boolean removeDataSource(String dataSourceId) {
        if (containsDataSource(dataSourceId)) {
            AdvDataSourceWrapper dataSource = dataSourceMap.remove(dataSourceId);
            if (dataSource != null) {
                dataSource.close();
                log.info("已移除并关闭数据源: {}", dataSourceId);
                return true;
            }
        }
        log.debug("数据源不存在，移除失败: {}", dataSourceId);
        return false;
    }
}
