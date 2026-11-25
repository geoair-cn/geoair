package cn.geoair.base.data.model;

import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.exception.GirException;
import cn.geoair.base.util.GutilReflection;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Predicate;

/**
 * 模型
 *
 * @author Ray
 * @param <ID> 主键类型
 */
public interface GiModelable<ID extends Serializable> extends Serializable {

    @SuppressWarnings({"unchecked"})
    public static <M extends GiModelable<ID>, ID extends Serializable> Class<ID> getIDClass(
            Class<M> cls) {
        return (Class<ID>) getModelGenericTypes(cls);
    }

    public static Type getModelGenericTypes(Class<?> modelClass) {

        Type[] genericInterfaces = modelClass.getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) type;

                if (GiModelable.class.isAssignableFrom((Class<?>) pType.getRawType())) {

                    Type[] ts = pType.getActualTypeArguments();

                    if (ts.length == 1) {
                        if (Serializable.class.isAssignableFrom((Class<?>) ts[0])) {
                            return ts[0];
                        }
                    }
                }
            }
            if (type instanceof Class && GiModelable.class.isAssignableFrom((Class<?>) type)) {
                return getModelGenericTypes((Class<?>) type);
            }
        }
        return null;
    }

    public static final Predicate<Field> idFieldPredicate =
            s -> {
                GaModelField mf = s.getDeclaredAnnotation(GaModelField.class);
                return mf != null && mf.isID();
            };

    @SuppressWarnings("unchecked")
    default ID id() {
        Field field = GutilReflection.findField(this.getClass(), idFieldPredicate);
        if (field != null) {

            if (!idClass().isAssignableFrom(field.getType())) {
                throw new GirException(
                        "模型类 {} idClass() {} 类型与标记@GaModelField(isID=true)的属性类型不匹配，复写id()方法或者idClass()方法",
                        this.getClass().getName(),
                        idClass().getName());
            }

            try {
                GutilReflection.makeAccessible(field);
                return (ID) field.get(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        throw new GirException(
                "模型类  {} 没有标记id键,在id属性上加注解@GaModelField(isID = true)或@Id 或 覆写 id()方法",
                this.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    default Class<? extends GiModelable<ID>> modelClass() {
        return (Class<? extends GiModelable<ID>>) this.getClass();
    }

    @SuppressWarnings("unchecked")
    default Class<ID> idClass() {
        return GiModelable.getIDClass(this.getClass());
    }
}
