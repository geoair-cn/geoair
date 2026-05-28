package cn.geoair.comp.dynamic.ds.datasource;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.AdvDynamicDataSourceStorage;
import cn.geoair.comp.dynamic.ds.datasource.enums.LoadStrategyType;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.db.ds.simple.AbstractDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2025/1/2 18:31
 * @description： 数据源组（ 用于负载均衡）
 */
public class GirGroupPxyDataSource extends AbstractDataSource {

    protected final GiLogger log = GirLogger.getLoger(GirGroupPxyDataSource.class);

    /**
     * 组名
     */
    private String groupName;
    /**
     * 该组下对应的数据源Id
     */
    private List<String> dataSourceIds;

    /**
     * 负载策略类型
     */
    private LoadStrategyType strategyType;

    /**
     * 轮询计数器
     */
    private AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /**
     * 权重配置（dataSourceId -> weight）
     */
    private Map<String, Integer> weightMap = new ConcurrentHashMap<>();

    /**
     * 总权重值
     */
    private int totalWeight = 0;

    /**
     * 构造方法（默认随机策略）
     */
    public GirGroupPxyDataSource(String groupName, List<String> dataSourceIds) {
        this(groupName, dataSourceIds, LoadStrategyType.RANDOM);
    }

    /**
     * 构造方法（指定负载策略）
     */
    public GirGroupPxyDataSource(String groupName, List<String> dataSourceIds, LoadStrategyType strategyType) {
        this.groupName = groupName;
        this.dataSourceIds = dataSourceIds;
        this.strategyType = strategyType;

        // 初始化权重（默认每个数据源权重为1）
        for (String dsId : dataSourceIds) {
            weightMap.put(dsId, 1);
            totalWeight += 1;
        }
    }

    /**
     * 设置权重（仅在权重策略下生效）
     */
    public void setWeight(String dataSourceId, int weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("权重必须大于0");
        }
        Integer oldWeight = weightMap.get(dataSourceId);
        if (oldWeight != null) {
            totalWeight -= oldWeight;
        }
        weightMap.put(dataSourceId, weight);
        totalWeight += weight;
        log.info("Group [{}] 设置数据源 [{}] 权重为: {}", groupName, dataSourceId, weight);
    }

    /**
     * 根据负载策略选择数据源ID
     */
    private String selectDataSourceId() {
        if (dataSourceIds.isEmpty()) {
            throw new IllegalStateException("读库数据源列表为空");
        }

        switch (strategyType) {
            case ROUND_ROBIN:
                return roundRobinSelect();
            case WEIGHT:
                return weightSelect();
            case LEAST_ACTIVE:
                return leastActiveSelect();
            case RANDOM:
            default:
                return randomSelect();
        }
    }

    /**
     * 随机策略
     */
    private String randomSelect() {
        int i = RandomUtil.randomInt(dataSourceIds.size());
        return dataSourceIds.get(i);
    }

    /**
     * 轮询策略
     */
    private String roundRobinSelect() {
        int index = roundRobinCounter.getAndIncrement() % dataSourceIds.size();
        return dataSourceIds.get(index);
    }

    /**
     * 权重策略
     */
    private String weightSelect() {
        int randomWeight = RandomUtil.randomInt(totalWeight);
        int currentWeight = 0;
        for (Map.Entry<String, Integer> entry : weightMap.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }
        return dataSourceIds.get(0);
    }

    /**
     * 最少连接数策略（优先选择当前活跃连接最少的从库）
     */
    private String leastActiveSelect() {
        String selectedId = null;
        int minActiveCount = Integer.MAX_VALUE;

        for (String dsId : dataSourceIds) {
            AdvDataSourceWrapper dataSource = AdvDynamicDataSourceStorage.getInstance().getDataSource(dsId);
            Integer activeCount = dataSource.getActiveCount();
            if (activeCount != null) {
                if (activeCount < minActiveCount) {
                    minActiveCount = activeCount;
                    selectedId = dsId;
                }
            } else {
                log.warn("数据源 [{}] 无法获取到现在连接数，最少连接数策略降级为随机策略", dsId);
                return randomSelect();
            }
        }
        return selectedId != null ? selectedId : randomSelect();
    }

    public String getUrl() {
        String dsId = selectDataSourceId();
        return AdvDynamicDataSourceStorage.getInstance().getDataSource(dsId).getJdbcUrl();
    }

    @Override
    public Connection getConnection() throws SQLException {
        String dsId = selectDataSourceId();
        long startTime = System.currentTimeMillis();
        Connection conn = AdvDynamicDataSourceStorage.getInstance().getDataSource(dsId).getConnection();
        long cost = System.currentTimeMillis() - startTime;

        if (cost > 100) {
            log.warn("Group [{}] 策略 [{}] 选择数据源 [{}] 获取连接耗时: {}ms",
                    groupName, strategyType.getDescription(), dsId, cost);
        } else {
            log.debug("Group [{}] 策略 [{}] 选择数据源: {}", groupName, strategyType.getDescription(), dsId);
        }

        return conn;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        String dsId = selectDataSourceId();
        return AdvDynamicDataSourceStorage.getInstance().getDataSource(dsId).getConnection(username, password);
    }

    /**
     * 动态切换负载策略
     */
    public void setStrategyType(LoadStrategyType strategyType) {
        this.strategyType = strategyType;
        log.info("Group [{}] 负载策略切换为: {}", groupName, strategyType.getDescription());
    }

    public LoadStrategyType getStrategyType() {
        return strategyType;
    }

    /**
     * 获取当前数据源组的大小
     */
    public int size() {
        return dataSourceIds.size();
    }

    /**
     * 获取所有数据源ID
     */
    public List<String> getDataSourceIds() {
        return java.util.Collections.unmodifiableList(dataSourceIds);
    }

    @Override
    public void close() throws IOException {
        //  数据源组关闭时不释放数据源，由外部统一管理
        log.debug("Group [{}] close called, but no action taken", groupName);
    }
}
