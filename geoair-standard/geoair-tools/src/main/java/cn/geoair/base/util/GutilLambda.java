package cn.geoair.base.util;

import cn.geoair.base.lang.lambda.*;
import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Lambda元数据解析工具
 *
 * <p>支持解析实现了{@link Serializable}接口的 lambda 表达式（函数式接口），例如MyBatis-Plus 风格的 方法引用{@code
 * SFunction}。解析优先级如下：
 *
 * <ol>
 *   <li>IDEA调试模式下 lambda 为{@link Proxy}代理对象，走 {@link GkIdeaProxyLambdaMeta}
 *   <li>反射调用 {@code writeReplace} 方法获取{@link SerializedLambda}，走 {@link GkReflectLambdaMeta}
 *   <li>反射失败（例如某些非标准lambda实现）时，降级为序列化方式读取，走 {@link GkShadowLambdaMeta}
 * </ol>
 *
 * @author
 */
public class GutilLambda {

    /**
     * 解析 lambda 表达式的元数据
     *
     * <p>支持的类型：实现了{@link Serializable}接口的 lambda（方法引用或匿名函数式接口实例）， 序列化要求：lambda
     * 捕获的参数（若存在）及其类型必须可序列化，否则降级解析可能失败并抛出 {@link IllegalStateException}（由 {@link
     * GkSerializedLambda#extract(Serializable)} 抛出）。
     *
     * <p>失败行为：反射解析失败（包括lambda未实现{@code writeReplace}方法、安全策略拒绝访问等）时 自动降级为序列化方式解析，不返回{@code null}。
     *
     * @param func 需要解析的 lambda 对象，必须非{@code null}
     * @param <T> 类型，被调用的 Function 对象的目标类型
     * @return 返回解析后的结果
     * @throws IllegalArgumentException {@code func}为{@code null}
     * @throws IllegalStateException lambda无法序列化解析（仅当反射与序列化两条路径都不可用时）
     */
    public static <T> GkfLambdaMeta extract(Serializable func) {
        if (func == null) {
            throw new IllegalArgumentException("Lambda to extract must not be null");
        }
        // 1. IDEA 调试模式下 lambda 表达式是一个代理
        if (func instanceof Proxy) {
            return new GkIdeaProxyLambdaMeta((Proxy) func);
        }
        // 2. 反射读取
        try {
            Method method = func.getClass().getDeclaredMethod("writeReplace");
            return new GkReflectLambdaMeta(
                    (SerializedLambda) GutilReflection.setAccessible(method).invoke(func));
        } catch (ReflectiveOperationException | SecurityException e) {
            // 3. 反射失败使用序列化的方式读取
            return new GkShadowLambdaMeta(GkSerializedLambda.extract(func));
        }
    }
}
