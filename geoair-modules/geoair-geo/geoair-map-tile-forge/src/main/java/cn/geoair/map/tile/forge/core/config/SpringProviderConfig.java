package cn.geoair.map.tile.forge.core.config;

import cn.geoair.map.tile.forge.core.bygwc.ProviderConfig;
import cn.hutool.extra.spring.SpringUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ：张俊
 * @date ：Created in 2025/11/19 17:14
 * @description：服务提供者配置类，用于读取geoair.provider前缀的配置属性
 */
@Data
@ConfigurationProperties(prefix = "geoair.provider")
public class SpringProviderConfig extends ProviderConfig {

    public SpringProviderConfig() {
        instance = this;
    }

    /**
     * 配置实例对象
     */
    static SpringProviderConfig instance = null;

    /**
     * 获取ProviderConfig单例实例
     *
     * @return 返回ProviderConfig实例
     */
    public static SpringProviderConfig getInstance() {
        return instance = instance == null ? SpringUtil.getBean(SpringProviderConfig.class) : instance;
    }


}
