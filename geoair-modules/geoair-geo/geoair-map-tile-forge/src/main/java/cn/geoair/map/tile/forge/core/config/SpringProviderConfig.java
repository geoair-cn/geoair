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
@Component
@ConfigurationProperties(prefix = "geoair.provider")
public class SpringProviderConfig extends ProviderConfig {
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

    /**
     * 服务提供者站点地址
     */
    private String providerSite = "XXXX";

    /**
     * 服务提供者名称
     */
    private String providerName = "http://www.XXXX.cn";
    /**
     * 服务提供者简介
     */
    private String abstractInfo = "";


}
