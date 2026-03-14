package cn.geoair.base.data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import cn.geoair.base.data.model.GiModelable;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.util.GutilReflection;
import cn.geoair.base.Gir;

public interface GiValuable<T> extends Serializable {

	@SuppressWarnings("unchecked")
	public static <T> Class<T> getValueType(Class<? extends GiValuable<T>> cls) {
		return (Class<T>) getValueGenericType(cls);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Type getValueGenericType(Class<? extends GiValuable> valueClass) {

		Type[] genericInterfaces = valueClass.getGenericInterfaces();
		for (Type type : genericInterfaces) {
			if (type instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType) type;
				if (GiValuable.class.isAssignableFrom((Class<?>) pType.getRawType())) {
					Type[] ts = pType.getActualTypeArguments();
					if (ts.length == 1) {
						return ts[0];
					}
				}
			}
			if (type instanceof Class && GiValuable.class.isAssignableFrom((Class<?>) type)) {
				return getValueGenericType((Class<? extends GiValuable>) type);
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	default T value() {

		Class<?> myClass = this.getClass();

		if (myClass.isEnum()) {

			Class<Enum<?>> em = (Class<Enum<?>>) myClass;
			Field[] fs = em.getDeclaredFields();

			int flen = 0;
			Field vField = null;
			for (Field f : fs) {
				if (!Modifier.isStatic(f.getModifiers())) {
					GaModelField mf = f.getAnnotation(GaModelField.class);
					if (mf != null && mf.isID()) {
						try {
							GutilReflection.makeAccessible(f);
							return (T) f.get(this);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}
					flen++;
					if (f.getType() == this.valueType()) {
						vField = f;
					}

				}
			}
			if (flen == 1 && vField != null) {
				try {
					GutilReflection.makeAccessible(vField);
					return (T) vField.get(this);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			else if (flen == 0) {
				Enum<?> e = (Enum<?>) this;
				return (T) e.name();
			}
			else {
				Gir.log.error("枚举 {} 没有标记value键,在值属性上加注解@GiModelField(isID = true)或覆写 value()方法", myClass.getName());
			}

		}
		else {
			Field field = GutilReflection.findField(myClass, GiModelable.idFieldPredicate);
			if (field != null) {
				try {
					GutilReflection.makeAccessible(field);
					return (T) field.get(this);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			Gir.log.error("类  {} 没有标记value键,在值属性上加注解@GiModelField(isID = true)或覆写 value()方法", myClass.getName());
		}
		return null;

	}

	@SuppressWarnings("unchecked")
	default Class<T> valueType() {
		return GiValuable.getValueType((Class<? extends GiValuable<T>>) this.getClass());
	}

}
