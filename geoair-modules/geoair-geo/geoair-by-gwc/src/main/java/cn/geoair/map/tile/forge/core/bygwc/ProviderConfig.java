package cn.geoair.map.tile.forge.core.bygwc;

import cn.geoair.base.Gir;
import lombok.Data;

/**
 * @author ：张俊
 * @date ：Created in 2025/11/19 17:14
 * @description：服务提供者配置类，用于读取geoair.provider前缀的配置属性
 */
@Data
public class ProviderConfig {
    /** 配置实例对象 */
    static ProviderConfig instance = null;

    /**
     * 获取ProviderConfig单例实例
     *
     * @return 返回ProviderConfig实例
     */
    public static ProviderConfig getInstance() {
        return instance = instance == null ? Gir.beans.getBean(ProviderConfig.class) : instance;
    }

    /** 服务提供者站点地址 */
    private String providerSite = "XXXX";

    /** 服务提供者名称 */
    private String providerName = "http://www.XXXX.cn";
    /** 服务提供者简介 */
    private String abstractInfo = "";
}
