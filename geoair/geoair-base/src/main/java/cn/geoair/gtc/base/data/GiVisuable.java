package cn.geoair.gtc.base.data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.util.GutilReflection;
import cn.geoair.gtc.base.Gir;

/**
 * 可显示的数据
 * @author Ray
 *
 */
public interface GiVisuable extends Serializable {


	public static final Predicate<Field> displayFieldPredicate = s -> {
        GaModelField mf = s.getAnnotation(GaModelField.class);return mf != null && mf.isDisplay();};


	@SuppressWarnings("unchecked")
	default String display() {

		Class<?> myClass = this.getClass();

		if(myClass.isEnum()) {
			Class<Enum<?>> em = (Class<Enum<?>>)myClass;
			Field[] fs = em.getDeclaredFields();

			int flen = 0;
			for(Field f:fs) {
				if(!Modifier.isStatic(f.getModifiers())) {
					GaModelField mf = f.getAnnotation(GaModelField.class);
					if(mf != null && mf.isDisplay()) {
						try {
							GutilReflection.makeAccessible(f);
							return String.valueOf(f.get(this));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if(f.getType() == String.class) {
						flen++;
					}
				}
			}
			if(flen == 0) {
				Enum<?> e = (Enum<?>)this;
				return e.name();
			}else {
				 Gir.log.error("枚举 {} 没有标记显示键,在显示属性上加注解@GaModelField(isDisplay = true)或覆写 display()方法", myClass.getName());
			}

		}else {
			Field field = GutilReflection.findField(myClass,displayFieldPredicate);
			if(field != null) {
				try {
					GutilReflection.makeAccessible(field);
					return (String)field.get(this);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			 Gir.log.error("类  {} 没有标记显示键,在值属性上加注解@GaModelField(isDisplay = true)或覆写 display()方法", myClass.getName());
		}
		return null;

	}
}
