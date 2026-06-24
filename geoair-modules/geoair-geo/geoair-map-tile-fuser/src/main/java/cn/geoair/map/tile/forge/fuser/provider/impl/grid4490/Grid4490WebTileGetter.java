package cn.geoair.map.tile.forge.fuser.provider.impl.grid4490;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.utils.HttpTileRequestUtils;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.OriginType;
import cn.geoair.map.tile.forge.fuser.provider.BaseTileGetter;
import lombok.extern.slf4j.Slf4j;

import java.net.Proxy;

/**
 * 谷歌网络瓦片获取器
 *
 * @author 张俊
 * @date Created in 2026/5/9 14:10
 */
@Slf4j
public class Grid4490WebTileGetter extends BaseTileGetter {

    protected final String urlTemplate;

    protected final Proxy proxy;
    protected final int connectionTimeout;
    protected final int readTimeout;
    protected final int totalTimeout;


    public Grid4490WebTileGetter(PxyLayerInfo config) {
        this(config, 60*1000*1, 60*1000*3, 60*1000*5);
    }

    public Grid4490WebTileGetter(PxyLayerInfo config, int connectionTimeout, int readTimeout, int totalTimeout) {
        super(config);
        this.urlTemplate = config.getPath();
        this.proxy = HttpTileRequestUtils.getHttpProxy(config);
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.totalTimeout = totalTimeout;
    }


    @Override
    public Resource getTileResource(int z, int x, int y) {
        OriginType originType = OriginType.fromMode(getLayerInfo().getOriginType());
        if (originType.isGoogle()) {
            y = GirAdvTools.getTileGrid4326SeparateOpt().reverseY(y, z);
        }
        String httpUrl = urlTemplate.replace("{z}", String.valueOf(z))
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y));

        String logContext = String.format("(%d,%d,%d)", z, x, y);

        // 使用工具类请求瓦片（带重试）
        return HttpTileRequestUtils.requestTileWithRetry(
                httpUrl,
                proxy,
                totalTimeout,
                3,
                1,
                3,
                getSrcFormat(),
                logContext
        );
    }
}
