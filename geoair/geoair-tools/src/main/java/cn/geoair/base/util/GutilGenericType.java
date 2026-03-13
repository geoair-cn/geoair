package cn.geoair.base.util;

import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GaMethodHandImpl;
import cn.geoair.base.lang.invoke.GaMethodHandImpl.ImplType;
import cn.geoair.base.lang.invoke.GkMethodHand;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class GutilGenericType {

	static {
		GkMethodHand.implFromClass(GutilGenericType.class);
	}

	/**
	 * 获取类的泛型
	 * @param clazz 目标class
	 * @param forClass 设置泛型的类或接口
	 * @return 泛型数组 找不到为null
	 */
	@GaMethodHandDefine(expectClassName = "cn.geoair.spi.util.GenericTypeUtil4Gir",
			expectMethodName = "resolveTypeArguments")
	public static Type[] resolveTypeArguments(final Class<?> clazz, final Class<?> genericIfc) {
		return (Type[]) GkMethodHand.invokeSelf(clazz, genericIfc);
	}

	/**
	 * 获取类的泛型
	 * @param clazz 目标class
	 * @param forClass 设置泛型的类或接口
	 * @return 泛型数组 找不到为null
	 */
	@GaMethodHandImpl(implClass = GutilGenericType.class, implMethod = "resolveTypeArguments", type = ImplType.comity)
	private static Type[] _resolveTypeArguments(Class<?> clazz, Class<?> forClass) {
		if (forClass.isInterface()) {
			Type[] genericInterfaces = clazz.getGenericInterfaces();
			if (genericInterfaces.length > 0) {
				for (Type type : genericInterfaces) {
					if (type instanceof ParameterizedType) {
						ParameterizedType pType = (ParameterizedType) type;

						// Class<?> pc = pType.getClass();

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
			if (type != null) {
				return _resolveTypeArguments((Class<?>) type, forClass);
			}
		}
		else {
			Type type = clazz.getGenericSuperclass();
			if (type instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType) type;
				if (pType.getRawType() == forClass) {
					return pType.getActualTypeArguments();
				}
			}
			if (type instanceof Class) {
				return _resolveTypeArguments((Class<?>) type, forClass);
			}
		}
		return null;
	}

}
