package cn.geoair.map.tile.forge.fuser.provider.util;

import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/16 09:20
 * @description： TODO
 */
public class WebPxyUtils {

    public static   Proxy getHttpProxy(PxyLayerInfo config) {
        Proxy proxy = null;
        if ("true".equalsIgnoreCase(config.getUseWebPxy())
                && config.getWebPxyHost() != null
                && config.getWebPxyPort() != null) {
            proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(config.getWebPxyHost(), config.getWebPxyPort()));
        }
        return proxy;
    }

}
