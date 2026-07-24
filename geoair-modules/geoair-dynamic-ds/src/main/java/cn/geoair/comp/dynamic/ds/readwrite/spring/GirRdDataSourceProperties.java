package cn.geoair.comp.dynamic.ds.readwrite.spring;

import cn.geoair.base.log.GemLogLevel;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import java.util.*;
import java.util.function.Consumer;
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

    /**
     * 复制 GirRdDataSourceProperties，完全由外部自定义
     *
     * @param source 源配置
     * @param readWriteConfigConsumer 对 ReadWriteConfig 的自定义操作
     * @return 复制后的配置
     */
    public static GirRdDataSourceProperties copyWithCustomization(
            GirRdDataSourceProperties source,
            Consumer<GirRdDataSourceProperties.ReadWriteConfig> readWriteConfigConsumer) {

        if (source == null) {
            return null;
        }

        // 1. 复制主对象
        GirRdDataSourceProperties target = new GirRdDataSourceProperties();
        BeanUtil.copyProperties(source, target);
        target.setGroupName(source.getGroupName() + "_by_copy_" + IdUtil.fastSimpleUUID());
        String masterDataSourceIdBySource = source.masterDataSourceId;
        if (GutilObject.isNotEmpty(masterDataSourceIdBySource)) {
            target.setMasterDataSourceId(
                    source.masterDataSourceId + "_by_copy_" + IdUtil.fastSimpleUUID());
        }
        // 2. 处理 ReadWriteConfig
        GirRdDataSourceProperties.ReadWriteConfig sourceReadwrite = source.getReadwrite();
        if (sourceReadwrite != null) {
            GirRdDataSourceProperties.ReadWriteConfig targetReadwrite =
                    copyReadWriteConfig(sourceReadwrite);

            // 3. 执行外部自定义操作
            if (readWriteConfigConsumer != null) {
                readWriteConfigConsumer.accept(targetReadwrite);
            }

            target.setReadwrite(targetReadwrite);
        }

        return target;
    }

    /** 复制 ReadWriteConfig，支持分别自定义主库和从库 */
    public static GirRdDataSourceProperties.ReadWriteConfig copyReadWriteConfigWithCustom(
            GirRdDataSourceProperties.ReadWriteConfig source,
            Consumer<List<ReadDataSourceConfig>> slavesConsumer) {

        if (source == null) {
            return null;
        }

        GirRdDataSourceProperties.ReadWriteConfig target =
                new GirRdDataSourceProperties.ReadWriteConfig();
        BeanUtil.copyProperties(source, target);

        // 复制并修改从库列表
        List<ReadDataSourceConfig> sourceSlaves = source.getReadDataSourceConfigs();
        if (sourceSlaves != null && !sourceSlaves.isEmpty()) {
            List<ReadDataSourceConfig> targetSlaves =
                    sourceSlaves
                            .stream()
                            .map(slave -> copyReadDataSourceConfig(slave, null))
                            .collect(Collectors.toList());

            // 执行外部自定义从库列表
            if (slavesConsumer != null) {
                slavesConsumer.accept(targetSlaves);
            }

            target.setReadDataSources(targetSlaves);
        }

        return target;
    }

    /** 复制单个 ReadDataSourceConfig，支持自定义操作 */
    public static ReadDataSourceConfig copyReadDataSourceConfig(
            ReadDataSourceConfig source, Consumer<ReadDataSourceConfig> consumer) {

        if (source == null) {
            return null;
        }

        ReadDataSourceConfig target = new ReadDataSourceConfig();
        BeanUtil.copyProperties(source, target);

        // 执行外部自定义操作
        if (consumer != null) {
            consumer.accept(target);
        }

        return target;
    }

    // ==================== 便捷方法（快速修改 schema） ====================

    /** 简单复制并添加 schema */
    public static GirRdDataSourceProperties copyWithSchema(
            GirRdDataSourceProperties source, String schema) {

        if (source == null) {
            return null;
        }

        return copyWithCustomization(
                source,
                readWriteConfig -> {
                    // 修改所有从库的 schema
                    List<ReadDataSourceConfig> slaves = readWriteConfig.getReadDataSourceConfigs();
                    if (slaves != null && !slaves.isEmpty()) {
                        slaves.forEach(
                                slave -> {
                                    String url = slave.getUrl();
                                    String id = slave.getId();
                                    slave.setId(id + "by_copy_" + IdUtil.fastSimpleUUID());
                                    slave.setUrl(appendSchemaToUrl(url, schema));
                                });
                    }
                });
    }

    /** 复制并自定义 ReadWriteConfig */
    public static GirRdDataSourceProperties copy(
            GirRdDataSourceProperties source,
            Consumer<GirRdDataSourceProperties.ReadWriteConfig> readWriteConfigConsumer) {
        return copyWithCustomization(source, readWriteConfigConsumer);
    }

    /** 复制 ReadWriteConfig（内部使用，不对外暴露） */
    private static GirRdDataSourceProperties.ReadWriteConfig copyReadWriteConfig(
            GirRdDataSourceProperties.ReadWriteConfig source) {

        if (source == null) {
            return null;
        }

        GirRdDataSourceProperties.ReadWriteConfig target =
                new GirRdDataSourceProperties.ReadWriteConfig();
        BeanUtil.copyProperties(source, target);

        // 复制从库列表
        List<ReadDataSourceConfig> sourceSlaves = source.getReadDataSourceConfigs();
        if (sourceSlaves != null && !sourceSlaves.isEmpty()) {
            List<ReadDataSourceConfig> targetSlaves =
                    sourceSlaves
                            .stream()
                            .map(slave -> copyReadDataSourceConfig(slave, null))
                            .collect(Collectors.toList());
            target.setReadDataSources(targetSlaves);
        }

        return target;
    }

    // ==================== URL 工具方法 ====================

    /** 为 URL 添加 schema 参数 */
    public static String appendSchemaToUrl(String url, String schema) {
        if (url == null || url.trim().isEmpty() || schema == null || schema.trim().isEmpty()) {
            return url;
        }

        // 如果 URL 已经包含 currentSchema，先移除旧的
        if (url.contains("currentSchema=")) {
            url = removeParameterFromUrl(url, "currentSchema");
        }

        // 添加新的 schema 参数
        if (url.contains("?")) {
            return url + "&currentSchema=" + schema;
        } else {
            return url + "?currentSchema=" + schema;
        }
    }

    /** 从 URL 中移除指定参数 */
    public static String removeParameterFromUrl(String url, String paramName) {
        if (url == null || !url.contains(paramName + "=")) {
            return url;
        }
        String pattern = "[&?]" + paramName + "=[^&]*";
        String result = url.replaceAll(pattern, "");
        if (result.endsWith("?")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
