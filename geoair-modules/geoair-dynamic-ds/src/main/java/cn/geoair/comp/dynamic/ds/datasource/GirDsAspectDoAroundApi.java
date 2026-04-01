package cn.geoair.comp.dynamic.ds.datasource;

import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/1/7 13:39 @description： 切面拦截后的处理方法
 */
public interface GirDsAspectDoAroundApi {

    /**
     * 执行代理方法前
     *
     * @param method
     * @param point
     */
    void doBefore(Method method, ProceedingJoinPoint point);

    /**
     * 执行代理方法后
     *
     * @param method
     * @param point
     */
    Object doAfter(Object proceed, Method method, ProceedingJoinPoint point);

    /**
     * 发生异常的时候
     *
     * @param point
     */
    void onError(Exception e, ProceedingJoinPoint point);

    /**
     * 无论成功与否都执行的方法
     *
     * @param method
     * @param point
     */
    void onFinally(Method method, ProceedingJoinPoint point);
}
