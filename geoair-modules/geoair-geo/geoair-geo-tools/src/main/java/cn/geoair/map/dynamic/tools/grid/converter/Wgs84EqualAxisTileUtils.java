package cn.geoair.map.dynamic.tools.grid.converter;

import cn.geoair.map.dynamic.tools.ToolsConfig;

/**
 * WGS84（4326）等轴瓦片转换实现类（已废弃）。
 *
 * <p>该实现与 {@link Wgs84SeparateAxisTileUtils} 的差别只有纬度覆盖范围，而且这个差别是错的：
 * 它把纬度裁剪到 Web Mercator 的有效纬度 {@code ±85.0511287798}，并且 {@code tileRangeByBox}
 * 的 Y 上限误用列数写成了 {@code (1 << z) - 1}（真实行数只有 {@code 2^(z-1)}），于是每一级都会
 * 产出不存在的行号，而这些行号换回纬度是零高度的空范围。</p>
 *
 * <p>WMTS 的 EPSG:4326 矩阵集（含底图在用的 {@code EPSG:4326_19}）定义的是
 * {@code 2^z} 列 × {@code 2^(z-1)} 行、TopLeftCorner 纬度 90、覆盖 {@code ±90°}，
 * 与非等轴实现逐项一致。因此本类已无独立实现，全部行为直接继承自
 * {@link Wgs84SeparateAxisTileUtils}，仅为兼容既有引用而保留。</p>
 *
 * @author 张逢吉
 * @deprecated 请改用 {@link Wgs84SeparateAxisTileUtils}；本类保留只为兼容既有引用，
 * 行为与 Separate 实现完全一致。
 */
@Deprecated
public class Wgs84EqualAxisTileUtils extends Wgs84SeparateAxisTileUtils {

    // 单例实例
    private static volatile Wgs84EqualAxisTileUtils INSTANCE;

    public Wgs84EqualAxisTileUtils(ToolsConfig advToolsConfig) {
        super(advToolsConfig);
    }

    /**
     * 双重校验锁单例
     *
     * @deprecated 请改用 {@link Wgs84SeparateAxisTileUtils#getInstance(ToolsConfig)}
     */
    @Deprecated
    public static Wgs84EqualAxisTileUtils getInstance() {
        if (INSTANCE == null) {
            synchronized (Wgs84EqualAxisTileUtils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Wgs84EqualAxisTileUtils(new ToolsConfig());
                }
            }
        }
        return INSTANCE;
    }

    /**
     * @deprecated 请改用 {@link Wgs84SeparateAxisTileUtils#getInstance(ToolsConfig)}
     */
    @Deprecated
    public static Wgs84EqualAxisTileUtils getInstance(ToolsConfig advToolsConfig) {
        return new Wgs84EqualAxisTileUtils(advToolsConfig);
    }
}
