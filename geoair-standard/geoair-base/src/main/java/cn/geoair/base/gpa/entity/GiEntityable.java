package cn.geoair.base.gpa.entity;

import cn.geoair.base.data.model.GiModelable;
import cn.geoair.base.exception.GirException;
import cn.geoair.base.util.GutilReflection;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.Predicate;

/**
 * 表模型
 *
 * @author Ray
 * @param <PK> 主键类型
 */
public interface GiEntityable<PK extends Serializable> extends GiModelable<PK> {

    public static final Predicate<Field> pkFieldPredicate =
            s -> {
                Annotation[] ans = s.getAnnotations();
                for (Annotation an : ans) {
                    if ("jakarta.persistence.EmbeddedId".equals(an.annotationType().getName())
                            || "jakarta.persistence.Id".equals(an.annotationType().getName())) {
                        return true;
                    }
                }
                return false;
            };

    @SuppressWarnings("unchecked")
    @Override
    default PK id() {
        Field field = GutilReflection.findField(this.getClass(), pkFieldPredicate);
        if (field != null) {

            if (!idClass().isAssignableFrom(field.getType())) {
                throw new GirException(
                        "实体类 {} idClass() {} 类型与标记@Id的属性类型不匹配，复写id()方法或者idClass()方法",
                        this.getClass().getName(),
                        idClass().getName());
            }
            try {
                GutilReflection.makeAccessible(field);
                return (PK) field.get(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return GiModelable.super.id();
    }

    @SuppressWarnings("unchecked")
    default Class<? extends GiEntityable<PK>> modelClass() {
        return (Class<? extends GiEntityable<PK>>) this.getClass();
    }
}
