package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.fuser.CustomTileCacheHelper;
import cn.geoair.map.tile.forge.fuser.GirFuserLayerTileHelper;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.constant.Constant;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.OriginType;
import cn.geoair.map.tile.forge.fuser.fuser.CacheTileFuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.FuserExec;


/**
 * @author ：张俊
 * @date ：Created in 2026/6/22 12:13
 * @description： TODO
 */

public class FuserCacheUtils {
    private static GiLogger log = GirLoggerFactory.getLogger();
    /**
     * 原始网格名称后缀
     */
    public static final String ORIGINAL_GRID_SUFFIX = Constant._original_grid_name_suffix;


    /**
     * 保存到mbtiles的时候，判断是否需要翻转 Y
     */
    public static boolean mbtilesCheckIsNeedReverseY(String layerName) {
        try {
            PxyLayerInfo pxyLayerInfo = GirFuserLayerTileHelper.getInstance().getPxyLayerInfo(layerName);
            if (pxyLayerInfo != null) {
                String originTypeStr = pxyLayerInfo.getOriginType();
                OriginType originType = OriginType.fromMode(originTypeStr);
                // Google 坐标系需要翻转 Y（TMS 风格）
                return originType.isGoogle();
            }
        } catch (Exception e) {
            log.debug("获取图层 {} 的 OriginType 失败，默认不翻转", layerName);
        }
        // 默认不翻转
        return false;
    }

    /**
     * 保存到文件的时候，判断是否需要翻转 Y
     */
    public static boolean fileCheckIsNeedReverseY(String layerName) {
        //layerName请求找缓存的时候，用的是 google原点
        // layerName_orgin_grid_请求找缓存的时候，用的是tms原点
//        try {
//            PxyLayerInfo pxyLayerInfo = GirFuserLayerTileHelper.getInstance().getPxyLayerInfo(layerName);
//            if (pxyLayerInfo != null) {
//                String originTypeStr = pxyLayerInfo.getOriginType();
//                OriginType originType = OriginType.fromMode(originTypeStr);
//                // Google 坐标系需要翻转 Y（TMS 风格）
//                return !originType.isGoogle();
//            }
//        } catch (Exception e) {
//            log.debug("获取图层 {} 的 OriginType 失败，默认不翻转", layerName);
//        }
//        // 默认翻转
//        return true;
        return !mbtilesCheckIsNeedReverseY(layerName);
    }

    /**
     * XYZ → TMS Y 转换（如果需要）
     *
     * @param z           层级
     * @param y           原始 Y 坐标
     * @param needReverse 是否需要翻转
     * @return 转换后的 Y 坐标
     */
    public static int getStoreY(int z, int y, boolean needReverse) {
        if (needReverse) {
            return GirAdvTools.getTileGrid3857Opt().reverseY(y, z);  // 这里使用3857的网格翻转逻辑来进行翻转Y，不进行判断43426的网格原因是因为mbtile规范并不支持4326网格，这里在4326网格的时候就把mbtiles当做一个存储器
        }
        return y;
    }


    /**
     * 删除缓存，通过请求的网格zxy
     *
     * @param layerName   图层名称
     * @param imageFormat 图片格式
     */
    public static void deleteCacheByRequestGrid(String layerName, Integer z, Integer x, Integer y, CacheTileFuserExec cacheTileFuser, ImageMime imageFormat) {
        // 删除当前瓦片缓存
        cacheTileFuser.delCache(z, x, y);

        // 删除瓦片原始缓存
        deleteOriginalGridCache(layerName, cacheTileFuser, imageFormat);
    }

    /**
     * 删除瓦片原始缓存
     *
     * @param layerName   图层名称
     * @param cacheFuser  缓存融合执行器
     * @param imageFormat 图片格式
     */
    public static void deleteOriginalGridCache(String layerName, FuserExec cacheFuser, ImageMime imageFormat) {
        if (!(cacheFuser instanceof CacheTileFuserExec)) {
            return;
        }

        TileCache tileCache = CustomTileCacheHelper.getInstance()
                .getTileCache(layerName + ORIGINAL_GRID_SUFFIX);

        if (tileCache == null) {
            log.warn("获取瓦片缓存失败: layer={}", layerName + ORIGINAL_GRID_SUFFIX);
            return;
        }

        RangeApo srcRange = cacheFuser.getSrcRange();
        int z = srcRange.getZ();
        int minX = srcRange.getMinX();
        int maxX = srcRange.getMaxX();
        int minY = srcRange.getMinY();
        int maxY = srcRange.getMaxY();

        int deletedCount = 0;
        for (int i = minX; i <= maxX; i++) {
            for (int j = minY; j <= maxY; j++) {
                boolean deleted = tileCache.delete(layerName + ORIGINAL_GRID_SUFFIX, z, i, j, imageFormat);
                if (deleted) {
                    deletedCount++;
                }
            }
        }

        if (deletedCount > 0) {
            log.info("删除周边瓦片缓存: layer={}, z={}, 范围=[{}-{}][{}-{}], 删除数量={}",
                    layerName, z, minX, maxX, minY, maxY, deletedCount);
        }
    }
}
