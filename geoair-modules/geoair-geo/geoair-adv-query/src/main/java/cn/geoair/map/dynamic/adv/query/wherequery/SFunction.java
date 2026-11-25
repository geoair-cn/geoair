package cn.geoair.map.dynamic.adv.query.wherequery;

import java.io.Serializable;
import java.util.function.Function;

/**
 * 支持序列化的Function接口
 *
 * <p>用于Lambda表达式的字段名提取，需要继承Serializable
 *
 * <p>使用示例：
 *
 * <pre>
 * SFunction&lt;User, String&gt; getName = User::getName;
 * String columnName = LambdaUtils.getColumnName(getName);
 * </pre>
 *
 * @param <T> 实体类型
 * @param <R> 返回值类型
 * @author 张俊
 * @date Created in 2026/5/18
 */
@FunctionalInterface
public interface SFunction<T, R> extends Function<T, R>, Serializable {

    /**
     * 应用函数
     *
     * @param t 实体对象
     * @return 属性值
     */
    @Override
    R apply(T t);
}
