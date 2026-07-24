package cn.geoair.base.util;

import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GkMethodHand;
import java.lang.reflect.Proxy;

public class GutilAop {

    /**
     * 检查给定对象是否为JDK动态代理或CGLIB代理。
     *
     * <p>此方法除了检查对象是否为代理外，还会额外检查给定对象是否为{@link Proxy}的实例。
     *
     * @param object 要检查的对象
     * @return 如果对象是AOP代理则返回true，否则返回false
     * @see #isJdkDynamicProxy
     * @see #isCglibProxy
     */
    @GaMethodHandDefine(
            expectClassName = "org.springframework.aop.support.AopUtils",
            expectMethodName = "isAopProxy")
    public static boolean isAopProxy(Object object) {
        return (boolean) GkMethodHand.invokeSelf(object);
    }

    /**
     * 检查给定对象是否为JDK动态代理。
     *
     * <p>此方法在{@link Proxy#isProxyClass(Class)}实现的基础上，额外检查给定对象是否为 的实例。
     *
     * @param object 要检查的对象
     * @return 如果对象是JDK动态代理则返回true，否则返回false
     * @see java.lang.reflect.Proxy#isProxyClass
     */
    @GaMethodHandDefine(
            expectClassName = "org.springframework.aop.support.AopUtils",
            expectMethodName = "isJdkDynamicProxy")
    public static boolean isJdkDynamicProxy(Object object) {
        return (boolean) GkMethodHand.invokeSelf(object);
    }

    /**
     * 检查给定对象是否为CGLIB代理。
     *
     * <p>此方法在 实现的基础上，额外检查给定对象是否为 的实例。
     *
     * @param object 要检查的对象
     * @return 如果对象是CGLIB代理则返回true，否则返回false
     */
    @GaMethodHandDefine(
            expectClassName = "org.springframework.aop.support.AopUtils",
            expectMethodName = "isCglibProxy")
    public static boolean isCglibProxy(Object object) {
        return (boolean) GkMethodHand.invokeSelf(object);
    }

    /**
     * 确定给定bean实例的目标类，该实例可能是AOP代理。
     *
     * <p>如果给定实例是AOP代理，则返回目标类；否则返回普通类。
     *
     * @param candidate 要检查的实例（可能是AOP代理）
     * @return 目标类（如果是AOP代理）或给定对象的普通类（作为后备）；永远不会为{@code null}
     */
    @GaMethodHandDefine(
            expectClassName = "org.springframework.aop.support.AopUtils",
            expectMethodName = "getTargetClass")
    public static Class<?> getTargetClass(Object candidate) {
        return (Class<?>) GkMethodHand.invokeSelf(candidate);
    }
}
