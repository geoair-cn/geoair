package cn.geoair.map.dynamic.tools.grid.converter;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileLevelMetadata;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/** WGS84（4326）瓦片转换抽象父类 提取等轴/非等轴瓦片转换的公共逻辑，子类仅实现差异化的核心计算 */
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
     * 非等轴Y索引转换为等轴Y索引（4326坐标系）
     *
     * <p>核心逻辑： 1. 非等轴Y索引 → 对应纬度坐标（基于非等轴跨度：180/2^z） 2. 纬度坐标 → 等轴Y索引（基于等轴跨度：360/2^z）
     *
     * <p>注意：转换后的等轴Y索引可能是浮点数，需根据业务需求取整（默认向下取整）
     *
     * @param separateAxisY 非等轴Y索引（XYZ规范，原点左上角）
     * @param zoom 缩放级别（0-30）
     * @param roundingType 取整方式：FLOOR(向下取整)/CEIL(向上取整)/ROUND(四舍五入)
     * @return 等轴Y索引（XYZ规范，原点左上角）
     * @throws IllegalArgumentException 入参不合法时抛出
     */
    public int convertSeparateAxisYToEqualAxisY(
            int separateAxisY, int zoom, RoundingType roundingType) {
        // 1. 基础参数校验
        if (zoom < 0 || zoom > 30) {
            throw new IllegalArgumentException("缩放级别不合法：zoom=" + zoom + "（合法范围0-30）");
        }
        // 非等轴Y索引的合法范围：0 ~ 2^z -1
        int separateMaxY = (1 << zoom) - 1;
        if (separateAxisY < 0 || separateAxisY > separateMaxY) {
            throw new IllegalArgumentException(
                    String.format(
                            "非等轴Y索引不合法：Y=%d, zoom=%d（合法范围0~%d）",
                            separateAxisY, zoom, separateMaxY));
        }
        if (roundingType == null) {
            roundingType = RoundingType.FLOOR; // 默认向下取整
        }

        // 2. 步骤1：非等轴Y索引 → 对应的纬度坐标（顶部纬度）
        double separateLatSpan = 180.0 / (1 << zoom); // 非等轴纬度跨度：180/2^z
        double lat = MAX_LAT - separateAxisY * separateLatSpan; // 非等轴Y索引对应的顶部纬度

        // 3. 步骤2：纬度坐标 → 等轴Y索引（浮点数）
        double equalLatSpan = 360.0 / (1 << zoom); // 等轴纬度跨度：360/2^z
        double equalAxisY = (MAX_LAT - lat) / equalLatSpan; // 反向计算等轴Y索引

        // 4. 根据业务需求取整（ 不同取整方式适配不同场景）
        int finalEqualY;
        switch (roundingType) {
            case FLOOR:
                finalEqualY = (int) Math.floor(equalAxisY);
                break;
            case CEIL:
                finalEqualY = (int) Math.ceil(equalAxisY);
                break;
            case ROUND:
                finalEqualY = (int) Math.round(equalAxisY);
                break;
            default:
                finalEqualY = (int) Math.floor(equalAxisY);
        }

        // 5. 修正等轴Y索引的合法范围（0 ~ 2^z -1）
        int equalMaxY = (1 << zoom) - 1;
        finalEqualY = Math.max(0, Math.min(finalEqualY, equalMaxY));

        return finalEqualY;
    }

    /** 取整方式枚举（便于明确业务规则） */
    public enum RoundingType {
        FLOOR, // 向下取整
        CEIL, // 向上取整
        ROUND // 四舍五入
    }

    /**
     * 反向转换：等轴Y索引 → 非等轴Y索引
     *
     * <p>与convertSeparateAxisYToEqualAxisY互为逆运算
     *
     * @param equalAxisY 等轴Y索引（XYZ规范）
     * @param zoom 缩放级别（0-30）
     * @param roundingType 取整方式
     * @return 非等轴Y索引（XYZ规范）
     */
    public int convertEqualAxisYToSeparateAxisY(
            int equalAxisY, int zoom, RoundingType roundingType) {
        // 1. 参数校验
        if (zoom < 0 || zoom > 30) {
            throw new IllegalArgumentException("缩放级别不合法：zoom=" + zoom + "（合法范围0-30）");
        }
        int equalMaxY = (1 << zoom) - 1;
        if (equalAxisY < 0 || equalAxisY > equalMaxY) {
            throw new IllegalArgumentException(
                    String.format("等轴Y索引不合法：Y=%d, zoom=%d（合法范围0~%d）", equalAxisY, zoom, equalMaxY));
        }
        if (roundingType == null) {
            roundingType = RoundingType.FLOOR;
        }

        // 2. 等轴Y索引 → 纬度坐标
        double equalLatSpan = 360.0 / (1 << zoom);
        double lat = MAX_LAT - equalAxisY * equalLatSpan;

        // 3. 纬度坐标 → 非等轴Y索引
        double separateLatSpan = 180.0 / (1 << zoom);
        double separateAxisY = (MAX_LAT - lat) / separateLatSpan;

        // 4. 取整并修正范围
        int finalSeparateY;
        switch (roundingType) {
            case FLOOR:
                finalSeparateY = (int) Math.floor(separateAxisY);
                break;
            case CEIL:
                finalSeparateY = (int) Math.ceil(separateAxisY);
                break;
            case ROUND:
                finalSeparateY = (int) Math.round(separateAxisY);
                break;
            default:
                finalSeparateY = (int) Math.floor(separateAxisY);
        }

        int separateMaxY = (1 << zoom) - 1;
        finalSeparateY = Math.max(0, Math.min(finalSeparateY, separateMaxY));

        return finalSeparateY;
    }

    // ========== 子类需实现的差异化核心方法 ==========

    /** 计算经度瓦片跨度（子类实现：等轴返回360/2^z，非等轴返回360/2^z） */
    protected abstract double calculateTileLonSpan(int z);

    /** 计算纬度瓦片跨度（子类实现：等轴返回360/2^z，非等轴返回180/2^z） */
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
        Double tileHeight = tileWidth / 2;
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
