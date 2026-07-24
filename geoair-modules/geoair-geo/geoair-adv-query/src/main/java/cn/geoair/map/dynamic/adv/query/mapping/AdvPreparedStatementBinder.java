package cn.geoair.map.dynamic.adv.query.mapping;

import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： PreparedStatement参数绑定器
 */
public class AdvPreparedStatementBinder {

    private final AdvTypeHandlerRegistry typeHandlerRegistry = AdvTypeHandlerRegistry.getInstance();

    public void bind(PreparedStatement preparedStatement, int index, Object value)
            throws SQLException {
        Object jdbcValue =
                typeHandlerRegistry.convertForWrite(
                        value,
                        value == null ? Object.class : value.getClass(),
                        AdvTypeHandlerContext.simple(null));
        preparedStatement.setObject(index, jdbcValue);
    }

    public void bindAll(PreparedStatement preparedStatement, List<Object> values)
            throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            bind(preparedStatement, i + 1, values.get(i));
        }
    }
}
