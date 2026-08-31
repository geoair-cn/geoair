package cn.geoair.map.dynamic.adv.query.typehandler;

import cn.geoair.base.sp.GirSpHelper;
import cn.geoair.map.dynamic.adv.query.typehandler.impl.*;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.db.dialect.DialectName;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 高级查询类型处理器注册表
 *     <p>每个数据库方言执行器拥有独立的 Registry 实例，通过 {@link #create(DialectName, List)} 工厂方法创建。 Registry 自动加载
 *     SPI 公共处理器 + 全局注册处理器 + 用户自定义处理器 + 方言专属 Geometry 处理器
 *     <p><b>全局注册（兼容旧 API）</b>
 *     <pre>{@code
 * AdvTypeHandlerRegistry.getInstance().register(new MyHandler());
 * }</pre>
 *     全局注册的 handler 会被所有后续创建的 Executor Registry 继承，优先级高于 SPI 但低于 per-executor 自定义。
 *     <p><b>按 Executor 注册</b>
 *     <pre>{@code
 * AdvQueryGlobalConfig.of().addTypeHandler(new MyHandler());
 * }</pre>
 *     仅对当前 Executor 生效，优先级高于全局注册。
 */
public class AdvTypeHandlerRegistry {

    /** 全局共享 Registry，用于 SPI 之外的统一注入入口 */
    private static final AdvTypeHandlerRegistry GLOBAL = new AdvTypeHandlerRegistry();

    private final List<AdvTypeHandler<?>> handlers = new CopyOnWriteArrayList<>();

    private final AdvTypeHandler<Object> defaultHandler = new ObjectAdvTypeHandler();

    /**
     * 获取全局 Registry 实例，兼容旧 API。 通过 {@code getInstance().register(handler)} 注册的处理器会被所有 后续通过 {@link
     * #create} 创建的 Executor Registry 继承。
     */
    public static AdvTypeHandlerRegistry getInstance() {
        return GLOBAL;
    }

    /**
     * 创建指定方言的执行器专属 TypeHandlerRegistry
     *
     * <p>加载优先级（从低到高）：
     *
     * <ol>
     *   <li>SPI 公共 handlers（Boolean, Number, Temporal, ByteArray, Character, Enum）
     *   <li>全局 handlers（通过 {@link #getInstance()}.register() 注册）
     *   <li>用户自定义 handlers（来自 AdvQueryGlobalConfig.typeHandlers）
     *   <li>方言专属 Geometry handler（优先级最高）
     * </ol>
     *
     * @param dialect 数据库方言
     * @param customHandlers 用户自定义类型处理器（可为 null），优先级高于全局和 SPI
     */
    public static AdvTypeHandlerRegistry create(
            DialectName dialect, List<AdvTypeHandler<?>> customHandlers) {

        AdvTypeHandlerRegistry registry = new AdvTypeHandlerRegistry();

        // 1. 加载 SPI 公共 handlers（优先级最低）
        List<AdvTypeHandler> spiHandlers = GirSpHelper.loadAll(AdvTypeHandler.class);
        for (AdvTypeHandler handler : spiHandlers) {
            registry.registerLast(handler);
        }

        // 2. 合并全局 handlers（通过 getInstance().register() 注册的）
        for (AdvTypeHandler<?> handler : GLOBAL.handlers) {
            registry.register(handler);
        }

        // 3. 注册用户自定义 handlers（来自 AdvQueryGlobalConfig，优先级高于全局）
        if (customHandlers != null) {
            for (AdvTypeHandler<?> handler : customHandlers) {
                registry.register(handler);
            }
        }

        // 4. 注册方言专属 Geometry handler（优先级最高）
        AdvTypeHandler<?> geometryHandler = createGeometryHandler(dialect);
        registry.register(geometryHandler);

        return registry;
    }

    /** 根据方言创建对应的 Geometry 类型处理器 */
    private static AdvTypeHandler<?> createGeometryHandler(DialectName dialect) {
        if (dialect == null) {
            return new WktGeometryAdvTypeHandler();
        }
        switch (dialect) {
            case POSTGRESQL:
                return new PostGisGeometryAdvTypeHandler();
            case MYSQL:
                return new MysqlGeometryAdvTypeHandler();
            case ORACLE:
                return new OracleGeometryAdvTypeHandler();
            case DM:
                return new DmGeometryAdvTypeHandler();
            default:
                return new WktGeometryAdvTypeHandler();
        }
    }

    // ==================== 实例方法 ====================

    /**
     * 获取一个 SPI-only 的默认 Registry（不包含方言 Geometry handler）， 用于非 Executor 上下文中的向后兼容场景，如
     * BeanToQueryFilterConverter 等工具类。 如需方言支持，请使用 {@link #create(DialectName, List)}。
     */
    public static AdvTypeHandlerRegistry defaultInstance() {
        return create(null, null);
    }

    public List<AdvTypeHandler<?>> getHandlers() {
        return ListUtil.unmodifiable(handlers);
    }

    /** 注册处理器到列表头部，优先级最高 */
    public void register(AdvTypeHandler<?> handler) {
        if (handler != null) {
            handlers.add(0, handler);
        }
    }

    /** 注册处理器到列表尾部，优先级最低 */
    public void registerLast(AdvTypeHandler<?> handler) {
        if (handler != null) {
            handlers.add(handler);
        }
    }

    public Object convertForRead(Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        AdvTypeHandler handler = resolve(javaType, value);
        return handler.convertForRead(value, javaType, context);
    }

    public Object convertForWrite(Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        AdvTypeHandler handler = resolve(javaType, value);
        return handler.convertForWrite(value, javaType, context);
    }

    private AdvTypeHandler<?> resolve(Class<?> javaType, Object value) {
        for (AdvTypeHandler<?> handler : handlers) {
            if (handler.supports(javaType, value)) {
                return handler;
            }
        }
        return defaultHandler;
    }

    /** 获取值的 SQL 占位符表达式。 */
    public SqlPlaceholder getSqlPlaceholder(Object value) {
        if (value == null) return null;
        AdvTypeHandler<?> handler = resolve(value.getClass(), value);
        if (handler != null) {
            return handler.getSqlPlaceholder(value);
        }
        return null;
    }
}
