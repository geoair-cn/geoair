package cn.geoair.map.dynamic.adv.query.mapping;

import cn.hutool.db.handler.RsHandler;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： 单条bean结果处理器
 */
public class AdvBeanHandler<T> implements RsHandler<T> {

    private final Class<T> beanType;
    private final AdvBeanMapper beanMapper = new AdvBeanMapper();

    public AdvBeanHandler(Class<T> beanType) {
        this.beanType = beanType;
    }

    @Override
    public T handle(ResultSet rs) throws SQLException {
        if (rs.next()) {
            return beanMapper.mapRow(rs, beanType);
        }
        return null;
    }
}
