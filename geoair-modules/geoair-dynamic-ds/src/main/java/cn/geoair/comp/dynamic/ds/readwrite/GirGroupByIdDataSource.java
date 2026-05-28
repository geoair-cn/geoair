package cn.geoair.comp.dynamic.ds.readwrite;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.AdvDynamicDataSourceStorage;
import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.hutool.core.util.RandomUtil;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源组代理（基于数据源ID）
 * 使用 Builder 模式，在 build 时再初始化
 *
 * @author 张俊
 * @date Created in 2025/1/2 18:31
 */
public class GirGroupByIdDataSource extends GirGroupSource {

    protected final GiLogger log = GirLogger.getLoger(GirGroupByIdDataSource.class);

    /**
     * 该组下对应的数据源Id列表
     */
    private List<String> dataSourceIds;

    /**
     * 权重配置（数据源Id -> weight）
     */
    protected Map<String, Integer> weightMap = new ConcurrentHashMap<>();

    // ==================== 私有构造方法 ====================

    /**
     * 私有构造方法，通过 Builder 创建
     */
    protected GirGroupByIdDataSource(Builder builder) {
        super(builder.groupName, new ArrayList<>(), builder.strategyType);
        this.dataSourceIds = new ArrayList<>(builder.dataSourceIds);

        // 设置权重
        if (builder.weights != null) {
            for (Map.Entry<String, Integer> entry : builder.weights.entrySet()) {
                this.weightMap.put(entry.getKey(), entry.getValue());
                totalWeight += entry.getValue();
            }
        } else {
            // 默认权重为1
            for (String dsId : dataSourceIds) {
                this.weightMap.put(dsId, 1);
                totalWeight += 1;
            }
        }


        refreshDataSourcesFromIds();

        log.info("初始化数据源组代理 [{}], 数据源数量: {}, 负载策略: {}, 总权重: {}",
                groupName, dataSourceIds.size(), strategyType.getDescription(), totalWeight);
    }


    // ==================== 私有方法 ====================

    /**
     * 从数据源ID列表刷新 DataSource 列表
     */
    private void refreshDataSourcesFromIds() {
        List<DataSource> newDataSources = new ArrayList<>();
        for (String dsId : dataSourceIds) {
            newDataSources.add(new LazyDataSourceWrapper(dsId));
        }
        if (newDataSources.isEmpty()) {
            throw new IllegalStateException("数据源组 [" + groupName + "] 没有可用的数据源");
        }
        this.dataSources = newDataSources;
    }

    // ==================== 实例方法 ====================

    /**
     * 设置权重（通过数据源ID）
     */
    public GirGroupByIdDataSource setWeightById(String dataSourceId, int weight) {
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
        return this;
    }

    /**
     * 获取数据源ID的权重
     */
    public int getWeightById(String dataSourceId) {
        return weightMap.getOrDefault(dataSourceId, 1);
    }

    /**
     * 添加数据源（通过ID）
     */
    public GirGroupByIdDataSource addDataSourceById(String dataSourceId) {
        return addDataSourceById(dataSourceId, 1);
    }

    /**
     * 添加数据源并指定权重（通过ID）
     */
    public GirGroupByIdDataSource addDataSourceById(String dataSourceId, int weight) {
        this.dataSourceIds.add(dataSourceId);
        // 添加延迟加载包装器
        this.dataSources.add(new LazyDataSourceWrapper(dataSourceId));
        setWeightById(dataSourceId, weight);
        log.info("Group [{}] 添加数据源, dsId: {}, 当前数量: {}", groupName, dataSourceId, dataSources.size());
        return this;
    }

