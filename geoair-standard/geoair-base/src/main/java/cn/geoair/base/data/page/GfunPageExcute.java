package cn.geoair.base.data.page;

import cn.geoair.base.lang.lambda.GkfLambdaMeta;
import cn.geoair.base.util.GutilClass;
import cn.geoair.base.util.GutilGenericType;
import cn.geoair.base.util.GutilLambda;

import java.io.Serializable;
import java.lang.reflect.Type;

/**
 * 分页执行函数接口
 *
 * <p>定义分页数据查询的执行接口，支持泛型返回类型R
 *
 * @author Ray
 * @param <R> 返回的数据类型
 */
@FunctionalInterface
public interface GfunPageExcute<R> extends Serializable {

    /**
     * 执行分页查询操作
     *
     * @return 查询结果集合
     */
    Iterable<R> excute();

    /**
     * 获取返回包装对象
     *
     * <p>根据返回类型创建对应的分页包装器
     *
     * @return 分页包装对象
     */
    default GiPager<R> getgtcPager() {
        return GiPager.ofClass(this.getReturnClass());
    }

    /**
     * 获取返回类型
     *
     * <p>通过反射机制解析泛型参数类型，支持Lambda表达式和普通实现类
     *
     * <p>注意：对于Lambda表达式，需要通过工具类提取实际类型信息
     *
     * @return 返回数据的Class类型
     */
    @SuppressWarnings("unchecked")
    default Class<R> getReturnClass() {

        Class<?> myClass = this.getClass();

        // 处理Lambda表达式的情况
        if (myClass.getName().contains(GutilClass.LAMBDA_CLASS_SIGN)) {
            GkfLambdaMeta lm = GutilLambda.extract(this);
            myClass = lm.getInstantiatedClass();
        } else {
            // 获取用户定义的实现类
            myClass = GutilClass.getUserClass(this);
        }

        // 解析泛型参数
        if (GfunPageExcute.class.isAssignableFrom(myClass)) {
            Type[] types = GutilGenericType.resolveTypeArguments(myClass, GfunPageExcute.class);
            if (types != null && types.length > 0) {
                Type type = types[0];
                if (type instanceof Class) {
                    return (Class<R>) type;
                }
            }
        }
        return null;
    }
}
