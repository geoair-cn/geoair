package cn.geoair.comp.dynamic.ds.readwrite;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.AdvDynamicDataSourceStorage;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
import cn.geoair.comp.dynamic.ds.readwrite.log.RdLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

/**
 * 读写分离数据源构建器
 *
 * @author 张俊
 * @date Created in 2026/5/28 18:55
 */
public class GirReadWriteDataSourceBuilder {

    // 主库配置
    private String masterDataSourceId;
    private DataSource masterDataSource;

    // 从库配置
    private List<DataSource> slaveDataSource = new ArrayList<>();
    private GirGroupSource slaveGroupSource;
    private GirGroupByIdDataSource slaveGroupByIdDataSource;

    // 从库组配置
    private List<String> slaveDataSourceIds = new ArrayList<>();
    private String slaveGroupName = "defaultSlaveGroup";
    private LoadStrategyType slaveStrategy = LoadStrategyType.RANDOM;
    private boolean enableLazyLoad = true;

    // 读写分离数据源配置
    private String dataSourceName;

    /** 开始构建 */
    public static GirReadWriteDataSourceBuilder builder() {
        return new GirReadWriteDataSourceBuilder();
    }

    // ==================== 主库配置方法 ====================

    /** 设置主库（通过ID） */
    public GirReadWriteDataSourceBuilder master(String dataSourceId) {
        this.masterDataSourceId = dataSourceId;
        return this;
    }

    /** 设置主库（直接传入DataSource） */
    public GirReadWriteDataSourceBuilder master(DataSource dataSource) {
        this.masterDataSource = dataSource;
        return this;
    }

    // ==================== 从库配置方法（核心 API） ====================

    /** 设置从库（直接传入单个 DataSource） */
    public GirReadWriteDataSourceBuilder slave(DataSource dataSource) {
        this.slaveDataSource.add(dataSource);
        return this;
    }

    /** 设置从库组（直接传入 GirGroupSource） */
    public GirReadWriteDataSourceBuilder slave(GirGroupSource dataSources) {
        this.slaveGroupSource = dataSources;
        return this;
    }

    /** 设置从库组（直接传入 GirGroupByIdDataSource） */
    public GirReadWriteDataSourceBuilder slave(GirGroupByIdDataSource dataSources) {
        this.slaveGroupByIdDataSource = dataSources;
        return this;
    }

    /** 设置从库组（直接传入 GirGroupByIdDataSource） */
    public GirReadWriteDataSourceBuilder slaves(GirGroupByIdDataSource dataSources) {
        this.slaveGroupByIdDataSource = dataSources;
        return this;
    }

    // ==================== 从库配置方法（辅助方法，用于快速构建） ====================

    /** 添加从库（通过ID） */
    public GirReadWriteDataSourceBuilder addSlave(String dataSourceId) {
        this.slaveDataSourceIds.add(dataSourceId);
        return this;
    }

    /** 批量添加从库（通过ID） */
    public GirReadWriteDataSourceBuilder slaves(String... dataSourceIds) {
        this.slaveDataSourceIds.addAll(Arrays.asList(dataSourceIds));
        return this;
    }

    /** 批量添加从库（通过ID列表） */
    public GirReadWriteDataSourceBuilder slaves(List<String> dataSourceIds) {
        this.slaveDataSourceIds.addAll(dataSourceIds);
        return this;
    }

    /** 批量添加从库（直接传入DataSource列表） */
    public GirReadWriteDataSourceBuilder slaves(DataSource... dataSources) {
        for (DataSource ds : dataSources) {
            slave(ds);
        }
        return this;
    }

    /** 设置从库组名称（当使用 ID 列表方式时生效） */
    public GirReadWriteDataSourceBuilder slaveGroupName(String groupName) {
        this.slaveGroupName = groupName;
        return this;
    }

    /** 设置从库负载策略（当使用 ID 列表方式时生效） */
    public GirReadWriteDataSourceBuilder slaveStrategy(LoadStrategyType strategy) {
        this.slaveStrategy = strategy;
        return this;
    }

    /** 设置从库负载策略（通过策略名称） */
    public GirReadWriteDataSourceBuilder slaveStrategy(String strategyName) {
        LoadStrategyType strategy = LoadStrategyType.fromCode(strategyName);
        if (strategy != null) {
            this.slaveStrategy = strategy;
        } else {
            RdLog.getInstance().warn("未知的负载策略: {}, 使用默认 RANDOM", strategyName);
        }
        return this;
    }

    /** 是否启用延迟加载（当使用 ID 列表方式时生效，默认true） */
    public GirReadWriteDataSourceBuilder enableLazyLoad(boolean enable) {
        this.enableLazyLoad = enable;
        return this;
    }

    // ==================== 通用配置方法 ====================

    /** 设置数据源名称 */
    public GirReadWriteDataSourceBuilder name(String name) {
        this.dataSourceName = name;
        return this;
    }

    // ==================== 构建方法 ====================

