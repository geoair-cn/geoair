package cn.geoair.comp.dynamic.ds.readwrite.spring;

import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 读写分离数据源配置
 * 配置前缀：spring.datasource.gir
 *
 * @author 张俊
 * @date Created in 2023/5/31 15:27
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource.geoair")
@Data
public class GirRdDataSourceProperties {

    /**
     * 读库组名称
     */
    private String groupName = "defaultRdGroup";
    /**
     * 读写分离配置
     */
    private ReadWriteConfig readwrite = new ReadWriteConfig();

    @Data
    public static class ReadWriteConfig {
        /**
         * 是否启用
         */
        private boolean enabled = false;
        /**
         * 读库的数据源列表，用逗号分割
         */
        private String readUrls;


        /**
         * 读库负载策略
         */
        private LoadStrategyType readStrategy = LoadStrategyType.RANDOM;


        /**
         * 权重配置（用于 WEIGHT 策略）
         * 格式：url:weight
         */
        private java.util.Map<String, Integer> weights;

        public List<String> getReadUrlList() {
            if (readUrls == null || readUrls.trim().isEmpty()) {
                return Collections.emptyList();
            }
            return Arrays.asList(readUrls.split(","));
        }
    }
}
