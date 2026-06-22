package cn.geoair.map.tile.forge.fuser.provider.google;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.OriginType;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.provider.BaseTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.util.WebPxyUtils;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.Proxy;

/**
 * 谷歌网络瓦片获取器
 *
 * @author 张俊
 * @date Created in 2026/5/9 14:10
 */
@Slf4j
public class GoogleWebTileGetter extends BaseTileGetter {

    protected final String urlTemplate;

    protected final Proxy proxy;
    protected final int connectionTimeout;
    protected final int readTimeout;
    protected final int totalTimeout;


    public GoogleWebTileGetter(PxyLayerInfo config) {
        this(config, 13000, 15000, 15000);
    }

    public GoogleWebTileGetter(PxyLayerInfo config, int connectionTimeout, int readTimeout, int totalTimeout) {
        super(config);
        this.urlTemplate = config.getPath();
        this.proxy = WebPxyUtils.getHttpProxy(config);
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
        this.totalTimeout = totalTimeout;
    }


    @Override
    public Resource getTileResource(int z, int x, int y) {
        OriginType originType = OriginType.fromMode(getLayerInfo().getOriginType());
        if (originType.isGoogle()) {
            y = GirAdvTools.getTileGrid3857Opt().reverseY(y, z);
        }
        String httpUrl = urlTemplate.replace("{z}", String.valueOf(z))
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y));

        HttpResponse response = null;
        try {
            HttpRequest get = HttpUtil.createGet(httpUrl)
                    .setFollowRedirects(true)
                    .timeout(totalTimeout)
                    .setConnectionTimeout(connectionTimeout)
                    .setReadTimeout(readTimeout)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-US;q=0.7")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .header("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .header("Sec-Fetch-Dest", "image")
                    .header("Sec-Fetch-Mode", "no-cors")
                    .header("Sec-Fetch-Site", "cross-site");

            if (proxy != null) {
                get.setProxy(proxy);
            }

            response = get.execute();

            if (response.isOk() && response.bodyBytes() != null) {
                BufferedImage read = ImageIO.read(response.bodyStream());
                if (read != null) {
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        ImageMime srcFormat = getSrcFormat();
                        String internalName = srcFormat.getInternalName();
                        ImageIO.write(read, internalName, baos);
                        log.info("从网络获取瓦片成功: {} - ({},{},{})", httpUrl, z, x, y);
                        return new ByteArrayResource(baos.toByteArray());
                    }
                }
            } else {
                log.debug("请求远程瓦片失败 z:{}, x:{}, y:{},code:{},message:{}", z, x, y, response.getStatus(), response.body());
            }
        } catch (Exception e) {
            log.error("请求远程瓦片失败 z:{}, x:{}, y:{}", z, x, y, e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return null;
    }
}
