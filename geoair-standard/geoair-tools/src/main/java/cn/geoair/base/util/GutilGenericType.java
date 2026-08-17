package cn.geoair.base.util;

import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 泛型类型解析工具类
 *
 * <p>支持通过可插拔的{@link GenericTypeProvider}（例如Spring桥接）解析泛型，<br>
 * 无提供者时使用默认的继承链递归解析。
 */
public class GutilGenericType {

    private static volatile GenericTypeProvider genericTypeProvider;

    /** 泛型类型提供者接口 */
    public interface GenericTypeProvider {
        Type[] resolveTypeArguments(Class<?> clazz, Class<?> genericIfc);
    }

    /**
     * 设置泛型类型提供者
     *
     * @param genericTypeProvider 泛型类型提供者，传{@code null}可清除已设置的提供者
     */
    public static void setGenericTypeProvider(GenericTypeProvider genericTypeProvider) {
        GutilGenericType.genericTypeProvider = genericTypeProvider;
    }

    static {
        GkMethodHand.implFromClass(GutilGenericType.class);
    }

    /**
     * 获取类的泛型
     *
     * <p>优先使用已设置的{@link GenericTypeProvider}解析，否则使用默认继承链递归解析。<br>
     * 注意：返回数组中的元素可能是{@link java.lang.reflect.TypeVariable}或
     * {@link java.lang.reflect.WildcardType}，调用方需自行判断。
     *
     * @param clazz 目标class
     * @param genericIfc 设置泛型的类或接口
     * @return 泛型数组 找不到为null
     */
    @GaMethodHandDefine(
            expectClassName = "cn.geoair.spi.util.SpringGenericTypeBridge",
            expectMethodName = "resolveTypeArguments"
    )
    public static Type[] resolveTypeArguments(final Class<?> clazz, final Class<?> genericIfc) {
        GenericTypeProvider provider = genericTypeProvider;
        if (provider != null) {
            return provider.resolveTypeArguments(clazz, genericIfc);
        }
        return (Type[]) GkMethodHand.invokeSelf(clazz, genericIfc);
    }

    /**
     * 获取类的泛型（默认实现）
     *
     * <p>注意：返回数组中的元素可能是{@link java.lang.reflect.TypeVariable}或
     * {@link java.lang.reflect.WildcardType}，调用方需自行判断。
     *
     * @param clazz 目标class
     * @param forClass 设置泛型的类或接口
     * @return 泛型数组 找不到为null
     * @throws IllegalArgumentException clazz或forClass为{@code null}时抛出
     */
    @GaMethodHandImpl(
            implClass = GutilGenericType.class,
            implMethod = "resolveTypeArguments",
            type = ImplType.comity
    )
    private static Type[] _resolveTypeArguments(Class<?> clazz, Class<?> forClass) {
        if (null == clazz || null == forClass) {
            throw new IllegalArgumentException("clazz and forClass must not be null");
        }
        if (forClass.isInterface()) {
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            if (genericInterfaces.length > 0) {
                for (Type type : genericInterfaces) {
                    if (type instanceof ParameterizedType) {
                        ParameterizedType pType = (ParameterizedType) type;

                        if (pType.getRawType() == forClass) {
                            return pType.getActualTypeArguments();
                        }
                    }
                    if (type instanceof Class) {
                        Type[] res = _resolveTypeArguments((Class<?>) type, forClass);
                        if (res != null && res.length > 0) {
                            return res;
                        }
                    }
                }
            }
            Type type = clazz.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) type;
                if (pType.getRawType() == forClass) {
                    return pType.getActualTypeArguments();
                }
                // 父类泛型不匹配时，沿父类的父类继续递归查找
                Type rawType = pType.getRawType();
                if (rawType instanceof Class) {
                    return _resolveTypeArguments((Class<?>) rawType, forClass);
                }
            } else if (type instanceof Class) {
                return _resolveTypeArguments((Class<?>) type, forClass);
            }
        } else {
            Type type = clazz.getGenericSuperclass();
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) type;
                if (pType.getRawType() == forClass) {
                    return pType.getActualTypeArguments();
                }
                // 当前父类泛型与目标不匹配时，沿父类的父类继续递归查找
                Type rawType = pType.getRawType();
                if (rawType instanceof Class) {
                    return _resolveTypeArguments((Class<?>) rawType, forClass);
                }
            }
            if (type instanceof Class) {
                return _resolveTypeArguments((Class<?>) type, forClass);
            }
        }
        return null;
    }
}