    /**
     * 移除数据源（通过ID）
     */
    public boolean removeDataSourceById(String dataSourceId) {
        boolean removed = this.dataSourceIds.remove(dataSourceId);
        if (removed) {
            // 找到并移除对应的 LazyDataSourceWrapper
            LazyDataSourceWrapper toRemove = null;
            for (DataSource ds : dataSources) {
                if (ds instanceof LazyDataSourceWrapper) {
                    LazyDataSourceWrapper wrapper = (LazyDataSourceWrapper) ds;
                    if (wrapper.getDataSourceId().equals(dataSourceId)) {
                        toRemove = wrapper;
                        break;
                    }
                }
            }
            if (toRemove != null) {
                dataSources.remove(toRemove);
            }
            weightMap.remove(dataSourceId);
            log.info("Group [{}] 移除数据源, dsId: {}, 当前数量: {}", groupName, dataSourceId, dataSources.size());
        }
        return removed;
    }

    // ==================== 查询方法 ====================

    /**
     * 获取当前数据源ID列表
     */
    public List<String> getDataSourceIds() {
        return java.util.Collections.unmodifiableList(dataSourceIds);
    }

    /**
     * 获取数据源ID对应的实际数据源
     */
    public AdvDataSourceWrapper getDataSourceById(String dataSourceId) {
        return AdvDynamicDataSourceStorage.getInstance().getDataSource(dataSourceId);
    }

    /**
     * 获取某个数据源的URL（用于调试）
     */
    public String getUrl() {
        DataSource ds = selectDataSource();
        if (ds instanceof LazyDataSourceWrapper) {
            return ((LazyDataSourceWrapper) ds).getJdbcUrl();
        }
        return ds.toString();
    }

    // ==================== 重写父类方法 ====================

    @Override
    protected DataSource weightSelect() {

        int randomWeight = RandomUtil.randomInt(totalWeight);
        int currentWeight = 0;
        for (Map.Entry<String, Integer> entry : weightMap.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                String selectedId = entry.getKey();
                // 找到对应的 DataSource
                for (DataSource ds : dataSources) {
                    if (ds instanceof LazyDataSourceWrapper) {
                        LazyDataSourceWrapper wrapper = (LazyDataSourceWrapper) ds;
                        if (wrapper.getDataSourceId().equals(selectedId)) {
                            return wrapper;
                        }
                    }
                }
            }
        }
        return dataSources.get(0);
    }




    // ==================== Builder ====================

    public static GirGroupByIdDataSource.Builder builderById() {
        return new GirGroupByIdDataSource.Builder();
    }

    public static class Builder {
        private String groupName;
        private List<String> dataSourceIds = new ArrayList<>();
        private Map<String, Integer> weights = new HashMap<>();
        private LoadStrategyType strategyType = LoadStrategyType.RANDOM;

        /**
         * 设置组名
         */
        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        /**
         * 添加数据源ID
         */
        public Builder addDataSourceId(String dataSourceId) {
            this.dataSourceIds.add(dataSourceId);
            return this;
        }

        /**
         * 批量添加数据源ID
         */
        public Builder dataSourceIds(List<String> dataSourceIds) {
            this.dataSourceIds.addAll(dataSourceIds);
            return this;
        }

        /**
         * 批量添加数据源ID（可变参数）
         */
        public Builder dataSourceIds(String... dataSourceIds) {
            this.dataSourceIds.addAll(Arrays.asList(dataSourceIds));
            return this;
        }

        /**
         * 设置权重（单个）
         */
        public Builder weight(String dataSourceId, int weight) {
            this.weights.put(dataSourceId, weight);
            return this;
        }

        /**
         * 设置权重（批量）
         */
        public Builder weights(Map<String, Integer> weights) {
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
         * 构建 GirGroupByIdDataSource
         */
        public GirGroupByIdDataSource build() {
            if (groupName == null || groupName.trim().isEmpty()) {
                throw new IllegalStateException("groupName 不能为空");
            }
            if (dataSourceIds.isEmpty()) {
                throw new IllegalStateException("dataSourceIds 不能为空");
            }
            return new GirGroupByIdDataSource(this);
        }
    }
}
