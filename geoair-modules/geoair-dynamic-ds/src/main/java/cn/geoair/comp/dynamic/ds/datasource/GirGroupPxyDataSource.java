package cn.geoair.comp.dynamic.ds.datasource;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.AdvDynamicDataSourceStorage;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.db.ds.simple.AbstractDataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2025/1/2 18:31
 * @description： 一组多个数据源的组合
 */
public class GirGroupPxyDataSource extends AbstractDataSource {

    protected final GiLogger log = GirLogger.getLoger(GirGroupPxyDataSource.class);

    String groupName;

    List<String> dataSourceIds = new ArrayList<>();

    /**
     * @param groupName 组名称
     * @param dataSourceIds 这个组对应有哪些数据源，这里传入数据源ID
     */
    public GirGroupPxyDataSource(String groupName, List<String> dataSourceIds) {
        this.groupName = groupName;
        this.dataSourceIds = dataSourceIds;
    }

    public String getUrl() {
        int i = RandomUtil.randomInt(dataSourceIds.size());
        String dsId = dataSourceIds.get(i);
        return AdvDynamicDataSourceStorage.getInstance().getDataSource(dsId).getJdbcUrl();
    }

    @Override
    public Connection getConnection() throws SQLException {
        int i = RandomUtil.randomInt(dataSourceIds.size());
        String dsId = dataSourceIds.get(i);
        return AdvDynamicDataSourceStorage.getInstance().getDataSource(dsId).getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        int i = RandomUtil.randomInt(dataSourceIds.size());
        String dsId = dataSourceIds.get(i);
        return AdvDynamicDataSourceStorage.getInstance()
                .getDataSource(dsId)
                .getConnection(username, password);
    }

    @Override
    public void close() throws IOException {
        // 暂不支持
    }
}
