package cn.geoair.map.dynamic.adv.query.mapping;

import cn.hutool.db.handler.RsHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： bean列表结果处理器
 */
public class AdvBeanListHandler<T> implements RsHandler<List<T>> {

    private final Class<T> beanType;
    private final AdvBeanMapper beanMapper;

    public AdvBeanListHandler(Class<T> beanType, AdvBeanMapper beanMapper) {
        this.beanType = beanType;
        this.beanMapper = beanMapper;
    }

    @Override
    public List<T> handle(ResultSet rs) throws SQLException {
        return beanMapper.mapList(rs, beanType);
    }
}
