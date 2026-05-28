//package cn.geoair.comp.dynamic.ds.readwrite;
//
//import cn.geoair.base.log.GiLogger;
//import cn.geoair.base.log.GirLogger;
//import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
//import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
//import cn.hutool.core.util.RandomUtil;
//import cn.hutool.db.ds.simple.AbstractDataSource;
//
//import javax.sql.DataSource;
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.SQLException;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * 数据源组
// * 直接持有 DataSource 列表，提供完整的负载均衡功能
// *
// * @author 张俊
// * @date Created in 2025/1/2 18:31
// */
//public class GirGroupSourceBack extends AbstractDataSource {
//
//    protected final GiLogger log = GirLogger.getLoger(GirGroupSourceBack.class);
//
//    /**
//     * 组名
//     */
//    protected String groupName;
//
//    /**
//     * 该组下对应的数据源列表
//     */
//    protected List<DataSource> dataSources;
//
//    /**
//     * 负载策略类型
//     */
//    protected LoadStrategyType strategyType = LoadStrategyType.RANDOM;
//
//    /**
//     * 轮询计数器
//     */
//    protected AtomicInteger roundRobinCounter = new AtomicInteger(0);
//
//    /**
//     * 权重配置（数据源 -> weight）
//     */
//    protected Map<DataSource, Integer> weightMap = new ConcurrentHashMap<>();
//
//    /**
//     * 总权重值
//     */
//    protected int totalWeight = 0;
//
//    // ==================== 构造方法 ====================
//
//    /**
//     * 构造方法（默认随机策略）
//     */
//    public GirGroupSourceBack(String groupName, List<DataSource> dataSources) {
//        this(groupName, dataSources, LoadStrategyType.RANDOM);
//    }
//
//    /**
//     * 构造方法（指定负载策略）
//     */
//    public GirGroupSourceBack(String groupName, List<DataSource> dataSources, LoadStrategyType strategyType) {
//        this.groupName = groupName;
//        this.dataSources = dataSources;
//        this.strategyType = strategyType;
//
//        // 初始化权重（默认每个数据源权重为1）
//        for (DataSource ds : dataSources) {
//            weightMap.put(ds, 1);
//            totalWeight += 1;
//        }
//
//        log.info("初始化数据源组 [{}], 数据源数量: {}, 负载策略: {}",
//                groupName, dataSources.size(), strategyType.getDescription());
//    }
//
//    // ==================== 权重管理 ====================
//
//    /**
//     * 设置权重（仅在权重策略下生效）
//     */
//    public GirGroupSourceBack setWeight(DataSource dataSource, int weight) {
//        if (weight <= 0) {
//            throw new IllegalArgumentException("权重必须大于0");
//        }
//        Integer oldWeight = weightMap.get(dataSource);
//        if (oldWeight != null) {
//            totalWeight -= oldWeight;
//        }
//        weightMap.put(dataSource, weight);
//        totalWeight += weight;
//        log.info("Group [{}] 设置数据源权重为: {}", groupName, weight);
//        return this;
//    }
//
//    /**
//     * 设置权重（通过索引）
//     */
//    public GirGroupSourceBack setWeight(int index, int weight) {
//        if (index < 0 || index >= dataSources.size()) {
//            throw new IllegalArgumentException("数据源索引越界: " + index);
//        }
//        setWeight(dataSources.get(index), weight);
//        return this;
//    }
//
//    /**
//     * 获取数据源的权重
//     */
//    public int getWeight(DataSource dataSource) {
//        return weightMap.getOrDefault(dataSource, 1);
//    }
//
//    // ==================== 负载均衡策略 ====================
//
//    /**
//     * 选择数据源（基于负载策略）
//     */
//    protected DataSource selectDataSource() {
//        if (dataSources == null || dataSources.isEmpty()) {
//            throw new IllegalStateException("数据源组 [" + groupName + "] 数据源列表为空");
//        }
//
//        switch (strategyType) {
//            case ROUND_ROBIN:
//                return roundRobinSelect();
//            case WEIGHT:
//                return weightSelect();
//            case LEAST_ACTIVE:
//                return leastActiveSelect();
//            case RANDOM:
//            default:
//                return randomSelect();
//        }
//    }
//
//    /**
//     * 随机策略
//     */
//    protected DataSource randomSelect() {
//        int i = RandomUtil.randomInt(dataSources.size());
//        return dataSources.get(i);
//    }
//
//    /**
//     * 轮询策略
//     */
//    protected DataSource roundRobinSelect() {
//        int index = roundRobinCounter.getAndIncrement() % dataSources.size();
//        return dataSources.get(index);
//    }
//
//    /**
//     * 权重策略
//     */
//    protected DataSource weightSelect() {
//        int randomWeight = RandomUtil.randomInt(totalWeight);
//        int currentWeight = 0;
//        for (Map.Entry<DataSource, Integer> entry : weightMap.entrySet()) {
//            currentWeight += entry.getValue();
//            if (randomWeight < currentWeight) {
//                return entry.getKey();
//            }
//        }
//        return dataSources.get(0);
//    }
//
//    /**
//     * 最少连接数策略
//     */
//    protected DataSource leastActiveSelect() {
//        DataSource selected = null;
//        int minActiveCount = Integer.MAX_VALUE;
//
//        for (DataSource ds : dataSources) {
//            Integer activeCount = getActiveCount(ds);
//            if (activeCount != null) {
//                if (activeCount < minActiveCount) {
//                    minActiveCount = activeCount;
//                    selected = ds;
//                }
//            } else {
//                log.warn("数据源 [{}] 无法获取活跃连接数，降级为随机策略", getDataSourceInfo(ds));
//                return randomSelect();
//            }
//        }
//        return selected != null ? selected : randomSelect();
//    }
//
//    /**
//     * 获取数据源的活跃连接数
//     */
//    protected Integer getActiveCount(DataSource dataSource) {
//        // 如果是 AdvDataSourceWrapper，使用其方法
//        if (dataSource instanceof AdvDataSourceWrapper) {
//            return ((AdvDataSourceWrapper) dataSource).getActiveCount();
//        }
//        if (dataSource instanceof LazyDataSourceWrapper) {
//            return ((LazyDataSourceWrapper) dataSource).getActiveCount();
//        }
//
//        // 尝试通过反射获取活跃连接数（支持常见连接池）
//        try {
//            if (dataSource.getClass().getName().contains("Druid")) {
//                return (Integer) dataSource.getClass().getMethod("getActiveCount").invoke(dataSource);
//            } else if (dataSource.getClass().getName().contains("Hikari")) {
//                Object pool = dataSource.getClass().getMethod("getHikariPoolMXBean").invoke(dataSource);
//                return (Integer) pool.getClass().getMethod("getActiveConnections").invoke(pool);
//            }
//        } catch (Exception e) {
//        }
//
//        return null;
//    }
//
//    /**
//     * 获取数据源信息（用于日志）
//     */
//    protected String getDataSourceInfo(DataSource ds) {
//        if (ds instanceof AdvDataSourceWrapper) {
//            return ((AdvDataSourceWrapper) ds).getJdbcUrl();
//        }
//        return ds.getClass().getSimpleName() + "@" + ds.hashCode();
//    }
//
//    // ==================== 公共方法 ====================
//
//    /**
//     * 动态切换负载策略
//     */
//    public GirGroupSourceBack setStrategyType(LoadStrategyType strategyType) {
//        this.strategyType = strategyType;
//        log.info("Group [{}] 负载策略切换为: {}", groupName, strategyType.getDescription());
//        return this;
//    }
//
//    public LoadStrategyType getStrategyType() {
//        return strategyType;
//    }
//
//    /**
//     * 获取当前数据源组的大小
//     */
//    public int size() {
//        return dataSources != null ? dataSources.size() : 0;
//    }
//
//    /**
//     * 获取所有数据源（只读）
//     */
//    public List<DataSource> getDataSources() {
//        return java.util.Collections.unmodifiableList(dataSources);
//    }
//
//    /**
//     * 获取组名
//     */
//    public String getGroupName() {
//        return groupName;
//    }
//
//    /**
//     * 刷新数据源列表
//     */
//
//    public GirGroupSourceBack refreshDataSources(List<DataSource> newDataSources) {
//        if (newDataSources == null || newDataSources.isEmpty()) {
//            throw new IllegalArgumentException("数据源列表不能为空");
//        }
//
//        this.dataSources = newDataSources;
//
//        // 重新初始化权重
//        weightMap.clear();
//        totalWeight = 0;
//        for (DataSource ds : dataSources) {
//            weightMap.put(ds, 1);
//            totalWeight += 1;
//        }
//
//        // 重置轮询计数器
//        roundRobinCounter.set(0);
//
//        log.info("Group [{}] 刷新数据源列表, 新数量: {}", groupName, dataSources.size());
//        return this;
//    }
//
//    /**
//     * 添加数据源
//     */
//    public GirGroupSourceBack addDataSource(DataSource dataSource) {
//        addDataSource(dataSource, 1);
//        return this;
//    }
//
//    /**
//     * 添加数据源并指定权重
//     */
//    public GirGroupSourceBack addDataSource(DataSource dataSource, int weight) {
//        if (dataSource == null) {
//            throw new IllegalArgumentException("数据源不能为空");
//        }
//        if (weight <= 0) {
//            throw new IllegalArgumentException("权重必须大于0");
//        }
//
//        this.dataSources.add(dataSource);
//        weightMap.put(dataSource, weight);
//        totalWeight += weight;
//
//        log.info("Group [{}] 添加数据源, 当前数量: {}", groupName, dataSources.size());
//        return this;
//    }
//
//    /**
//     * 移除数据源
//     */
//    public boolean removeDataSource(DataSource dataSource) {
//        boolean removed = this.dataSources.remove(dataSource);
//        if (removed) {
//            Integer oldWeight = weightMap.remove(dataSource);
//            if (oldWeight != null) {
//                totalWeight -= oldWeight;
//            }
//            log.info("Group [{}] 移除数据源, 当前数量: {}", groupName, dataSources.size());
//        }
//        return removed;
//    }
//
//    // ==================== Connection 获取方法 ====================
//
//    @Override
//    public Connection getConnection() throws SQLException {
//        long startTime = System.currentTimeMillis();
//        DataSource ds = selectDataSource();
//        Connection conn = ds.getConnection();
//        long cost = System.currentTimeMillis() - startTime;
//
//        if (cost > 100) {
//            log.warn("Group [{}] 策略 [{}] 选择数据源 [{}] 获取连接耗时: {}ms",
//                    groupName, strategyType.getDescription(), getDataSourceInfo(ds), cost);
//        } else {
//            log.debug("Group [{}] 策略 [{}] 选择数据源: {}",
//                    groupName, strategyType.getDescription(), getDataSourceInfo(ds));
//        }
//
//        return conn;
//    }
//
//    @Override
//    public Connection getConnection(String username, String password) throws SQLException {
//        DataSource ds = selectDataSource();
//        return ds.getConnection(username, password);
//    }
//
//    @Override
//    public void close() throws IOException {
//        log.debug("Group [{}] close called, but no action taken - data sources managed externally", groupName);
//    }
//}
