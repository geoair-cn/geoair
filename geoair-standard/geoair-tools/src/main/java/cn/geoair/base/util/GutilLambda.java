package cn.geoair.base.util;

import cn.geoair.base.lang.lambda.*;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class GutilLambda {

    /**
     * @param func 需要解析的 lambda 对象
     * @param <T> 类型，被调用的 Function 对象的目标类型
     * @return 返回解析后的结果
     */
    public static <T> GkfLambdaMeta extract(Serializable func) {
        // 1. IDEA 调试模式下 lambda 表达式是一个代理
        if (func instanceof Proxy) {
            return new GkIdeaProxyLambdaMeta((Proxy) func);
        }
        // 2. 反射读取
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            return new GkReflectLambdaMeta(
                    (SerializedLambda) GutilReflection.setAccessible(method).invoke(func));
        } catch (Throwable e) {
            // 3. 反射失败使用序列化的方式读取
            return new GkShadowLambdaMeta(GkSerializedLambda.extract(func));
        }
    }

    /**
     * @param func 需要解析的 lambda 对象
     * @param <T> 类型，被调用的 Function 对象的目标类型
     * @return 返回解析后的结果
     *     <p>public static <T> LambdaMeta extract(Object lambdaObj) { // 1. IDEA 调试模式下 lambda
     *     表达式是一个代理 if (lambdaObj instanceof Proxy) { return new IdeaProxyLambdaMeta((Proxy)
     *     lambdaObj); } // 2. 反射读取 try { Method method =
     *     lambdaObj.getClass().getDeclaredMethod("writeReplace"); return new
     *     ReflectLambdaMeta((SerializedLambda)
     *     gtcReflectionUtil.setAccessible(method).invoke(lambdaObj)); } catch (Throwable e) { // 3.
     *     反射失败使用序列化的方式读取 if(lambdaObj instanceof Serializable) { return new ShadowLambdaMeta(
     *     cn.geoair..base.lang.lambda.SerializedLambda.extract((Serializable)lambdaObj)); }else {
     *     return null; } } }
     */
}
