package cn.geoair.comp.dynamic.ds.datasource;

import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/1/7 13:39 @description： 切面拦截后的处理方法
 */
public interface GirDsAspectDoAroundApi {


    String getDataSourceKey(String groupName, GirDataSourceRwTypeEnum girDataSourceRwTypeEnum);

    /**
     * 执行代理方法前
     *
     * @param method
     * @param point
     */
    default void doBefore(Method method, ProceedingJoinPoint point) {
        GirDsDataSource annotation =
                method.getDeclaringClass().getAnnotation(GirDsDataSource.class);
        if (annotation == null) {
            annotation = method.getAnnotation(GirDsDataSource.class);
        }
        if (annotation != null) {
            String dataSourceKey = getDataSourceKey(annotation.groupName(), annotation.rwType());
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
