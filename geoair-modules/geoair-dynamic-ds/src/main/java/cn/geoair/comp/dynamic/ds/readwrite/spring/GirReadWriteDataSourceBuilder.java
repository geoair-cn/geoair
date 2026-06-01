package cn.geoair.comp.dynamic.ds.readwrite.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.AdvDynamicDataSourceStorage;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceHelper;
import cn.geoair.comp.dynamic.ds.apo.DataSourceApo;
import cn.geoair.comp.dynamic.ds.readwrite.GirGroupSource;
import cn.geoair.comp.dynamic.ds.readwrite.GirReadWriteDataSource;
import cn.geoair.comp.dynamic.ds.readwrite.GirGroupByIdDataSource;
import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
import cn.geoair.comp.dynamic.ds.spring.GirSpringDataSourceUtils;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 读写分离数据源构建工具类
 * 基于 GirRdDataSourceProperties 配置构建
 *
 * @author 张俊
 * @date Created in 2026/5/29
 */
public class GirReadWriteDataSourceBuilder {

    private static final GiLogger log = GirLogger.getLoger(GirReadWriteDataSourceBuilder.class);

    private final GirRdDataSourceProperties properties;

    private final DataSourceProperties springDataSourceProperties;

    private final IAdvDataSourceHelper iAdvDataSourceHelper;


    public GirReadWriteDataSourceBuilder(GirRdDataSourceProperties properties,
                                         DataSourceProperties springDataSourceProperties, IAdvDataSourceHelper iAdvDataSourceHelper) {
        this.properties = properties;
        this.springDataSourceProperties = springDataSourceProperties;
        this.iAdvDataSourceHelper = iAdvDataSourceHelper;
    }


    public static GirReadWriteDataSourceBuilder builder(GirRdDataSourceProperties properties,
                                                        DataSourceProperties springDataSourceProperties, IAdvDataSourceHelper iAdvDataSourceHelper) {
        return new GirReadWriteDataSourceBuilder(properties, springDataSourceProperties, iAdvDataSourceHelper);
    }

    /**
     * 构建读写分离数据源
     */
    public GirReadWriteDataSource build() {
        validate();


        DataSource masterDataSource = getOrCreateMasterDataSource();


        GirGroupSource slaveGroup = createSlaveGroup();


        GirReadWriteDataSource readWriteDataSource = new GirReadWriteDataSource(masterDataSource, slaveGroup);

        log.info("读写分离数据源构建完成，从库数量: {}, 策略: {}",
                properties.getReadwrite().getReadUrlList().size(),
                properties.getReadwrite().getReadStrategy().getDescription());

        return readWriteDataSource;
    }

    /**
     * 验证配置
     */
    private void validate() {
        if (springDataSourceProperties == null) {
            throw new IllegalStateException("Spring DataSourceProperties 不能为空");
        }
        if (properties.getReadwrite() == null) {
            throw new IllegalStateException("读写分离配置不能为空");
        }
        if (properties.getReadwrite().getReadUrls() == null ||
                properties.getReadwrite().getReadUrls().isEmpty()) {
            throw new IllegalStateException("从库URL列表不能为空");
        }
    }


    private DataSource getOrCreateMasterDataSource() {
        return createDataSourceFromProperties(springDataSourceProperties);
    }


    private DataSource createDataSourceFromProperties(DataSourceProperties props) {
        DataSourceApo dataSourceApo = GirSpringDataSourceUtils.convertToDataSourceApo(props);
        String masterDsId = properties.getGroupName() + "_master";
        dataSourceApo.setId(masterDsId);
        DataSource dbDataSourceByApo = iAdvDataSourceHelper.getDbDataSourceByApo(dataSourceApo);
        AdvDynamicDataSourceStorage.getInstance()
                .registerDataSource(masterDsId, dbDataSourceByApo);
        log.info("创建数据源: URL={}, Driver={}", props.getUrl(), props.getDriverClassName());
        return dbDataSourceByApo;
    }


    private GirGroupSource createSlaveGroup() {
        GirRdDataSourceProperties.ReadWriteConfig config = properties.getReadwrite();
        List<String> readUrls = config.getReadUrlList();
        LoadStrategyType strategy = config.getReadStrategy();
        String groupName = properties.getGroupName();
        Map<String, Integer> weights = config.getWeights();

        // 创建从库数据源ID列表
        List<String> slaveIds = new ArrayList<>();
        Map<String, String> urlToIdMap = new ConcurrentHashMap<>();

        for (int i = 0; i < readUrls.size(); i++) {
            String url = readUrls.get(i);
            String slaveId = groupName + "_slave_" + i;
            slaveIds.add(slaveId);
            urlToIdMap.put(url, slaveId);
            // 检查是否已存在，不存在则创建
            try {
                boolean existing = AdvDynamicDataSourceStorage.getInstance()
                        .containsDataSource(slaveId);
                if (!existing) {
                    DataSource slaveDs = createSlaveDataSource(url, slaveId);
                    AdvDynamicDataSourceStorage.getInstance()
                            .registerDataSource(slaveId, slaveDs);
                    log.info("创建并注册从库数据源: {} -> {}", slaveId, url);
                }
            } catch (Exception e) {
                log.error("注册从库数据源失败: {}", slaveId, e);
            }
        }

        // 创建 GirGroupByIdDataSource
        GirGroupByIdDataSource slaveGroup = GirGroupByIdDataSource.builderById()
                .groupName(groupName)
                .dataSourceIds(slaveIds)
                .strategy(strategy)
                .build();

        // 设置权重（如果使用 WEIGHT 策略且有权重配置）
        if (strategy == LoadStrategyType.WEIGHT && weights != null && !weights.isEmpty()) {
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                String url = entry.getKey();
                Integer weight = entry.getValue();
                String slaveId = urlToIdMap.get(url);
                if (slaveId != null) {
                    slaveGroup.setWeightById(slaveId, weight);
                    log.info("设置从库权重: {} -> {} ({})", slaveId, weight, url);
                } else {
                    log.warn("未找到对应的从库URL: {}", url);
                }
            }
        }

        log.info("创建从库组: {}, 数量: {}, 策略: {}", groupName, slaveIds.size(), strategy.getDescription());
        return slaveGroup;
    }

    private DataSource createSlaveDataSource(String slaveUrl, String slaveId) {
        DataSourceProperties propertiesSlave = new DataSourceProperties();
        BeanUtil.copyProperties(springDataSourceProperties, propertiesSlave);
        propertiesSlave.setUrl(slaveUrl);
        DataSourceApo dataSourceApo = GirSpringDataSourceUtils.convertToDataSourceApo(propertiesSlave);
        dataSourceApo.setId(slaveId);
        DataSource dbDataSourceByApo = iAdvDataSourceHelper.getDbDataSourceByApo(dataSourceApo);
        log.debug("创建从库数据源: URL={}, Driver={}", slaveUrl, springDataSourceProperties.getDriverClassName());
        return dbDataSourceByApo;
    }
}
