package cn.geoair.comp.dynamic.ds.readwrite.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.AdvDynamicDataSourceStorage;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceInitHelper;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.readwrite.GirGroupByIdDataSource;
import cn.geoair.comp.dynamic.ds.readwrite.GirGroupSource;
import cn.geoair.comp.dynamic.ds.readwrite.GirReadWriteDataSource;
import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
import cn.geoair.comp.dynamic.ds.readwrite.log.RdLog;
import cn.geoair.comp.dynamic.ds.spring.GirSpringDataSourceUtils;
import cn.hutool.core.bean.BeanUtil;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

/**
 * 读写分离数据源构建工具类 基于 GirRdDataSourceProperties 配置构建
 *
 * @author 张俊
 * @date Created in 2026/5/29
 */
public class GirSpringReadWriteDataSourceBuilder {

    private static final GiLogger log =
            GirLoggerFactory.getLogger(GirSpringReadWriteDataSourceBuilder.class);

    private final GirRdDataSourceProperties properties;

    private final DataSourceProperties springDataSourceProperties;

    private final IAdvDataSourceInitHelper iAdvDataSourceInitHelper;

    public GirSpringReadWriteDataSourceBuilder(
            GirRdDataSourceProperties properties,
            DataSourceProperties springDataSourceProperties,
            IAdvDataSourceInitHelper iAdvDataSourceInitHelper) {
        this.properties = properties;
        this.springDataSourceProperties = springDataSourceProperties;
        this.iAdvDataSourceInitHelper = iAdvDataSourceInitHelper;
    }

    public static GirSpringReadWriteDataSourceBuilder builder(
            GirRdDataSourceProperties properties,
            DataSourceProperties springDataSourceProperties,
            IAdvDataSourceInitHelper iAdvDataSourceInitHelper) {
        return new GirSpringReadWriteDataSourceBuilder(
                properties, springDataSourceProperties, iAdvDataSourceInitHelper);
    }

    /** 构建读写分离数据源 */
    public GirReadWriteDataSource build() {
        validate();

        DataSource masterDataSource = getOrCreateMasterDataSource();

        GirGroupSource slaveGroup = createSlaveGroup();

        GirReadWriteDataSource readWriteDataSource =
                new GirReadWriteDataSource(masterDataSource, slaveGroup);

        RdLog.getInstance()
                .info(
                        "读写分离数据源构建完成，从库数量: {}, 策略: {}",
                        properties.getReadwrite().findValidDataSources().size(),
                        properties.getReadwrite().getReadStrategy().getDescription());

        return readWriteDataSource;
    }

    /** 验证配置 */
    private void validate() {
        if (springDataSourceProperties == null) {
            throw new IllegalStateException("Spring DataSourceProperties 不能为空");
        }
        if (properties.getReadwrite() == null) {
            throw new IllegalStateException("读写分离配置不能为空");
        }

        // 检查是否有可用的从库配置
        if (!properties.getReadwrite().hasAvailableDataSources()) {
            throw new IllegalStateException("没有可用的从库配置，请检查 readDataSources 配置");
        }
    }

    /** 获取或创建主数据源 */
    private DataSource getOrCreateMasterDataSource() {
        return createDataSourceFromProperties(springDataSourceProperties);
    }

    /** 从配置属性创建数据源 */
    private DataSource createDataSourceFromProperties(DataSourceProperties props) {
        DataSourceApo dataSourceApo = GirSpringDataSourceUtils.convertToDataSourceApo(props);
        String masterDataSourceId = properties.getMasterDataSourceId();
        if (GutilObject.isEmpty(masterDataSourceId)) {
            masterDataSourceId = properties.getGroupName() + "_master";
        }
        dataSourceApo.setId(masterDataSourceId);
        DataSource dbDataSourceByApo = iAdvDataSourceInitHelper.getDbDataSourceByApo(dataSourceApo);
        AdvDynamicDataSourceStorage.getInstance()
                .registerDataSource(masterDataSourceId, dbDataSourceByApo);
        RdLog.getInstance()
                .info(
                        "创建主数据源: ID={}, URL={}, Driver={}",
                        masterDataSourceId,
                        props.getUrl(),
                        props.getDriverClassName());
        return dbDataSourceByApo;
    }

    /** 创建从库组 */
    private GirGroupSource createSlaveGroup() {
        GirRdDataSourceProperties.ReadWriteConfig config = properties.getReadwrite();
        List<ReadDataSourceConfig> validDataSources = config.findValidDataSources();
        LoadStrategyType strategy = config.getReadStrategy();
        String groupName = properties.getGroupName();

        // 创建从库数据源ID列表
        List<String> slaveIds = new ArrayList<>();

        int count = 0;
        for (ReadDataSourceConfig dsConfig : validDataSources) {
            String id = dsConfig.getId();
            String url = dsConfig.getUrl();

            if (GutilObject.isEmpty(id)) {
                id = groupName + "_" + count;
            }
            count++;
            slaveIds.add(id);

            // 检查是否已存在，不存在则创建
            try {
                boolean existing = AdvDynamicDataSourceStorage.getInstance().containsDataSource(id);
                if (!existing) {
                    DataSource slaveDs = createSlaveDataSource(url, id);
                    AdvDynamicDataSourceStorage.getInstance().registerDataSource(id, slaveDs);
                    RdLog.getInstance()
                            .info(
                                    "创建并注册从库数据源: ID={}, URL={}, 权重={}",
                                    id,
                                    url,
                                    dsConfig.getWeight());
                }
            } catch (Exception e) {
                RdLog.getInstance().error("注册从库数据源失败: ID={}, URL={}", id, url, e);
                throw new RuntimeException("注册从库数据源失败: " + id, e);
            }
        }

        // 构建从库组
        GirGroupByIdDataSource slaveGroup =
                GirGroupByIdDataSource.builderById()
                        .groupName(groupName)
                        .dataSourceIds(slaveIds)
                        .strategy(strategy)
                        .build();

        // 设置权重（如果使用 WEIGHT 策略）
        if (strategy == LoadStrategyType.WEIGHT) {
            for (ReadDataSourceConfig dsConfig : validDataSources) {
                String id = dsConfig.getId();
                Integer weight = dsConfig.getWeight();
                if (weight != null && weight > 0) {
                    slaveGroup.setWeightById(id, weight);
                    RdLog.getInstance()
                            .info("设置从库权重: ID={}, 权重={}, URL={}", id, weight, dsConfig.getUrl());
                }
            }
        }

        RdLog.getInstance()
                .info(
                        "创建从库组: groupName={}, 数量={}, 策略={}",
                        groupName,
                        slaveIds.size(),
                        strategy.getDescription());
        return slaveGroup;
    }

    /** 创建从库数据源 */
    private DataSource createSlaveDataSource(String slaveUrl, String slaveId) {
        DataSourceProperties propertiesSlave = new DataSourceProperties();
        BeanUtil.copyProperties(springDataSourceProperties, propertiesSlave);
        propertiesSlave.setUrl(slaveUrl);

        DataSourceApo dataSourceApo =
                GirSpringDataSourceUtils.convertToDataSourceApo(propertiesSlave);
        dataSourceApo.setId(slaveId);

        DataSource dbDataSourceByApo = iAdvDataSourceInitHelper.getDbDataSourceByApo(dataSourceApo);

        RdLog.getInstance()
                .debug(
                        "创建从库数据源完成: ID={}, URL={}, Driver={}",
                        slaveId,
                        slaveUrl,
                        springDataSourceProperties.getDriverClassName());

        return dbDataSourceByApo;
    }
}