    /** 构建读写分离数据源 */
    public GirReadWriteDataSource build() {
        validate();

        // 获取或创建从库组
        GirGroupSource slaveGroup = buildSlaveGroup();

        // 获取主库
        DataSource master = getMasterDataSource();

        // 创建读写分离数据源
        GirReadWriteDataSource dataSource = new GirReadWriteDataSource(master, slaveGroup);

        RdLog.getInstance()
                .debug(
                        "读写分离数据源构建完成: master={}, slaveGroupType={}",
                        getMasterInfo(),
                        getSlaveGroupType());

        return dataSource;
    }

    private void validate() {
        // 校验主库
        if (masterDataSourceId == null && masterDataSource == null) {
            throw new IllegalStateException("主库不能为空，请设置 master 或 masterId");
        }

        // 校验从库（至少有一种方式配置了从库）
        boolean hasSlave =
                slaveDataSource != null
                        || slaveGroupSource != null
                        || slaveGroupByIdDataSource != null
                        || !slaveDataSourceIds.isEmpty();

        if (!hasSlave) {
            throw new IllegalStateException("从库不能为空，请通过 slave()、slaves()、addSlave() 等方式配置从库");
        }

        RdLog.getInstance().debug("校验通过: master={}", getMasterInfo());
    }

    private GirGroupSource buildSlaveGroup() {
        // 优先级：GirGroupByIdDataSource > GirGroupSource > 单个 DataSource > ID列表

        if (slaveGroupByIdDataSource != null) {
            RdLog.getInstance().debug("使用已配置的 GirGroupByIdDataSource 作为从库组");
            return slaveGroupByIdDataSource;
        }

        if (slaveGroupSource != null) {
            RdLog.getInstance().debug("使用已配置的 GirGroupSource 作为从库组");
            return slaveGroupSource;
        }

        if (GutilObject.isNotEmpty(slaveDataSource)) {
            return new GirGroupSource(slaveGroupName, slaveDataSource, slaveStrategy);
        }

        if (!slaveDataSourceIds.isEmpty()) {
            RdLog.getInstance().debug("使用 ID 列表创建从库组，延迟加载: {}", enableLazyLoad);
            if (enableLazyLoad) {
                return GirGroupByIdDataSource.builderById()
                        .dataSourceIds(slaveDataSourceIds)
                        .strategy(slaveStrategy)
                        .groupName(slaveGroupName)
                        .build();
            } else {
                // 立即加载所有数据源
                List<DataSource> loadedDataSources = new ArrayList<>();
                for (String dsId : slaveDataSourceIds) {
                    AdvDataSourceWrapper ds =
                            AdvDynamicDataSourceStorage.getInstance().getOrCreateDataSource(dsId);
                    if (ds != null) {
                        loadedDataSources.add(ds);
                    } else {
                        RdLog.getInstance().warn("数据源不存在: {}", dsId);
                    }
                }
                return new GirGroupSource(slaveGroupName, loadedDataSources, slaveStrategy);
            }
        }

        throw new IllegalStateException("无法创建从库组");
    }

    private DataSource getMasterDataSource() {
        if (masterDataSource != null) {
            return masterDataSource;
        }
        if (enableLazyLoad) {
            return new LazyDataSourceWrapper(masterDataSourceId);
        } else {
            AdvDataSourceWrapper ds =
                    AdvDynamicDataSourceStorage.getInstance()
                            .getOrCreateDataSource(masterDataSourceId);
            if (ds == null) {
                throw new IllegalStateException("主库数据源不存在: " + masterDataSourceId);
            }
            return ds;
        }
    }

    private String getMasterInfo() {
        if (masterDataSource != null) {
            return masterDataSource.getClass().getSimpleName();
        }
        return masterDataSourceId;
    }

    private String getSlaveGroupType() {
        if (slaveGroupByIdDataSource != null) {
            return "GirGroupByIdDataSource";
        }
        if (slaveGroupSource != null) {
            return "GirGroupSource";
        }
        if (slaveDataSource != null) {
            return "SingleDataSource";
        }
        if (!slaveDataSourceIds.isEmpty()) {
            return "IdList (LazyLoad=" + enableLazyLoad + ")";
        }
        return "Unknown";
    }

    // ==================== 便捷静态方法 ====================

    /** 快速构建（使用 GirGroupSource 作为从库组） */
    public static GirReadWriteDataSource build(String masterId, GirGroupSource slaveGroup) {
        return builder().master(masterId).slaves(slaveGroup).build();
    }

    /** 快速构建（使用 GirGroupByIdDataSource 作为从库组） */
    public static GirReadWriteDataSource build(String masterId, GirGroupByIdDataSource slaveGroup) {
        return builder().master(masterId).slaves(slaveGroup).build();
    }

    /** 快速构建（使用 DataSource 作为主库，GirGroupSource 作为从库组） */
    public static GirReadWriteDataSource build(DataSource master, GirGroupSource slaveGroup) {
        return builder().master(master).slaves(slaveGroup).build();
    }

    /** 快速构建（主库ID + 从库ID列表） */
    public static GirReadWriteDataSource build(String masterId, List<String> slaveIds) {
        return builder().master(masterId).slaves(slaveIds).build();
    }

    /** 快速构建（主库ID + 单个从库ID） */
    public static GirReadWriteDataSource build(String masterId, String slaveId) {
        return builder().master(masterId).addSlave(slaveId).build();
    }
}
