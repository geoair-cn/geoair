package cn.geoair.comp.dynamic.ds.datasource;

import cn.geoair.base.bean.GirBeanHelper;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.GirDsAspectDoAroundApiHelper;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;

/**
 * @author ：张俊
 * @date ：Created in 2024/12/31 15:45 @description：
 */
@Slf4j
@Aspect
@Order(-1)
public class GirDynamicDataSourceAspect {

    static GirDsAspectDoAroundApiHelper gtcDsAspectDoAroundApi;

    /**
     * 配置切点：方法上带有我们自定义这个注解的就切 被切到就会执行下面的环绕通知去切换数据源
     *
     * <p>由于事务的原因,由于spring的事务,每次开启事务的时候拿一个链接, 在事务体里面无论多少次数据库操作,都只会使用一个链接.
     */
    // @Pointcut("execution(public * com.gtc.gishubteam.*.servface..*.*(..))")
    // 设置 DataSource 注解的切点表达式

    /** 配置切点：匹配方法上带有GtcDsDataSource注解的方法 */
    //    @Pointcut("@annotation(cn.geoair.comp.dynamic.ds.datasource.GirDsDataSource)")
    //    public void methodPointcut() {
    //    }
    @Pointcut(
            "@annotation(cn.geoair.comp.dynamic.ds.datasource.GirDsDataSource) || @annotation(cn.geoair.comp.dynamic.ds.datasource.GirDataSourceChange)")
    public void methodPointcut() {}

    /** 配置切点：匹配类上带有GtcDsDataSource注解的类中的所有方法 */
    @Pointcut("within(@cn.geoair.comp.dynamic.ds.datasource.GirDsDataSource *)")
    public void classPointcut() {}

    /** 组合切点：匹配方法级别或类级别的注解 */
    @Pointcut("methodPointcut() || classPointcut()")
    public void dataSourcePointcut() {}

    /**
     * 环绕通知：处理数据源切换逻辑
     *
     * @param point 连接点
     * @return 方法执行结果
     * @throws Throwable 异常
     */
    @Around("dataSourcePointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        if (gtcDsAspectDoAroundApi == null) {
            try {
                gtcDsAspectDoAroundApi =
                        GirBeanHelper.getProvider().getBean(GirDsAspectDoAroundApiHelper.class);
            } catch (Exception e) {
                log.error(e.getMessage());
                throw new RuntimeException("无法找到 GirDsAspectDoAroundApi 对应的实现，请调用方进行手动实现");
            }
        }
        Method method = null;
        try {
            // 获取当前被切到的方法
            MethodSignature signature = (MethodSignature) point.getSignature();
            // 获取被切到的方法
            method = signature.getMethod();
            gtcDsAspectDoAroundApi.doBefore(method, point);
            // 执行目标方法
            Object proceed = point.proceed();
            Object o = gtcDsAspectDoAroundApi.doAfter(proceed, method, point);
            if (GutilObject.isEmpty(proceed)) {
                return proceed;
            } else {
                return o;
            }
        } catch (Exception e) {
            gtcDsAspectDoAroundApi.onError(e, point);
            throw e;
        } finally {
            gtcDsAspectDoAroundApi.onFinally(method, point);
        }
    }
}
