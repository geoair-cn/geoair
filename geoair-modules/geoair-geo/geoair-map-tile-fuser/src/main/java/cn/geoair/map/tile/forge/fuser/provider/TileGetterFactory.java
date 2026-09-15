package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.map.tile.forge.fuser.CustomTileCacheHelper;
import cn.geoair.map.tile.forge.fuser.CustomTileGetterHelper;
import cn.geoair.map.tile.forge.fuser.GirFuser;
import cn.geoair.map.tile.forge.fuser.cache.TileCache;
import cn.geoair.map.tile.forge.fuser.constant.Constant;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.SrcType;
import cn.geoair.map.tile.forge.fuser.provider.impl.MBTilesTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.impl.google.GoogleLocalFileTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.impl.google.GoogleWebTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.impl.grid4490.Grid4490LocalFileTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.impl.grid4490.Grid4490WebTileGetter;

/**
 * 瓦片获取器工厂类
 *
 * @author 张俊
 * @date Created in 2026/6/15
 */
public class TileGetterFactory {

    /**
     * 根据配置创建瓦片获取器（按配置决定是否带缓存）
     */
    public static LayerTileGetter create(PxyLayerInfo pxyLayerInfo) {
        return create(pxyLayerInfo, null);
    }

    /**
     * 根据图层名创建瓦片获取器（按配置决定是否带缓存）
     */
    public static LayerTileGetter create(String layerName) {
        PxyLayerInfo pxyLayerInfo = getRequiredLayerInfo(layerName);
        return create(pxyLayerInfo, null);
    }

    /**
     * 根据配置创建瓦片获取器（按配置决定是否带缓存）
     *
     * @param pxyLayerInfo 配置信息
     * @param tileCache    自定义缓存（可选）
     */
    public static LayerTileGetter create(PxyLayerInfo pxyLayerInfo, TileCache tileCache) {
        PxyLayerInfo validatedConfig = validateConfig(pxyLayerInfo);
        String layerCachePreFix = validatedConfig.getLayerName() + Constant._original_grid_name_suffix;
        return create(validatedConfig, tileCache, layerCachePreFix);
    }

    public static LayerTileGetter create(PxyLayerInfo pxyLayerInfo, TileCache tileCache, String layerCachePreFix) {
        PxyLayerInfo validatedConfig = validateConfig(pxyLayerInfo);
        LayerTileGetter realGetter = createRealGetter(validatedConfig);
        if (!isCacheConfigured(validatedConfig)) {
            return realGetter;
        }
        return new CachedTileGetter(realGetter, layerCachePreFix, tileCache);
    }

    public static LayerTileGetter create(String layerName, TileCache tileCache) {
        PxyLayerInfo pxyLayerInfo = getRequiredLayerInfo(layerName);
        return create(pxyLayerInfo, tileCache);
    }

    /**
     * 创建一个返回类型明确、并且缓存实际可用的瓦片获取器。
     *
     * <p>该方法供原始网格预缓存和检查修复任务使用。与 {@link #create(PxyLayerInfo)}
     * 不同，只要图层未开启缓存、缓存实现为空或缓存实现被禁用，本方法都会在任务启动阶段
     * 直接抛出带图层上下文的异常。</p>
     *
     * @param pxyLayerInfo     图层配置
     * @param tileCache        自定义缓存；为 {@code null} 时使用项目默认缓存
     * @param layerCachePreFix 缓存图层名
     * @return 已启用缓存的强类型获取器
     */
    public static CachedTileGetter createRequiredCached(PxyLayerInfo pxyLayerInfo,
                                                         TileCache tileCache,
                                                         String layerCachePreFix) {
        PxyLayerInfo validatedConfig = validateConfig(pxyLayerInfo);
        if (!isCacheConfigured(validatedConfig)) {
            throw invalidConfig(validatedConfig, "enableCache", validatedConfig.getEnableCache(),
                    "原始网格缓存任务要求图层开启缓存");
        }
        if (isBlank(layerCachePreFix)) {
            throw invalidConfig(validatedConfig, "cacheName", layerCachePreFix, "缓存图层名不能为空");
        }

        TileCache requiredCache = tileCache;
        if (requiredCache == null) {
            requiredCache = CustomTileCacheHelper.getInstance().getTileCache(layerCachePreFix);
        }
        if (requiredCache == null) {
            throw invalidConfig(validatedConfig, "tileCache", null, "没有可用的缓存实现");
        }
        if (!requiredCache.isEnabled()) {
            throw invalidConfig(validatedConfig, "tileCache", requiredCache.getClass().getName(),
                    "缓存实现未启用");
        }

        LayerTileGetter realGetter = createRealGetter(validatedConfig);
        return new CachedTileGetter(realGetter, layerCachePreFix, requiredCache);
    }

