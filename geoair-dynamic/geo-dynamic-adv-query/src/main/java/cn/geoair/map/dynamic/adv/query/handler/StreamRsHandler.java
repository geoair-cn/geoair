package cn.geoair.map.dynamic.adv.query.handler;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.HandleHelper;
import cn.hutool.db.handler.RsHandler;


import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/11/11 19:57
 * @description： 流式消费结果的处理器
 */
public class StreamRsHandler implements RsHandler<Integer> {

    Consumer<GirAdvOneRow> rowConsumer;
    /**
     * 是否大小写不敏感
     */
    private final boolean caseInsensitive;

    public StreamRsHandler(Consumer<GirAdvOneRow> rowConsumer) {
        this.rowConsumer = rowConsumer;
        this.caseInsensitive = false;
    }

    public StreamRsHandler(Consumer<GirAdvOneRow> rowConsumer, boolean caseInsensitive) {
        this.rowConsumer = rowConsumer;
        this.caseInsensitive = caseInsensitive;
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
        Entity entity = HandleHelper.handleRow(columnCount, meta, rs, this.caseInsensitive);
        GirAdvOneRow girAdvOneRow = GirAdvOneRow.ofByEntity(entity);
        rowConsumer.accept(girAdvOneRow);
    }

}
