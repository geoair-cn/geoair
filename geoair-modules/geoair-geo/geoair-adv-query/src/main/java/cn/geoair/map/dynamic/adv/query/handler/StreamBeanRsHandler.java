package cn.geoair.map.dynamic.adv.query.handler;

import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanMapper;
import cn.hutool.db.handler.RsHandler;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/11/11 19:57 @description： 流式消费结果的处理器
 */
public class StreamBeanRsHandler<T> implements RsHandler<Integer> {

    Consumer<T> rowConsumer;

    private final Class<T> elementBeanType;

    private final AdvBeanMapper beanMapper;

    public StreamBeanRsHandler(Consumer<T> rowConsumer, Class<T> elementBeanType, AdvBeanMapper beanMapper) {
        this.rowConsumer = rowConsumer;
        this.elementBeanType = elementBeanType;
        this.beanMapper = beanMapper;
    }

    @Override
    public Integer handle(ResultSet rs) throws SQLException {
        final ResultSetMetaData meta = rs.getMetaData();
        final int columnCount = meta.getColumnCount();
        while (rs.next()) {
            doAccept(meta, columnCount, rs);
        }
        return 1;
    }

    void doAccept(ResultSetMetaData meta, int columnCount, ResultSet rs) throws SQLException {
        T t = beanMapper.mapRow(rs, this.elementBeanType);
        rowConsumer.accept(t);
    }
}
