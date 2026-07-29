package cn.geoair.map.dynamic.adv.query.typehandler;

import cn.geoair.base.sp.GirSpHelper;
import cn.geoair.map.dynamic.adv.query.typehandler.impl.*;
import cn.hutool.core.collection.ListUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 高级查询类型处理器注册表
 */
public class AdvTypeHandlerRegistry {

    private static final AdvTypeHandlerRegistry INSTANCE = new AdvTypeHandlerRegistry();

    private final List<AdvTypeHandler<?>> handlers = new CopyOnWriteArrayList<>();

    private final AdvTypeHandler<Object> defaultHandler = new ObjectAdvTypeHandler();


    private AdvTypeHandlerRegistry() {
        List<AdvTypeHandler> advTypeHandlers = GirSpHelper.loadAll(AdvTypeHandler.class);
        for (AdvTypeHandler handler : advTypeHandlers) {
            register(handler);
        }
    }

    public List<AdvTypeHandler<?>> getHandlers() {
        return ListUtil.unmodifiable(handlers);
    }

    public static AdvTypeHandlerRegistry getInstance() {
        return INSTANCE;
    }

    public void register(AdvTypeHandler<?> handler) {
        if (handler != null) {
            handlers.add(0, handler);
        }
    }


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

    public static void main(String[] args) {
        AdvTypeHandlerRegistry.getInstance().getHandlers().forEach(System.out::println);
    }

}
