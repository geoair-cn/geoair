package cn.geoair.map.tile.forge.fuser.utils;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.tile.forge.fuser.CustomTileCacheHelper;
import cn.geoair.map.tile.forge.fuser.GirFuserLayerTileHelper;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.constant.Constant;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.fuser.CacheTileFuserExec;
import cn.geoair.map.tile.forge.fuser.fuser.FuserExec;
import cn.geoair.web.mime.GirImageMime;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/22 12:13
 * @description： TODO
 */
public class FuserCacheUtils {
    private static GiLogger log = GirLoggerFactory.getLogger();

    /** 原始网格名称后缀 */
    public static final String ORIGINAL_GRID_SUFFIX = Constant._original_grid_name_suffix;

    /** 保存到mbtiles的时候，判断是否需要翻转 Y */
    public static boolean mbtilesCheckIsNeedReverseY(String layerName) {
        try {
            PxyLayerInfo pxyLayerInfo =
                    GirFuserLayerTileHelper.getInstance().getPxyLayerInfo(layerName);
            if (pxyLayerInfo != null) {
                return pxyLayerInfo.getTileRowOriginEnums().isTopLeft();
            }
        } catch (Exception e) {
            log.debug("获取图层 {} 的行原点失败，默认不翻转", layerName);
        }
        // 默认不翻转
        return false;
    }

    /** 保存到文件的时候，判断是否需要翻转 Y */
    public static boolean fileCheckIsNeedReverseY(String layerName) {
        // 保持既有文件缓存布局：其行号方向与 MBTiles 缓存布局相反。
        return !mbtilesCheckIsNeedReverseY(layerName);
    }

    /**
     * @deprecated 未提供 gridSrid 时只能按历史行为使用 3857 网格。请使用带 gridSrid 的重载。
     */
    @Deprecated
    public static int getStoreY(int z, int y, boolean needReverse) {
        return getStoreY(z, y, needReverse, 3857);
    }

    /** 根据指定网格转换 Y 行号。 */
    public static int getStoreY(int z, int y, boolean needReverse, Integer gridSrid) {
        if (needReverse) {
            return reverseY(z, y, gridSrid);
        }
        return y;
    }

    /** 将内部 TMS（bottom-left）行号转换为图层源的行号。 */
    public static int getSourceY(PxyLayerInfo layerInfo, int z, int y) {
        if (layerInfo == null || !layerInfo.getTileRowOriginEnums().isTopLeft()) {
            return y;
        }
        return reverseY(z, y, layerInfo.getGridSrid());
    }

    /**
     * 获取缓存行号转换所用的网格。
     *
     * <p>未配置 tileRowOrigin 的旧图层固定返回 3857，以保持既有缓存布局； 显式使用新字段后才按 gridSrid 计算。原始缓存名称无法反查时同样按历史默认值处理。
     */
    public static int getCacheGridSrid(String layerName) {
        try {
            PxyLayerInfo layerInfo =
                    GirFuserLayerTileHelper.getInstance().getPxyLayerInfo(layerName);
            if (layerInfo == null
                    && layerName != null
                    && layerName.endsWith(ORIGINAL_GRID_SUFFIX)) {
                String sourceLayerName =
                        layerName.substring(0, layerName.length() - ORIGINAL_GRID_SUFFIX.length());
                layerInfo = GirFuserLayerTileHelper.getInstance().getPxyLayerInfo(sourceLayerName);
            }
            if (layerInfo != null
                    && layerInfo.isTileRowOriginConfigured()
                    && layerInfo.getGridSrid() != null) {
                return layerInfo.getGridSrid();
            }
        } catch (Exception e) {
            log.debug("获取图层 {} 的 gridSrid 失败，使用历史默认值 3857", layerName);
        }
        return 3857;
    }

    /** 获取单个图层用于缓存行号转换的网格。 */
    public static int getCacheGridSrid(PxyLayerInfo layerInfo) {
        if (layerInfo != null
                && layerInfo.isTileRowOriginConfigured()
                && layerInfo.getGridSrid() != null) {
            return layerInfo.getGridSrid();
        }
        return 3857;
    }

    private static int reverseY(int z, int y, Integer gridSrid) {
        if (gridSrid != null && (gridSrid == 3857 || gridSrid == 900913)) {
            return GirAdvTools.getTileGrid3857Opt().convertY(z, y, TileYAxis.XYZ, TileYAxis.TMS);
        }
        return GirAdvTools.getTileGrid4326SeparateOpt()
                .convertY(z, y, TileYAxis.XYZ, TileYAxis.TMS);
    }

    /**
     * 删除缓存，通过请求的网格zxy
     *
     * @param layerName 图层名称
     * @param imageFormat 图片格式
     */
    public static void deleteCacheByRequestGrid(
            String layerName,
            Integer z,
            Integer x,
            Integer y,
            CacheTileFuserExec cacheTileFuser,
            GirImageMime imageFormat) {
        // 删除当前瓦片缓存
        cacheTileFuser.delCache(z, x, y);

        // 删除瓦片原始缓存
        deleteOriginalGridCache(layerName, cacheTileFuser, imageFormat);
    }

    /**
     * 删除瓦片原始缓存
     *
     * @param layerName 图层名称
     * @param cacheFuser 缓存融合执行器
     * @param imageFormat 图片格式
     */
    public static void deleteOriginalGridCache(
            String layerName, FuserExec cacheFuser, GirImageMime imageFormat) {
        if (!(cacheFuser instanceof CacheTileFuserExec)) {
            return;
        }

        TileCache tileCache =
                CustomTileCacheHelper.getInstance().getTileCache(layerName + ORIGINAL_GRID_SUFFIX);

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
                boolean deleted =
                        tileCache.delete(layerName + ORIGINAL_GRID_SUFFIX, z, i, j, imageFormat);
                if (deleted) {
                    deletedCount++;
                }
            }
        }

        if (deletedCount > 0) {
            log.info(
                    "删除周边瓦片缓存: layer={}, z={}, 范围=[{}-{}][{}-{}], 删除数量={}",
                    layerName,
                    z,
                    minX,
                    maxX,
                    minY,
                    maxY,
                    deletedCount);
        }
    }
}
