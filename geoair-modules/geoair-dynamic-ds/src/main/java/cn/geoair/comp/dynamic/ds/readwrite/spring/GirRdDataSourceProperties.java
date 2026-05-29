package cn.geoair.comp.dynamic.ds.readwrite.spring;

import cn.geoair.comp.dynamic.ds.readwrite.enums.LoadStrategyType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 读写分离数据源配置
 * 配置前缀：spring.datasource.gir
 *
 * @author 张俊
 * @date Created in 2023/5/31 15:27
 */
@Component
@ConfigurationProperties(prefix = "spring.datasource.gir")
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
         * 读库（从库）URL 列表
         */
        private List<String> readUrls;

        /**
         * 读库负载策略
         */
        private LoadStrategyType readStrategy = LoadStrategyType.RANDOM;



        /**
         * 权重配置（用于 WEIGHT 策略）
         * 格式：url:weight
         */
        private java.util.Map<String, Integer> weights;
    }
}
