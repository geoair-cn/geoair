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
 * <p>每个数据库方言执行器拥有独立的 Registry 实例，通过 {@link #create(DialectName, List)} 工厂方法创建。
 * Registry 自动加载 SPI 公共处理器 + 用户自定义处理器 + 方言专属 Geometry 处理器</p>
 */
public class AdvTypeHandlerRegistry {

    private final List<AdvTypeHandler<?>> handlers = new CopyOnWriteArrayList<>();

    private final AdvTypeHandler<Object> defaultHandler = new ObjectAdvTypeHandler();

    /**
     * 创建指定方言的 TypeHandlerRegistry
     *
     * @param dialect        数据库方言
     * @param customHandlers 用户自定义类型处理器（可为 null），优先级高于 SPI 默认处理器
     */
    public static AdvTypeHandlerRegistry create(
            DialectName dialect,
            List<AdvTypeHandler<?>> customHandlers) {

        AdvTypeHandlerRegistry registry = new AdvTypeHandlerRegistry();

        // 1. 加载 SPI 公共 handlers（Boolean, Number, Temporal, ByteArray, Character, Enum）
        //    使用 registerLast 追加到尾部，后续注册的 handler 优先级更高
        List<AdvTypeHandler> spiHandlers = GirSpHelper.loadAll(AdvTypeHandler.class);
        for (AdvTypeHandler handler : spiHandlers) {
            registry.registerLast(handler);
        }

        // 2. 注册用户自定义 handlers（优先级高于 SPI）
        if (customHandlers != null) {
            for (AdvTypeHandler<?> handler : customHandlers) {
                registry.register(handler);
            }
        }

        // 3. 注册方言专属 Geometry handler（优先级最高）
        AdvTypeHandler<?> geometryHandler = createGeometryHandler(dialect);
        if (geometryHandler != null) {
            registry.register(geometryHandler);
        }

        return registry;
    }

    /**
     * 根据方言创建对应的 Geometry 类型处理器
     */
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
            default:
                return new WktGeometryAdvTypeHandler();
        }
    }

    // ==================== 实例方法 ====================

    /**
     * 获取一个 SPI-only 的默认 Registry（不包含方言 Geometry handler），
     * 用于非 Executor 上下文中的向后兼容场景，如 BeanToQueryFilterConverter 等工具类。
     * 如需方言支持，请使用 {@link #create(DialectName, List)}。
     */
    public static AdvTypeHandlerRegistry defaultInstance() {
        return create(null, null);
    }

    public List<AdvTypeHandler<?>> getHandlers() {
        return ListUtil.unmodifiable(handlers);
    }

    /**
     * 注册处理器到列表头部，优先级最高
     */
    public void register(AdvTypeHandler<?> handler) {
        if (handler != null) {
            handlers.add(0, handler);
        }
    }

    /**
     * 注册处理器到列表尾部，优先级最低
     */
    public void registerLast(AdvTypeHandler<?> handler) {
        if (handler != null) {
            handlers.add(handler);
        }
    }

    public Object convertForRead(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
        AdvTypeHandler handler = resolve(javaType, value);
        return handler.convertForRead(value, javaType, context);
    }

    public Object convertForWrite(
            Object value, Class<?> javaType, AdvTypeHandlerContext context) {
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
}
