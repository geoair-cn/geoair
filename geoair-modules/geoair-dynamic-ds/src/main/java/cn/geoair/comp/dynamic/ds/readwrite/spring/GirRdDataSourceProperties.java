package cn.geoair.comp.dynamic.ds.readwrite.spring;

import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 读写分离数据源配置
 * <p>
 * 配置前缀：<code>spring.datasource.geoair</code>
 * <p>
 * 配置模板文件位置：
 * <ul>
 *     <li>源码位置：<code>src/main/resources/cn/geoair/comp/dynamic/ds/readwrite/spring/template.yml</code></li>
 * </ul>
 *
 * <h3>YAML 配置示例</h3>
 * <pre>
 * spring:
 *   datasource:
 *     geoair:
 *       group-name: "orderRdGroup"
 *       master-data-source-id: "master_db_01"
 *       readwrite:
 *         enabled: true
 *         read-strategy: WEIGHT
 *         read-data-sources:
 *           - id: beijing_slave
 *             url: jdbc:postgresql://192.168.0.104:5432/ybls_address
 *             weight: 50
 *             enabled: true
 * </pre>
 *
 * <h3>Properties 配置示例</h3>
 * <pre>
 * spring.datasource.geoair.group-name=orderRdGroup
 * spring.datasource.geoair.readwrite.enabled=true
 * spring.datasource.geoair.readwrite.read-data-sources[0].id=beijing_slave
 * spring.datasource.geoair.readwrite.read-data-sources[0].url=jdbc:postgresql://...
 * </pre>
 *
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @author 张俊
 * @date Created in 2023/5/31 15:27
 */
@ConfigurationProperties(prefix = "spring.datasource.geoair")
@Data
public class GirRdDataSourceProperties {

    /**
     * 读库组名称
     */
    private String groupName;

    /**
     * 主节点的数据源Id
     */
    private String masterDataSourceId;

    /**
     * 读写分离配置
     */
    private ReadWriteConfig readwrite = new ReadWriteConfig();


    public String getGroupName() {
        if (GutilObject.isEmpty(groupName)) {
            return "defaultRdGroup";
        }
        return groupName;
    }

    public String getMasterDataSourceId() {
        if (GutilObject.isEmpty(masterDataSourceId)) {
            return getGroupName() + "_mater";
        }
        return masterDataSourceId;
    }

    @Data
    public static class ReadWriteConfig {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * 读库负载策略（RANDOM, ROUND_ROBIN, WEIGHT,LEAST_ACTIVE）
         */
        private LoadStrategyType readStrategy = LoadStrategyType.ROUND_ROBIN;

        private List<ReadDataSourceConfig> readDataSources = new ArrayList<>();

        public List<ReadDataSourceConfig> getReadDataSourceConfigs() {
            if (readDataSources != null && !readDataSources.isEmpty()) {
                return readDataSources;
            }
            return Collections.emptyList();
        }


        /**
         * 获取有效的读数据源配置（启用的）
         */
        public List<ReadDataSourceConfig> findValidDataSources() {
            return getReadDataSourceConfigs().stream()
                    .filter(config -> config.getEnabled() != null && config.getEnabled())
                    .collect(Collectors.toList());
        }

        /**
         * 获取读库 URL 列表
         */
        public List<String> findReadUrlList() {
            return findValidDataSources().stream()
                    .map(ReadDataSourceConfig::getUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        /**
         * 获取数据源ID列表
         */
        public List<String> findReadDataSourceIds() {
            return getReadDataSourceConfigs().stream()
                    .map(ReadDataSourceConfig::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        /**
         * 根据数据源ID获取配置
         */
        public ReadDataSourceConfig findByDataSourceId(String dataSourceId) {
            return getReadDataSourceConfigs().stream()
                    .filter(config -> dataSourceId.equals(config.getId()))
                    .findFirst()
                    .orElse(null);
        }

        /**
         * 根据URL获取配置
         */
        public ReadDataSourceConfig findByUrl(String url) {
            return getReadDataSourceConfigs().stream()
                    .filter(config -> url.equals(config.getUrl()))
                    .findFirst()
                    .orElse(null);
        }

        /**
         * 获取数据源数量
         */
        public int findDataSourceCount() {
            return getReadDataSourceConfigs().size();
        }

        /**
         * 检查是否有可用的数据源
         */
        public boolean hasAvailableDataSources() {
            return !findValidDataSources().isEmpty();
        }
    }
}
