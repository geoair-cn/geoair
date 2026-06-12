package cn.geoair.comp.db.service.core.typehander;

import cn.hutool.core.map.MapUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

import java.sql.ResultSet;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description： TODO
 */
public class BooleanTypeHandler extends BaseTypeHandler<Boolean> {

    @Override
    public Boolean getNonNullParameter(Object parameter, JdbcType jdbcType) {
        return Boolean.valueOf(String.valueOf(parameter));
    }

    @Override
    public Boolean getResult(Entity entity, String columnName) {
        return entity.getBool(columnName);
    }

    @Override
    public Boolean getResult(ResultSet resultSet, String columnName) {
        return null;
    }

    @Override
    public Boolean getResult(ResultSet resultSet, Integer columnIndex) {
        return null;
    }

    @Override
    public Boolean getResult(Map<String, Object> row, String columnName) {
        return MapUtil.getBool(row, columnName);
    }

    @Override
    public Boolean getResult(Object obj) {
        return Boolean.valueOf(String.valueOf(obj));
    }
}
