package cn.geoair.comp.dynamic.ds.readwrite;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.dswrapper.ConnectionWrapper;
import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.geoair.comp.dynamic.ds.readwrite.log.RdLog;
import cn.geoair.comp.dynamic.ds.readwrite.proxy.ReadWritePxyConnection;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.db.ds.simple.AbstractDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源组
 * 直接持有 DataSource 列表
 *
 * @author 张俊
 * @date Created in 2025/1/2 18:31
 */
public class GirGroupSource extends AbstractDataSource {


    /**
     * 组名
     */
    protected String groupName;

    /**
     * 该组下对应的数据源列表
     */
    protected List<DataSource> dataSources;

    /**
     * 负载策略类型
     */
    protected LoadStrategyType strategyType = LoadStrategyType.RANDOM;

    /**
     * 轮询计数器
     */
    protected AtomicInteger roundRobinCounter = new AtomicInteger(0);

    /**
     * 权重配置（数据源 -> weight）
     */
    protected Map<DataSource, Integer> weightMap = new ConcurrentHashMap<>();

    /**
     * 总权重值
     */
    protected int totalWeight = 0;

    /**
     * 构造方法（默认随机策略）
     */
    public GirGroupSource(String groupName, List<DataSource> dataSources) {
        this(groupName, dataSources, LoadStrategyType.RANDOM);
    }

    /**
     * 构造方法（指定负载策略）
     */
    public GirGroupSource(String groupName, List<DataSource> dataSources, LoadStrategyType strategyType) {
        this.groupName = groupName;
        this.dataSources = dataSources;
        this.strategyType = strategyType;

        // 初始化权重（默认每个数据源权重为1）
        for (DataSource ds : dataSources) {
            weightMap.put(ds, 1);
            totalWeight += 1;
        }

        RdLog.getInstance().trace("初始化数据源组 [{}], 数据源数量: {}, 负载策略: {}",
                groupName, dataSources.size(), strategyType.getDescription());
    }


    protected GirGroupSource(Builder builder) {
        this.groupName = builder.groupName;
        this.strategyType = builder.strategyType;
        this.dataSources = new ArrayList<>();

        // 初始化数据源和权重
        if (!builder.dataSources.isEmpty()) {
            for (DataSource ds : builder.dataSources) {
                this.dataSources.add(ds);
                int weight = builder.weights.getOrDefault(ds, 1);
                this.weightMap.put(ds, weight);
                this.totalWeight += weight;
            }
        }

        if (this.dataSources.isEmpty()) {
            throw new IllegalStateException("数据源组 [" + groupName + "] 数据源列表不能为空");
        }

        RdLog.getInstance().debug("初始化数据源组 [{}], 数据源数量: {}, 负载策略: {}, 总权重: {}",
                groupName, dataSources.size(), strategyType.getDescription(), totalWeight);
    }


    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String groupName;
        private final List<DataSource> dataSources = new ArrayList<>();
        private final Map<DataSource, Integer> weights = new HashMap<>();
        private LoadStrategyType strategyType = LoadStrategyType.RANDOM;

        /**
         * 设置组名
         */
        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        /**
         * 添加数据源
         */
        public Builder addDataSource(DataSource dataSource) {
            this.dataSources.add(dataSource);
            return this;
        }

        /**
         * 添加数据源并指定权重
         */
        public Builder addDataSource(DataSource dataSource, int weight) {
            this.dataSources.add(dataSource);
            this.weights.put(dataSource, weight);
            return this;
        }

        /**
         * 批量添加数据源
         */
        public Builder dataSources(List<DataSource> dataSources) {
            this.dataSources.addAll(dataSources);
            return this;
        }

        /**
         * 批量添加数据源（可变参数）
         */
        public Builder dataSources(DataSource... dataSources) {
            this.dataSources.addAll(Arrays.asList(dataSources));
            return this;
        }

        /**
         * 设置权重（单个）
         */
        public Builder weight(DataSource dataSource, int weight) {
            this.weights.put(dataSource, weight);
            return this;
        }

        /**
         * 设置权重（批量）
         */
        public Builder weights(Map<DataSource, Integer> weights) {
            this.weights.putAll(weights);
            return this;
        }