    /**
     * 统一校验图层来源配置。合法的旧版 {@code originType} 配置仍由
     * {@link PxyLayerInfo} 自身的兼容逻辑处理。
     *
     * @param pxyLayerInfo 图层配置
     * @return 原配置，便于调用方继续创建 Getter
     */
    public static PxyLayerInfo validateConfig(PxyLayerInfo pxyLayerInfo) {
        if (pxyLayerInfo == null) {
            throw invalidConfig(null, "pxyLayerInfo", null, "图层配置不能为空");
        }
        if (isBlank(pxyLayerInfo.getLayerName())) {
            throw invalidConfig(pxyLayerInfo, "layerName", pxyLayerInfo.getLayerName(), "图层名不能为空");
        }

        String srcTypeValue = pxyLayerInfo.getSrcType();
        if (!SrcType.isValidCode(srcTypeValue)) {
            throw invalidConfig(pxyLayerInfo, "srcType", srcTypeValue,
                    "不支持的来源类型，当前支持: " + SrcType.getImplementedCodesText());
        }

        SrcType srcType = SrcType.fromCode(srcTypeValue);
        if (srcType.isDeprecated()) {
            throw invalidConfig(pxyLayerInfo, "srcType", srcTypeValue,
                    "该来源类型尚未实现，当前支持: " + SrcType.getImplementedCodesText());
        }
        if (!srcType.isCustom() && isBlank(pxyLayerInfo.getPath())) {
            throw invalidConfig(pxyLayerInfo, "path", pxyLayerInfo.getPath(),
                    srcType.getCode() + " 来源必须配置资源路径");
        }
        return pxyLayerInfo;
    }

    /**
     * 创建真实的获取器（不带缓存）
     */
    private static LayerTileGetter createRealGetter(PxyLayerInfo pxyLayerInfo) {
        SrcType srcType = pxyLayerInfo.getSrcTypeEnums();
        if (srcType.isCustom()) {
            try {
                CustomTileGetterHelper instance = CustomTileGetterHelper.getInstance();
                LayerTileGetter customGetter = instance.getTileGetterByPxyLayerInfo(pxyLayerInfo);
                if (customGetter == null) {
                    throw new IllegalStateException("自定义 Getter 返回了 null");
                }
                return customGetter;
            } catch (Exception e) {
                throw new IllegalStateException("创建自定义瓦片 Getter 失败: layerName="
                        + printable(pxyLayerInfo.getLayerName()) + ", srcType=" + srcType.getCode(), e);
            }
        }
        if (srcType.isMbtiles()) {
            return new MBTilesTileGetter(pxyLayerInfo);
        }
        if (srcType.isLocal()) {
            if (pxyLayerInfo.isWebMercatorGrid()) {
                return new GoogleLocalFileTileGetter(pxyLayerInfo);
            }
            return new Grid4490LocalFileTileGetter(pxyLayerInfo);
        }
        if (pxyLayerInfo.isWebMercatorGrid()) {
            return new GoogleWebTileGetter(pxyLayerInfo);
        }
        return new Grid4490WebTileGetter(pxyLayerInfo);
    }

    private static PxyLayerInfo getRequiredLayerInfo(String layerName) {
        if (isBlank(layerName)) {
            throw invalidConfig(null, "layerName", layerName, "图层名不能为空");
        }
        PxyLayerInfo pxyLayerInfo = GirFuser.getPxyLayerInfo(layerName);
        if (pxyLayerInfo == null) {
            throw new IllegalArgumentException("图层来源配置不存在: layerName=" + printable(layerName));
        }
        return validateConfig(pxyLayerInfo);
    }

    private static boolean isCacheConfigured(PxyLayerInfo pxyLayerInfo) {
        return "true".equalsIgnoreCase(pxyLayerInfo.getEnableCache())
               || "1".equals(pxyLayerInfo.getEnableCache());
    }

    private static IllegalArgumentException invalidConfig(PxyLayerInfo pxyLayerInfo,
                                                           String field,
                                                           Object value,
                                                           String reason) {
        String layerName = pxyLayerInfo == null ? null : pxyLayerInfo.getLayerName();
        return new IllegalArgumentException("图层来源配置错误: layerName=" + printable(layerName)
                + ", field=" + field + ", value=" + printable(value) + ", reason=" + reason);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String printable(Object value) {
        if (value == null) {
            return "<null>";
        }
        if (value instanceof String && ((String) value).trim().isEmpty()) {
            return "<blank>";
        }
        return String.valueOf(value);
    }
}
