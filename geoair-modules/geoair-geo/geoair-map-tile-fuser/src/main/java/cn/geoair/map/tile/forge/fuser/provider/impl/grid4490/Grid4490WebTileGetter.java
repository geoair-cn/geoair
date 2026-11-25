package cn.geoair.map.tile.forge.fuser.provider.impl.grid4490;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.utils.HttpTileRequestUtils;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.provider.BaseTileGetter;

import java.net.Proxy;
import cn.geoair.map.tile.forge.fuser.utils.LayerHttpTileClient;

/**
 * 谷歌网络瓦片获取器
 *
 * @author 张俊
 * @date Created in 2026/5/9 14:10
 */

public class Grid4490WebTileGetter extends BaseTileGetter {
    private static GiLogger log = GirLoggerFactory.getLogger();
    protected final String urlTemplate;

    protected final Proxy proxy;
    protected final int connectionTimeout;
    protected final int readTimeout;
    protected final int totalTimeout;


    public Grid4490WebTileGetter(PxyLayerInfo config) {
        this(config, 60 * 1000 * 1, 60 * 1000 * 3, 60 * 1000 * 5);
    }

    public Grid4490WebTileGetter(PxyLayerInfo layerInfo, int connectionTimeout, int readTimeout, int totalTimeout) {
        super(layerInfo);
        this.urlTemplate = layerInfo.getPath();
        this.proxy = HttpTileRequestUtils.getHttpProxy(layerInfo);
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.totalTimeout = totalTimeout;
    }


    @Override
    public Resource getTileResource(int z, int x, int y) {
        y = FuserCacheUtils.getSourceY(getLayerInfo(), z, y);
        String httpUrl = urlTemplate.replace("{z}", String.valueOf(z))
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y));

        return LayerHttpTileClient.fetch(getLayerInfo(), httpUrl, z, x, y, getSrcFormat(), totalTimeout);
    }
}
