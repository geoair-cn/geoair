package cn.geoair.orm.mybatis.impls;

import cn.geoair.base.data.model.GiModelable;
import cn.geoair.base.gpa.dao.GiDao;
import cn.geoair.base.gpa.entity.GiEntityable;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface MyBatisMapper<T extends GiEntityable<PK>, PK extends Serializable>
        extends GiDao<T, PK> {

    @SuppressWarnings("unchecked")
    default Class<T> getModelClass() {
        Class<T> modelClass = null;
        Class<?>[] infs = this.getClass().getInterfaces();
        loop:
        for (Class<?> cls : infs) {
            if (MyBatisMapper.class.isAssignableFrom(cls)) {
                Type[] types = cls.getGenericInterfaces();
                for (Type type : types) {
                    if (type != null && type instanceof ParameterizedType) {
                        ParameterizedType pType = (ParameterizedType) type;
                        Type[] tps = pType.getActualTypeArguments();
                        if (tps.length > 0) {
                            if (GiModelable.class.isAssignableFrom((Class<?>) tps[0])) {
                                modelClass = (Class<T>) tps[0];
                                break loop;
                            }
                        }
                    }
                }
            }
        }
        return modelClass;
    }
}
