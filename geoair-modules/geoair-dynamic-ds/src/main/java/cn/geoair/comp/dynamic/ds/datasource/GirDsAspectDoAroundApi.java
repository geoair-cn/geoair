package cn.geoair.comp.dynamic.ds.datasource;

import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/1/7 13:39 @description： 切面拦截后的处理方法
 */
public interface GirDsAspectDoAroundApi {


    /**
     * 由于 groupName 和 girDataSourceRwTypeEnum 在生成 GirDynamicStackDataSource的时候，是由客户端进行指定的
     * 这里对于这个注解里面的groupName加上girDataSourceRwTypeEnum的组合，我也并不知道它对应多数据源里面的哪个路由键，所以需要客户端再进行实现一下
     *
     * @param groupName               注解中的组名称
     * @param girDataSourceRwTypeEnum 读写类型
     * @return 指向具体的多数据源的路由键
     */
    String getDataSourceKey(String groupName, GirDataSourceRwTypeEnum girDataSourceRwTypeEnum);

    /**
     * 执行代理方法前
     *
     * @param method
     * @param point
     */
    default void doBefore(Method method, ProceedingJoinPoint point) {
        // 1. 先获取两个注解（类上 + 方法上）
        GirDsDataSource dsAnnotation = method.getDeclaringClass().getAnnotation(GirDsDataSource.class);
        if (dsAnnotation == null) {
            dsAnnotation = method.getAnnotation(GirDsDataSource.class);
        }

        GirDataSourceChange changeAnnotation = method.getDeclaringClass().getAnnotation(GirDataSourceChange.class);
        if (changeAnnotation == null) {
            changeAnnotation = method.getAnnotation(GirDataSourceChange.class);
        }

        String dataSourceKey = null;
        // 2. 判断 GirDsDataSource 注解
        if (dsAnnotation != null) {
            dataSourceKey = getDataSourceKey(dsAnnotation.groupName(), dsAnnotation.rwType());
        }
        // 3. 判断 GirDataSourceChange 注解
        else if (changeAnnotation != null) {
            dataSourceKey = getDataSourceKey(changeAnnotation.groupName(), changeAnnotation.rwType());
        }

        // 4. 推入数据源
        if (dataSourceKey != null) {
            GirDynamicStackDataSource.pushDataSource(dataSourceKey);
        }
    }

    /**
     * 执行代理方法后
     *
     * @param method
     * @param point
     */
    default Object doAfter(Object proceed, Method method, ProceedingJoinPoint point) {
        return proceed;
    }

    /**
     * 发生异常的时候
     *
     * @param point
     */
    default void onError(Exception e, ProceedingJoinPoint point) {

    }

    /**
     * 无论成功与否都执行的方法
     *
     * @param method
     * @param point
     */
    default void onFinally(Method method, ProceedingJoinPoint point) {
        GirDynamicStackDataSource.popDataSource();
    }
}
