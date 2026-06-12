package cn.geoair.comp.dynamic.ds.readwrite.spring;

import cn.geoair.base.log.GemLogLevel;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 读写分离数据源配置 配置前缀：spring.datasource.geoair
 *
 * <p>配置模板文件：<code>META-INF/template.yml</code>
 *
 * <p>可从以下路径获取模板：
 *
 * <ul>
 *   <li>源码位置：<code>src/main/resources/META-INF/template.yml</code>
 *   <li>源码位置：<code>src/main/resources/META-INF/template.properties</code>
 * </ul>
 *
 * @author 张俊
 * @date Created in 2023/5/31 15:27
 */
@ConfigurationProperties(prefix = "spring.datasource.geoair")
@Data
public class GirRdDataSourceProperties {

    /** 读库组名称 */
    private String groupName;

    /** 主节点的数据源Id */
    private String masterDataSourceId;

    /** /** 输出的最小的日志级别 , 这里只是标记输出的最小级别， 这只是第一道拦截器，具体的日志级别还需要看具体的日志实现 */
    public GemLogLevel minLogLevel = GemLogLevel.INFO;

    /**
     * 是否使用独立日志实现，不依托于全局的日志实现
     *
     * <p>true: 使用独立的日志实现（如自定义的日志处理逻辑）<br>
     * false: 使用默认的日志实现（如 Slf4j、Log4j 等）
     */
    public boolean useIndependentLog = false;

    /** 读写分离配置 */
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
        /** 是否启用 */
        private boolean enabled = false;

        /** 读库负载策略（RANDOM, ROUND_ROBIN, WEIGHT,LEAST_ACTIVE） */
        private LoadStrategyType readStrategy = LoadStrategyType.ROUND_ROBIN;

        private List<ReadDataSourceConfig> readDataSources = new ArrayList<>();

        public List<ReadDataSourceConfig> getReadDataSourceConfigs() {
            if (readDataSources != null && !readDataSources.isEmpty()) {
                return readDataSources;
            }
            return Collections.emptyList();
        }

        /** 获取有效的读数据源配置（启用的） */
        public List<ReadDataSourceConfig> findValidDataSources() {
            return getReadDataSourceConfigs()
                    .stream()
                    .filter(config -> config.getEnabled() != null && config.getEnabled())
                    .collect(Collectors.toList());
        }

        /** 获取读库 URL 列表 */
        public List<String> findReadUrlList() {
            return findValidDataSources()
                    .stream()
                    .map(ReadDataSourceConfig::getUrl)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        /** 获取数据源ID列表 */
        public List<String> findReadDataSourceIds() {
            return getReadDataSourceConfigs()
                    .stream()
                    .map(ReadDataSourceConfig::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        /** 根据数据源ID获取配置 */
        public ReadDataSourceConfig findByDataSourceId(String dataSourceId) {
            return getReadDataSourceConfigs()
                    .stream()
                    .filter(config -> dataSourceId.equals(config.getId()))
                    .findFirst()
                    .orElse(null);
        }

        /** 根据URL获取配置 */
        public ReadDataSourceConfig findByUrl(String url) {
            return getReadDataSourceConfigs()
                    .stream()
                    .filter(config -> url.equals(config.getUrl()))
                    .findFirst()
                    .orElse(null);
        }

        /** 获取数据源数量 */
        public int findDataSourceCount() {
            return getReadDataSourceConfigs().size();
        }

        /** 检查是否有可用的数据源 */
        public boolean hasAvailableDataSources() {
            return !findValidDataSources().isEmpty();
        }
    }
}