        /**
         * 设置负载策略
         */
        public Builder strategy(LoadStrategyType strategyType) {
            this.strategyType = strategyType;
            return this;
        }

        /**
         * 设置负载策略（通过名称）
         */
        public Builder strategy(String strategyName) {
            LoadStrategyType strategy = LoadStrategyType.fromCode(strategyName);
            if (strategy != null) {
                this.strategyType = strategy;
            }
            return this;
        }

        /**
         * 构建 GirGroupSource
         */
        public GirGroupSource build() {
            if (groupName == null || groupName.trim().isEmpty()) {
                throw new IllegalStateException("groupName 不能为空");
            }
            if (dataSources.isEmpty()) {
                throw new IllegalStateException("dataSources 不能为空");
            }
            return new GirGroupSource(this);
        }
    }

    // ==================== 权重管理 ====================

    /**
     * 设置权重（仅在权重策略下生效）
     */
    public GirGroupSource setWeight(DataSource dataSource, int weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException("权重必须大于0");
        }
        Integer oldWeight = weightMap.get(dataSource);
        if (oldWeight != null) {
            totalWeight -= oldWeight;
        }
        weightMap.put(dataSource, weight);
        totalWeight += weight;
        RdLog.getInstance().debug("Group [{}] 设置数据源权重为: {}", groupName, weight);
        return this;
    }

    /**
     * 设置权重（通过索引）
     */
    public GirGroupSource setWeight(int index, int weight) {
        if (index < 0 || index >= dataSources.size()) {
            throw new IllegalArgumentException("数据源索引越界: " + index);
        }
        setWeight(dataSources.get(index), weight);
        return this;
    }

    /**
     * 获取数据源的权重
     */
    public int getWeight(DataSource dataSource) {
        return weightMap.getOrDefault(dataSource, 1);
    }

    // ==================== 负载均衡策略 ====================

    /**
     * 选择数据源（基于负载策略）
     */
    protected DataSource selectDataSource() {
        if (dataSources == null || dataSources.isEmpty()) {
            throw new IllegalStateException("数据源组 [" + groupName + "] 数据源列表为空");
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
    protected DataSource randomSelect() {
        int i = RandomUtil.randomInt(dataSources.size());
        return dataSources.get(i);
    }

    /**
     * 轮询策略
     */
    protected DataSource roundRobinSelect() {
        int index = roundRobinCounter.getAndIncrement() % dataSources.size();
        return dataSources.get(index);
    }

    /**
     * 权重策略
     */
    protected DataSource weightSelect() {
        int randomWeight = RandomUtil.randomInt(totalWeight);
        int currentWeight = 0;
        for (Map.Entry<DataSource, Integer> entry : weightMap.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }
        return dataSources.get(0);
    }

    /**
     * 最少连接数策略
     */
    protected DataSource leastActiveSelect() {
        DataSource selected = null;
        int minActiveCount = Integer.MAX_VALUE;

        for (DataSource ds : dataSources) {
            Integer activeCount = getActiveCount(ds);
            if (activeCount != null) {
                if (activeCount < minActiveCount) {
                    minActiveCount = activeCount;
                    selected = ds;
                }
            } else {
                RdLog.getInstance().warn("数据源 [{}] 无法获取活跃连接数，降级为随机策略", getDataSourceInfo(ds));
                return randomSelect();
            }
        }
        return selected != null ? selected : randomSelect();
    }

    /**
     * 获取数据源的活跃连接数
     */
    protected Integer getActiveCount(DataSource dataSource) {

        if (dataSource instanceof AdvDataSourceWrapper) {
            return ((AdvDataSourceWrapper) dataSource).getActiveCount();
        }
        if (dataSource instanceof LazyDataSourceWrapper) {
            return ((LazyDataSourceWrapper) dataSource).getActiveCount();
        }

        // 尝试通过反射获取活跃连接数（支持常见连接池）
        try {
            if (dataSource.getClass().getName().contains("Druid")) {
                return (Integer) dataSource.getClass().getMethod("getActiveCount").invoke(dataSource);
            } else if (dataSource.getClass().getName().contains("Hikari")) {
                Object pool = dataSource.getClass().getMethod("getHikariPoolMXBean").invoke(dataSource);
                return (Integer) pool.getClass().getMethod("getActiveConnections").invoke(pool);
            }
        } catch (Exception e) {
            // 忽略异常
        }

        return null;
    }

    /**
     * 获取数据源信息（用于日志）
     */
    protected String getDataSourceInfo(DataSource ds) {
        if (ds instanceof AdvDataSourceWrapper) {
            return ((AdvDataSourceWrapper) ds).getJdbcUrl();
        }
        if (ds instanceof LazyDataSourceWrapper) {
            return ((LazyDataSourceWrapper) ds).getJdbcUrl();
        }
        return ds.getClass().getSimpleName() + "@" + ds.hashCode();
    }

    // ==================== 公共方法 ====================

    /**
     * 动态切换负载策略
     */
    public GirGroupSource setStrategyType(LoadStrategyType strategyType) {
        this.strategyType = strategyType;
        RdLog.getInstance().debug("Group [{}] 负载策略切换为: {}", groupName, strategyType.getDescription());
        return this;
    }

    public LoadStrategyType getStrategyType() {
        return strategyType;
    }

    /**
     * 获取当前数据源组的大小
     */
    public int size() {
        return dataSources != null ? dataSources.size() : 0;
    }

    /**
     * 获取所有数据源（只读）
     */
    public List<DataSource> getDataSources() {
        return java.util.Collections.unmodifiableList(dataSources);
    }

    /**
     * 获取组名
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * 刷新数据源列表
     */
    public GirGroupSource refreshDataSources(List<DataSource> newDataSources) {
        if (newDataSources == null || newDataSources.isEmpty()) {
            throw new IllegalArgumentException("数据源列表不能为空");
        }

        this.dataSources = newDataSources;

        // 重新初始化权重
        weightMap.clear();
        totalWeight = 0;
        for (DataSource ds : dataSources) {
            weightMap.put(ds, 1);
            totalWeight += 1;
        }

        // 重置轮询计数器
        roundRobinCounter.set(0);

        RdLog.getInstance().debug("Group [{}] 刷新数据源列表, 新数量: {}", groupName, dataSources.size());
        return this;
    }

    /**
     * 添加数据源
     */
    public GirGroupSource addDataSource(DataSource dataSource) {
        addDataSource(dataSource, 1);
        return this;
    }

    /**
     * 添加数据源并指定权重
     */
    public GirGroupSource addDataSource(DataSource dataSource, int weight) {
        if (dataSource == null) {
            throw new IllegalArgumentException("数据源不能为空");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("权重必须大于0");
        }

        this.dataSources.add(dataSource);
        weightMap.put(dataSource, weight);
        totalWeight += weight;

        RdLog.getInstance().debug("Group [{}] 添加数据源, 当前数量: {}", groupName, dataSources.size());
        return this;
    }

    /**
     * 移除数据源
     */
    public boolean removeDataSource(DataSource dataSource) {
        boolean removed = this.dataSources.remove(dataSource);
        if (removed) {
            Integer oldWeight = weightMap.remove(dataSource);
            if (oldWeight != null) {
                totalWeight -= oldWeight;
            }
            RdLog.getInstance().debug("Group [{}] 移除数据源, 当前数量: {}", groupName, dataSources.size());
        }
        return removed;
    }


    @Override
    public Connection getConnection() throws SQLException {
        long startTime = System.currentTimeMillis();
        DataSource ds = selectDataSource();
        Connection conn = ds.getConnection();
        long cost = System.currentTimeMillis() - startTime;

        if (cost > 200) {
            RdLog.getInstance().warn("Group [{}] 策略 [{}] 选择数据源 [{}] 获取连接耗时: {}ms",
                    groupName, strategyType.getDescription(), getDataSourceInfo(ds), cost);
        } else {
            RdLog.getInstance().trace("Group [{}] 策略 [{}] 选择数据源: {}",
                    groupName, strategyType.getDescription(), getDataSourceInfo(ds));
        }
        return new ReadWritePxyConnection(conn, true, ds);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        DataSource ds = selectDataSource();
        Connection connection = ds.getConnection(username, password);
        return new ReadWritePxyConnection(connection, true, ds);

    }

    @Override
    public void close() throws IOException {
        RdLog.getInstance().debug("Group [{}] close called, but no action taken - data sources managed externally", groupName);
    }
}
