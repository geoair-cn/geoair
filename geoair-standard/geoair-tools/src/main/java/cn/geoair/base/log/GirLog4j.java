package cn.geoair.base.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 编译期自动生成日志字段的注解，功能与 Lombok 的 {@code @Slf4j} 类似。
 *
 * <p>标注在类上后，编译时由 {@code geoair-log4j-processor} 注解处理器自动向该类注入如下字段：
 *
 * <pre>
 * private static GiLogger log = GirLoggerFactory.getLogger(当前类.class);
 * </pre>
 *
 * 之后即可在类中直接使用 {@code log.info("...")}、{@code log.error("...")} 等日志方法， 无需手写字段，与手写的效果完全一致。
 *
 * <p>使用示例：
 *
 * <pre>
 * &#64;GirLog4j
 * public class DemoService {
 *
 *     public void say() {
 *         log.info("hello {}", "geoair");
 *     }
 * }
 * </pre>
 *
 * <p>约定与限制：
 *
 * <ul>
 *   <li>只能标注在类上（含内部类、枚举），标注在接口、注解、记录等其他类型上编译时报错
 *   <li>类中已存在同名字段时不再注入，避免重复定义
 *   <li>使用方工程需要依赖 {@code geoair-log4j-processor}，并开启注解处理（IDEA：Settings → Build → Compiler →
 *       Annotation Processors → Enable）
 *   <li>JDK 16+ 编译时，需为 javac 添加 {@code --add-opens jdk.compiler/com.sun.tools.javac.*=ALL-UNNAMED}
 *       参数
 * </ul>
 *
 * @author geoair
 * @since J8-dev-SNAPSHOT
 * @see GiLogger
 * @see GirLoggerFactory
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface GirLog4j {

    /**
     * 日志主题名称。
     *
     * <p>为空时自动使用被标注类的全限定类名（等价于手写 {@code GirLoggerFactory.getLogger(当前类.class)}）； 非空时等价于手写 {@code
     * GirLoggerFactory.getLogger("topic")}。
     *
     * @return 日志主题名称，默认空字符串
     */
    String topic() default "";

    /**
     * 生成的日志字段是否带 {@code final} 修饰符。
     *
     * @return {@code true} 生成 {@code private static final GiLogger log}，默认 {@code false}
     */
    boolean useFinal() default false;

    /**
     * 生成的日志字段名。
     *
     * @return 字段名，默认 {@code log}
     */
    String fieldName() default "log";
}
