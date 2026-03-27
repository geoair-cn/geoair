package cn.geoair.comp.db.service.core.typehander;

import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;
import java.sql.ResultSet;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description： oracle空间字段的解析
 */
public class BlobTypeHandler extends BaseTypeHandler<String> {

    @Override
    public String getNonNullParameter(Object parameter, JdbcType jdbcType) {
        return null;
    }

    @Override
    public String getResult(Entity entity, String columnName) {

        return String.valueOf("(Blob)");
    }

    @Override
    public String getResult(ResultSet resultSet, String columnName) {
        return String.valueOf("(Blob)");
    }

    @Override
    public String getResult(ResultSet resultSet, Integer columnIndex) {
        return String.valueOf("(Blob)");
    }

    @Override
    public String getResult(Map<String, Object> row, String columnName) {
        return String.valueOf("(Blob)");
    }

    @Override
    public String getResult(Object obj) {
        return String.valueOf("(Blob)");
    }
}
