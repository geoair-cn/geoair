package cn.geoair.map.dynamic.tools.grid.converter;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileLevelMetadata;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * WGS84（EPSG:4326）线性瓦片网格的公共实现。
 *
 * <p>子类定义经纬度方向的瓦片跨度和行列数量；本类负责范围换算、坐标系转换及层级元数据。 {@link #MAX_VALID_LAT} 只适用于等轴网格为兼容 Web Mercator
 * 所作的纬度裁剪，Separate 网格可覆盖完整的 {@code [-90°, 90°]}。
 *
 * @author 张逢吉
 */
public abstract class AbstractWgs84TileConverter extends TileConverterCommon {

    // 公共常量（4326坐标系基础参数）
    protected static final double MIN_LON = -180.0;

    protected static final double MAX_LON = 180.0;

    protected static final double MIN_LAT = -90.0;

    protected static final double MAX_LAT = 90.0;

    protected static final double MAX_VALID_LAT = 85.0511287798; // 3857有效纬度上限

    protected static final double MIN_VALID_LAT = -85.0511287798; // 3857有效纬度下限

    protected static final double PRECISION = 1e-9; // 浮点精度补偿

    // 地球周长（米）- 用于比例尺计算
    protected static final double EARTH_CIRCUMFERENCE = 40075016.686;
    // 墨卡托投影常量（地球半径）
    private static final double EARTH_RADIUS = 6378137.0;

    public static final double EPSG4326_TO_METERS = 6378137.0 * 2.0 * Math.PI / 360.0;

    public AbstractWgs84TileConverter(ToolsConfig advToolsConfig) {
        super(advToolsConfig);
    }

    /** 数值范围限制（工具方法） */
    protected double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /** 将几何图形从源坐标系转换为WGS84(4326) */
    protected Geometry transform(Geometry geometry, int srcSrid) {
        if (geometry == null || geometry.isEmpty()) return null;
        return sridConvertOpt.convert(geometry, srcSrid, 4326);
    }

    /**
     * 将当前 Separate 网格的 Y 行号转换为 Equal 网格的 Y 行号。
     *
     * <p>当前两种 EPSG:4326 网格使用相同的实际 Y 行定义（z=3 均为 4 行）和相同的 行号方向，因此映射是一一对应的。该实现通过层级元数据校验行号，不再采用旧的
     * {@code 2^z} 行公式。{@code roundingType} 因不存在小数行号而不参与计算，仅为保持 原方法签名而保留。
     *
     * @param separateAxisY Separate 网格的 XYZ Y 行号
     * @param zoom 缩放级别（0～22）
     * @param roundingType 兼容参数；当前网格映射中不参与计算
     * @return 对应的 Equal 网格 XYZ Y 行号
     * @throws IllegalArgumentException 行号或层级不合法时抛出
     */
    public int convertSeparateAxisYToEqualAxisY(
            int separateAxisY, int zoom, RoundingType roundingType) {
        validateCurrent4326Y(separateAxisY, zoom, "Separate");
        return separateAxisY;
    }

    /** 取整方式枚举（便于明确业务规则） */
    public enum RoundingType {
        FLOOR, // 向下取整
        CEIL, // 向上取整
        ROUND // 四舍五入
    }

    /**
     * 将当前 Equal 网格的 Y 行号转换为 Separate 网格的 Y 行号。
     *
     * <p>当前两种 EPSG:4326 网格在同一层级的实际 Y 行数相同，故该映射与 {@link #convertSeparateAxisYToEqualAxisY(int, int,
     * RoundingType)} 严格互逆。 {@code roundingType} 仅为兼容原方法签名而保留。
     *
     * @param equalAxisY Equal 网格的 XYZ Y 行号
     * @param zoom 缩放级别（0～22）
     * @param roundingType 兼容参数；当前网格映射中不参与计算
     * @return 对应的 Separate 网格 XYZ Y 行号
     * @throws IllegalArgumentException 行号或层级不合法时抛出
     */
    public int convertEqualAxisYToSeparateAxisY(
            int equalAxisY, int zoom, RoundingType roundingType) {
        validateCurrent4326Y(equalAxisY, zoom, "Equal");
        return equalAxisY;
    }

    /** 校验当前 EPSG:4326 网格中可用的 XYZ Y 行号。 */
    private void validateCurrent4326Y(int y, int zoom, String gridName) {
        // getTileRowCount 会同时校验当前实现支持的层级范围，并读取真实网格行数。
        int rowCount = getTileRowCount(zoom);
        if (y < 0 || y >= rowCount) {
            throw new IllegalArgumentException(
                    String.format(
                            "%s网格Y索引不合法：Y=%d, zoom=%d（合法范围0~%d）", gridName, y, zoom, rowCount - 1));
        }
    }

    // ========== 子类需实现的差异化核心方法 ==========

    /** 计算经度方向单瓦片跨度，单位为度。 */
    protected abstract double calculateTileLonSpan(int z);

    /**
     * 计算纬度方向单瓦片跨度，单位为度。
     *
     * <p>当前等轴和 Separate 实现均返回 {@code 360 / 2^z}；两者的差异体现在实际 行数与纬度裁剪策略，而不是这里的数值公式。
     */
    protected abstract double calculateTileLatSpan(int z);

    /**
     * 根据最大分辨率层级获取瓦片元数据（支持自定义瓦片尺寸和DPI）
     *
     * @param maxZoom 最大分辨率层级（最大缩放级别）
     * @param tilePixelSize 瓦片像素尺寸（例如：256、512）
     * @param dpi 屏幕DPI（例如：72、96、300）
     * @return 瓦片层级元数据对象
     */
    public TileLevelMetadata getTileLevelMetadata(int maxZoom, int tilePixelSize, double dpi) {
        validateXyz(maxZoom, 0, 0);

        Double tileWidth = Math.pow(2, maxZoom);
        // z=0 时仍至少应存在一行瓦片，避免 TMS/XYZ 行号转换出现零行网格。
        Double tileHeight = Math.max(1D, tileWidth / 2D);
        Double totalTiles = tileHeight * tileWidth;

        // 计算经度和纬度的瓦片跨度（度）
        double tileLonSpan = calculateTileLonSpan(maxZoom);
        // 瓦片的地理尺寸（度）
        double tileGeoWidth = tileLonSpan;
        // 计算地面分辨率（度/像素）
        double groundResolutionDegree = getResolution(maxZoom, tilePixelSize);

        double scale = groundResolutionDegree * EPSG4326_TO_METERS / (0.0254 / dpi);
        // 计算每像素代表的实际长度（毫米）
        //        double mmPerPixel = groundResolutionDegree * 1000;
        double mmPerPixel =
                ((2 * Math.PI * EARTH_RADIUS) / (Math.pow(2, maxZoom) * tilePixelSize)) * 1000;
        // 全局范围（4326坐标系）
        Envelope extent = new Envelope(MIN_LON, MAX_LON, MIN_LAT, MAX_LAT);

        return new TileLevelMetadata(
                maxZoom,
                tileWidth.intValue(),
                tileHeight.intValue(),
                tileGeoWidth,
                mmPerPixel,
                groundResolutionDegree,
                scale,
                totalTiles.intValue(),
                tilePixelSize,
                dpi,
                mmPerPixel,
                extent,
                "EPSG:4326");
    }

    /** 获取指定层级的度/像素分辨率 */
    public double getResolution(int zoom, int tilePixelSize) {
        validateXyz(zoom, 0, 0);
        double tileLonSpan = calculateTileLonSpan(zoom);
        return tileLonSpan / tilePixelSize;
    }

    /**
     * 根据最大分辨率层级获取瓦片元数据（使用默认配置）
     *
     * @param maxZoom 最大分辨率层级
     * @return 瓦片层级元数据对象
     */
    public TileLevelMetadata getTileLevelMetadata(int maxZoom) {
        int defaultTileSize =
                advToolsConfig.getTilePixelSize() > 0 ? advToolsConfig.getTilePixelSize() : 256;
        int defaultDpi = advToolsConfig.getDpi() > 0 ? advToolsConfig.getDpi() : 96;
        return getTileLevelMetadata(maxZoom, defaultTileSize, defaultDpi);
    }

    /**
     * 批量获取多个层级的瓦片元数据
     *
     * @param minZoom 最小层级
     * @param maxZoom 最大层级
     * @param tilePixelSize 瓦片像素尺寸
     * @param dpi 屏幕DPI
     * @return 层级元数据列表
     */
    public List<TileLevelMetadata> getTileLevelMetadataList(
            int minZoom, int maxZoom, int tilePixelSize, double dpi) {
        if (minZoom < 0 || maxZoom < minZoom) {
            throw new IllegalArgumentException(
                    "层级参数无效: minZoom=" + minZoom + ", maxZoom=" + maxZoom);
        }

        List<TileLevelMetadata> metadataList = new ArrayList<>();
        for (int z = minZoom; z <= maxZoom; z++) {
            metadataList.add(getTileLevelMetadata(z, tilePixelSize, dpi));
        }
        return metadataList;
    }

    /**
     * 根据地面分辨率反推合适的瓦片层级
     *
     * @param targetResolution 目标地面分辨率（米/像素）
     * @param tilePixelSize 瓦片像素尺寸
     * @return 最合适的瓦片层级
     */
    public int getZoomByResolution(double targetResolution, int tilePixelSize) {
        if (targetResolution <= 0) {
            throw new IllegalArgumentException("分辨率必须大于0");
        }

        // 计算每个层级的度/像素分辨率，找到最接近的
        for (int z = 0; z <= 22; z++) {
            double resolutionDegree = getResolution(z, tilePixelSize); // 度/像素

            if (resolutionDegree <= targetResolution) {
                return z;
            }
        }
        return 22;
    }

    /**
     * 根据比例尺反推合适的瓦片层级
     *
     * @param targetScale 目标比例尺（例如：10000 表示 1:10000）
     * @param tilePixelSize 瓦片像素尺寸
     * @param dpi 屏幕DPI
     * @return 最合适的瓦片层级
     */
    public int getZoomByScale(double targetScale, int tilePixelSize, double dpi) {
        if (targetScale <= 0) {
            throw new IllegalArgumentException("比例尺必须大于0");
        }

        // 根据比例尺计算地面分辨率
        // 公式：Resolution = Scale * 0.0254 / DPI
        double targetResolution = targetScale * 0.0254 / dpi;

        return getZoomByResolution(targetResolution, tilePixelSize);
    }

    @Override
    public BoxReferencedEnvelope boundsFromTileRange(
            long minTileX, long maxTileX, long minTileY, long maxTileY, int zoom, int targetSrid) {
        validateXyz(zoom, (int) minTileX, (int) minTileY);
        // 计算四个角的瓦片边界
        // 左下角瓦片
        double minX = tileXToCoordinateX((int) minTileX, zoom);
        double minY = tileYToCoordinateY((int) (maxTileY), zoom); // 注意Y轴方向

        // 右上角瓦片
        double maxX = tileXToCoordinateX((int) (maxTileX), zoom);
        double maxY = tileYToCoordinateY((int) minTileY, zoom);

        Envelope envelope = new Envelope(minX, maxX, minY, maxY);
        Envelope converted = sridConvertOpt.convert(envelope, 4326, targetSrid);
        return new BoxReferencedEnvelope(converted, targetSrid);
    }

    @Override
    public RangeApo tileRangeByBox(int z, Envelope tileBox, int srcSrid) {
        Envelope convert = sridConvertOpt.convert(tileBox, srcSrid, 4326);
        return tileRangeByBox(z, convert);
    }

    @Override
    public RangeApo tileRangeByGeom(int z, Geometry geometry, int srcSrid) {
        Geometry transform = transform(geometry, srcSrid);
        return tileRangeByGeom(z, transform);
    }
}
