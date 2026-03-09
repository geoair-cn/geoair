package cn.geoair.map.dynamic.dbservice.typehander;

import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

import java.sql.ResultSet;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:42
 * @description： 类型转换器
 */
public interface TypeHandler<T> {

    /**
     * 转换sql进入的时候的参数
     *
     * @param parameter
     * @param jdbcType
     * @return
     */
    T getParameter(Object parameter, JdbcType jdbcType);

    T getResult(Entity entity, String columnName);

    T getResult(ResultSet resultSet, String columnName);

    T getResult(ResultSet resultSet, Integer columnIndex);

    T getResult(Map<String, Object> row, String columnName);

    T getResult(Object obj);
}
