package cn.geoair.map.dynamic.dbservice.typehander;

import cn.hutool.db.meta.JdbcType;

import cn.geoair.base.util.GutilObject;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:47
 * @description： TODO
 */
public abstract class BaseTypeHandler<T> implements TypeHandler<T> {
    public T getParameter(Object parameter, JdbcType jdbcType) {
        if (GutilObject.isEmpty(parameter)) {
            return null;
        } else {
            return getNonNullParameter(parameter, jdbcType);
        }
    }

    public abstract T getNonNullParameter(Object parameter, JdbcType jdbcType);
}
