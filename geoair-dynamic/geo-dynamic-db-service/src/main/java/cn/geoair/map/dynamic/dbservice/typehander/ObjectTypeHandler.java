package cn.geoair.map.dynamic.dbservice.typehander;

import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

import cn.geoair.base.exception.GirException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/19 14:54
 * @description： TODO
 */
public class ObjectTypeHandler extends BaseTypeHandler<Object> {
    @Override
    public Object getNonNullParameter(Object parameter, JdbcType jdbcType) {
        return parameter;
    }

    @Override
    public Object getResult(Entity entity, String columnName) {
        return entity.get(columnName);
    }

    @Override
    public Object getResult(ResultSet resultSet, String columnName) {
        Object object = null;
        try {
            object = resultSet.getObject(columnName);
        } catch (SQLException throwables) {
            throw new GirException("无法找到字段名称为《{}》的字段", columnName);
        }
        return object;
    }

    @Override
    public Object getResult(ResultSet resultSet, Integer columnIndex) {
        Object object = null;
        try {
            object = resultSet.getObject(columnIndex);
        } catch (SQLException throwables) {
            throw new GirException("无法找到字段名称为《{}》的字段", columnIndex);
        }
        return object;
    }

    @Override
    public Object getResult(Map<String, Object> row, String columnName) {
        return row.get(columnName);
    }

    @Override
    public Object getResult(Object obj) {
        return obj;
    }
}
