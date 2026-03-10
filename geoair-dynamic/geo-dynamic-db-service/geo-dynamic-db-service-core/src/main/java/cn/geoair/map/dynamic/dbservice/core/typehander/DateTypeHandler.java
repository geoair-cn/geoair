package cn.geoair.map.dynamic.dbservice.core.typehander;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.meta.JdbcType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2024/4/18 17:46 @description： 日期
 */
public class DateTypeHandler extends BaseTypeHandler<Date> {

    @Override
    public Date getNonNullParameter(Object parameter, JdbcType jdbcType) {
        return new Date();
    }

    @Override
    public Date getResult(Entity entity, String columnName) {
        return entity.getDate(columnName);
    }

    @Override
    public Date getResult(ResultSet resultSet, String columnName) {
        try {
            Timestamp timestamp = resultSet.getTimestamp(columnName);
            return new Date(timestamp.getTime());
        } catch (SQLException e) {
            try {
                Time time = resultSet.getTime(columnName);
                return new Date(time.getTime());
            } catch (SQLException ex) {
                return null;
            }
        }
    }

    @Override
    public Date getResult(ResultSet resultSet, Integer columnIndex) {
        try {
            Timestamp timestamp = resultSet.getTimestamp(columnIndex);
            return new Date(timestamp.getTime());
        } catch (SQLException e) {
            try {
                Time time = resultSet.getTime(columnIndex);
                return new Date(time.getTime());
            } catch (SQLException ex) {
                return null;
            }
        }
    }

    @Override
    public Date getResult(Map<String, Object> row, String columnName) {
        return MapUtil.getDate(row, columnName);
    }

    @Override
    public Date getResult(Object obj) {
        return Convert.convert(Date.class, obj, null);
    }

    // String dateFormat = "yyyy-MM-dd HH:mm:ss";
    //
    // public String dateFormat(Date date) {
    // if (date == null) {
    // return null;
    // }
    // return DateUtil.format(date, dateFormat);
    // }

}
