package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;

import cn.geoair.map.dynamic.ds.DataSourceGetter;
import cn.geoair.map.dynamic.ds.apo.DataSourceApo;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PostgreSQL动态查询执行器代理工厂（JDK动态代理）
 */
public class AdvExecutorPGFactory {

    // 代理对象映射规则
    private static final Map<String, Class<?>> METHOD_PREFIX_MAP = new HashMap<>();
    static {
        // 初始化方法前缀与代理类的映射
        METHOD_PREFIX_MAP.put("b", PgAdvBaseOpt.class);       // 基础操作
        METHOD_PREFIX_MAP.put("d", PgAdvDDLOpt.class);        // DDL操作
        METHOD_PREFIX_MAP.put("e", PgAdvGeoOpt.class);        // 几何操作（基础）
        METHOD_PREFIX_MAP.put("ePre", PgAdvGeoPreOpt.class);  // 几何操作（带参数）
        METHOD_PREFIX_MAP.put("p", PgAdvSimplePageOpt.class); // 分页操作
        METHOD_PREFIX_MAP.put("tb", PgDialectTableNameUtil.class); // 表名处理
        METHOD_PREFIX_MAP.put("init", DataSourceGetter.class); // 数据源初始化
        METHOD_PREFIX_MAP.put("get", DataSourceGetter.class); // 获取数据源相关
        METHOD_PREFIX_MAP.put("close", DataSourceGetter.class); // 关闭资源
    }

    // 缓存：避免重复创建代理实例
    private static final Map<String, IAdvExecutor> EXECUTOR_CACHE = new ConcurrentHashMap<>();

    /**
     * 创建PostgreSQL执行器实例（动态代理）
     */
    public static IAdvExecutor createExecutor(DataSourceApo dataSourceApo) {
        String jsonString = JSON.toJSONString(dataSourceApo);
        String cacheKey = "pg_" +  SecureUtil.md5(jsonString);
        return EXECUTOR_CACHE.computeIfAbsent(cacheKey, key -> {
            // 1. 创建数据源获取器
            DataSourceGetter dataSourceGetter = new DataSourceGetter();
            dataSourceGetter.initByDataSourceApo(dataSourceApo);

            // 2. 创建代理处理器
            AdvExecutorInvocationHandler handler = new AdvExecutorInvocationHandler(dataSourceGetter);

            // 3. 创建动态代理实例
            return (IAdvExecutor) Proxy.newProxyInstance(
                    IAdvExecutor.class.getClassLoader(),
                    new Class[]{IAdvExecutor.class},
                    handler
            );
        });
    }

    public static IAdvExecutor createExecutor(DataSource dataSource) {
        // 简化实现，实际可根据dataSource生成唯一key
        String cacheKey = "pg_" + dataSource.hashCode();
        return EXECUTOR_CACHE.computeIfAbsent(cacheKey, key -> {
            DataSourceGetter dataSourceGetter = new DataSourceGetter();
            dataSourceGetter.initByDataSource(dataSource);
            AdvExecutorInvocationHandler handler = new AdvExecutorInvocationHandler(dataSourceGetter);
            return (IAdvExecutor) Proxy.newProxyInstance(
                    IAdvExecutor.class.getClassLoader(),
                    new Class[]{IAdvExecutor.class},
                    handler
            );
        });
    }

    public static IAdvExecutor createExecutor(Connection connection) {
        String cacheKey = "pg_" + connection.hashCode();
        return EXECUTOR_CACHE.computeIfAbsent(cacheKey, key -> {
            DataSourceGetter dataSourceGetter = new DataSourceGetter();
            dataSourceGetter.initByConnection(connection);
            AdvExecutorInvocationHandler handler = new AdvExecutorInvocationHandler(dataSourceGetter);
            return (IAdvExecutor) Proxy.newProxyInstance(
                    IAdvExecutor.class.getClassLoader(),
                    new Class[]{IAdvExecutor.class},
                    handler
            );
        });
    }

    /**
     * 动态代理处理器：核心逻辑 - 分发方法调用到对应代理对象
     */
    static class AdvExecutorInvocationHandler implements InvocationHandler {

        private final DataSourceGetter dataSourceGetter;
        private final Map<Class<?>, Object> proxyCache = new ConcurrentHashMap<>();
        private final DialectTableNameProcessor tableNameProcessor = PgDialectTableNameUtil.getInstance();

        public AdvExecutorInvocationHandler(DataSourceGetter dataSourceGetter) {
            this.dataSourceGetter = dataSourceGetter;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 1. 获取方法名，匹配对应的代理类
            String methodName = method.getName();
            Class<?> targetClass = findTargetClass(methodName, args);

            // 2. 获取/创建代理实例
            Object targetInstance = getProxyInstance(targetClass);

            // 3. 调用目标方法
            Method targetMethod = targetClass.getMethod(methodName, method.getParameterTypes());
            return targetMethod.invoke(targetInstance, args);
        }

        /**
         * 根据方法名和参数找到目标代理类
         */
        private Class<?> findTargetClass(String methodName, Object[] args) {
            // 优先匹配前缀
            for (Map.Entry<String, Class<?>> entry : METHOD_PREFIX_MAP.entrySet()) {
                if (methodName.startsWith(entry.getKey())) {
                    // 特殊处理：区分GeoOpt和GeoPreOpt
                    if (entry.getKey().equals("e") && args != null && args.length >= 2
                            && args[1] instanceof Map) {
                        return PgAdvGeoPreOpt.class;
                    }
                    return entry.getValue();
                }
            }
            throw new UnsupportedOperationException("不支持的方法：" + methodName);
        }

        /**
         * 获取代理实例（缓存）
         */
        private Object getProxyInstance(Class<?> clazz) throws Exception {
            return proxyCache.computeIfAbsent(clazz, cls -> {
                try {
                    if (cls == DataSourceGetter.class) {
                        return dataSourceGetter;
                    } else if (cls == PgDialectTableNameUtil.class) {
                        return tableNameProcessor;
                    } else {
                        // 其他代理类需要传入AdvExecutor上下文（这里简化为传入dataSourceGetter）
                        return cls.getConstructor(DataSourceGetter.class).newInstance(dataSourceGetter);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("创建代理实例失败：" + cls.getName(), e);
                }
            });
        }
    }
}
