package cn.geoair.base.data.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.function.Predicate;

import cn.geoair.base.Gir;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.util.GutilReflection;

/**
 * 树形模型
 *
 * @author Ray
 * @param <ID> 主键类型
 */
public interface GiTreeModelable<ID extends Serializable> extends GiModelable<ID> {

	public static final Predicate<Field> parentIdFieldPredicate = s -> {
		GaModelField mf = s.getAnnotation(GaModelField.class);
		return mf != null && mf.isParentId();
	};

	@SuppressWarnings("unchecked")
	default ID parentId() {
		Field field = GutilReflection.findField(this.getClass(), parentIdFieldPredicate);
		if (field != null) {
			GutilReflection.makeAccessible(field);
			try {
				return (ID) field.get(this);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		Gir.log.error("树形模型类  {} 没有标记parentId键,在值属性上加注解@GaModelField(isParentId = true)或覆写 parentId()方法",
				this.getClass().getName());
		return null;
	}

	LinkedList<? extends GiModelable<ID>> children();

}
