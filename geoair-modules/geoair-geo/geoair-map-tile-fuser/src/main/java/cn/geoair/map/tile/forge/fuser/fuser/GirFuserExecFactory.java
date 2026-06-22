package cn.geoair.map.tile.forge.fuser.fuser;


import cn.geoair.base.exception.GirException;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.fuser.CustomTileCacheHelper;
import cn.geoair.map.tile.forge.fuser.GirFuser;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import lombok.extern.slf4j.Slf4j;


/**
 * 瓦片融合器工厂类
 * 用于创建带缓存的瓦片融合器实例
 *
 * @author 张俊
 * @date Created in 2026/6/15
 */
@Slf4j
public class GirFuserExecFactory {

    /**
     * 默认瓦片尺寸
     */
    private static final int DEFAULT_TILE_SIZE = 256;

    /**
     * 默认输出格式
     */
    private static final ImageMime DEFAULT_OUTPUT_FORMAT = ImageMime.png;

    /**
     * 创建带缓存的瓦片融合器（使用默认参数）
     *
     * @param layerName 图层名称
     * @param z         zoom等级
     * @param x         x坐标
     * @param y         y坐标
     * @param bounds    边界范围
     * @return 带缓存的瓦片融合器
     */
    public static FuserExec createCachedFuser(String layerName, Integer z, Integer x, Integer y, BoundingBox bounds) {
        return createCachedFuser(layerName, z, x, y, bounds, DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE, DEFAULT_OUTPUT_FORMAT);
    }

    /**
     * 创建带缓存的瓦片融合器
     *
     * @param layerName    图层名称
     * @param z            zoom等级
     * @param x            x坐标
     * @param y            y坐标
     * @param bounds       边界范围
     * @param width        输出宽度
     * @param height       输出高度
     * @param outputFormat 输出格式
     * @return 带缓存的瓦片融合器
     */
    public static CacheTileFuserExec createCachedFuser(String layerName, Integer z, Integer x, Integer y,
                                                       BoundingBox bounds, int width, int height, ImageMime outputFormat) {
        // 获取瓦片获取器
        LayerTileGetter layerTileGetter = GirFuser.getLayerTileGetter(layerName);

        if (layerTileGetter == null) {
            log.error("获取LayerTileGetter失败: layerName={}", layerName);
            throw new GirException("图层不存在，图层名称: " + layerName);
        }

        // 创建GtcTileFuser
        FuserExec tileFuser = new GirFuserExec(layerTileGetter, outputFormat, bounds, width, height);

        // 获取缓存实例
        TileCache tileCache = CustomTileCacheHelper.getInstance().getTileCache(layerName);

        // 创建带缓存的融合器
        return new CacheTileFuserExec(tileFuser, tileCache, layerName, z, x, y);
    }

    /**
     * 创建不带缓存的瓦片融合器
     *
     * @param layerName    图层名称
     * @param bounds       边界范围
     * @param width        输出宽度
     * @param height       输出高度
     * @param outputFormat 输出格式
     * @return 瓦片融合器
     */
    public static FuserExec createFuser(String layerName, BoundingBox bounds,
                                        int width, int height, ImageMime outputFormat) {
        LayerTileGetter layerTileGetter = GirFuser.getLayerTileGetter(layerName);

        if (layerTileGetter == null) {
            log.error("获取LayerTileGetter失败: layerName={}", layerName);
            throw new IllegalArgumentException("LayerTileGetter创建失败");
        }

        return new GirFuserExec(layerTileGetter, outputFormat, bounds, width, height);
    }

    /**
     * 创建不带缓存的瓦片融合器（使用默认参数）
     *
     * @param layerName 图层名称
     * @param bounds    边界范围
     * @return 瓦片融合器
     */
    public static FuserExec createFuser(String layerName, BoundingBox bounds) {
        return createFuser(layerName, bounds, DEFAULT_TILE_SIZE, DEFAULT_TILE_SIZE, DEFAULT_OUTPUT_FORMAT);
    }
}
