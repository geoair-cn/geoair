package cn.geoair.map.dynamic.tools.grid.converter;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.geoair.map.dynamic.tools.grid.dto.TileLevelMetadata;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Web墨卡托（3857）坐标系瓦片转换实现类
 */
public class TileConverter3857Utils extends TileConverterCommon {
    GiLogger log = GirLoggerFactory.getLogger();

    // 墨卡托投影常量（地球半径）
    private static final double EARTH_RADIUS = 6378137.0;

    private static final double MAX_MERCATOR = EARTH_RADIUS * Math.PI;

    // 单例实例（volatile保证可见性，防止指令重排）
    private static volatile TileConverter3857Utils INSTANCE;

    public TileConverter3857Utils(ToolsConfig advToolsConfig) {
        super(advToolsConfig);
    }

    /**
     * 获取单例实例（双重校验锁）
     *
     * @return 单例对象
     */
    @Deprecated
    public static TileConverter3857Utils getInstance() {

        if (INSTANCE == null) {
            synchronized (TileConverter3857Utils.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TileConverter3857Utils(new ToolsConfig());
                }
            }
        }
        return INSTANCE;
    }

    @Deprecated
    public static TileConverter3857Utils getInstance(ToolsConfig advToolsConfig) {
        return new TileConverter3857Utils(advToolsConfig);
    }

    public BoxReferencedEnvelope xyzToTileBox(int z, int x, int y, int targetSrid) {
        validateXyz(z, x, y);
        // 直接计算3857坐标的瓦片范围
        double minX = tileXToCoordinateX(x, z);
        double maxX = tileXToCoordinateX(x + 1, z);
        double minY = tileYToCoordinateY(y + 1, z); // 注意y轴反转：瓦片Y越大，3857Y越小
        double maxY = tileYToCoordinateY(y, z);

        Envelope envelope = new Envelope(minX, maxX, minY, maxY);
        Envelope convert = sridConvertOpt.convert(envelope, 3857, targetSrid);
        return new BoxReferencedEnvelope(convert, targetSrid);
    }

    @Override
    public RangeApo tileRangeByBox(int z, Envelope tileBox) {
        if (Objects.isNull(tileBox)) {
            throw new IllegalArgumentException("地理范围Envelope不能为空");
        }
        validateXyz(z, 0, 0);

        // 点、线等退化范围：加极小偏移，保证几何能落进某一个瓦片
        Envelope envelope = tileBox;
        if (envelope.isNull()
                || Math.abs(envelope.getMaxX() - envelope.getMinX()) < POINT_OFFSET
                || Math.abs(envelope.getMaxY() - envelope.getMinY()) < POINT_OFFSET) {
            double centerX = (envelope.getMinX() + envelope.getMaxX()) / 2;
            double centerY = (envelope.getMinY() + envelope.getMaxY()) / 2;
            envelope = new Envelope(
                    centerX - POINT_OFFSET,
                    centerX + POINT_OFFSET,
                    centerY - POINT_OFFSET,
                    centerY + POINT_OFFSET);
        }

        // 夹到 Web Mercator 有效范围，避免越界坐标算出非法索引
        double envMinX = clampDouble(envelope.getMinX(), -MAX_MERCATOR, MAX_MERCATOR);
        double envMaxX = clampDouble(envelope.getMaxX(), -MAX_MERCATOR, MAX_MERCATOR);
        double envMinY = clampDouble(envelope.getMinY(), -MAX_MERCATOR, MAX_MERCATOR);
        double envMaxY = clampDouble(envelope.getMaxY(), -MAX_MERCATOR, MAX_MERCATOR);

        // 通过分辨率计算，geowebcache就是这样的计算方式
        TileLevelMetadata tileLevelMetadata = getTileLevelMetadata(z);
        double resolution = tileLevelMetadata.getResolution();
        double width = resolution * 256;
        double height = resolution * 256;
        double[] tileOrigin = {-20037508.3427892, 20037508.3427892};
        long minX = (long) Math.floor((envMinX - tileOrigin[0]) / width);
        long maxX = (long) Math.ceil((envMaxX - tileOrigin[0]) / width);
        long minY = (long) Math.floor((tileOrigin[1] - envMaxY) / height);
        long maxY = (long) Math.ceil((tileOrigin[1] - envMinY) / height);

        // 边界修正：ceil 可能得到 2^z，必须夹回 [0, 2^z - 1]，
        // 否则下游 xyzToQuadKey 会抛「X/Y坐标超出范围」。
        long maxTileIndex = (1L << z) - 1;
        minX = clampLong(minX, 0, maxTileIndex);
        maxX = clampLong(maxX, 0, maxTileIndex);
        minY = clampLong(minY, 0, maxTileIndex);
        maxY = clampLong(maxY, 0, maxTileIndex);

        return new RangeApo((int) minX, (int) maxX, (int) minY, (int) maxY, z);
    }

    /** 把数值限制在 [min, max] 区间内 */
    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    /** 把瓦片索引限制在 [min, max] 区间内 */
    private static long clampLong(long value, long min, long max) {
        return Math.max(min, Math.min(value, max));
    }

    @Override
    public RangeApo tileRangeByBox(int z, Envelope tileBox, int srcSrid) {
        Envelope convert = sridConvertOpt.convert(tileBox, srcSrid, 3857);
        return tileRangeByBox(z, convert);
    }

    @Override
    public RangeApo tileRangeByGeom(int z, Geometry geometry, int srcSrid) {
        Geometry transform = transform(geometry, srcSrid);
        return tileRangeByGeom(z, transform);
    }

    /**
     * 瓦片X索引转3857坐标系X坐标（米）
     *
     * @param x 瓦片X索引
     * @param z 缩放级别
     * @return 3857 X坐标（米）
     */
    public double tileXToCoordinateX(int x, int z) {
        validateXyz(z, x, 0);
        double tileCount = Math.pow(2.0, z);
        // 核心公式：3857X = (x / 总瓦片数) * 2*MAX_MERCATOR - MAX_MERCATOR
        return (x / tileCount) * 2 * MAX_MERCATOR - MAX_MERCATOR;
    }

    /**
     * 瓦片Y索引转3857坐标系Y坐标（米）
     *
     * @param y 瓦片Y索引
     * @param z 缩放级别
     * @return 3857 Y坐标（米）
     */
    public double tileYToCoordinateY(int y, int z) {
        validateXyz(z, 0, y);
        double tileCount = Math.pow(2.0, z);
        // 核心公式：3857Y = MAX_MERCATOR - (y / 总瓦片数) * 2*MAX_MERCATOR（Y轴反转）
        return MAX_MERCATOR - (y / tileCount) * 2 * MAX_MERCATOR;
    }

    /**
     * 将几何图形从源坐标系转换为WGS84(4326)坐标系
     *
     * @param geometry 几何图形对象
     * @param srcSrid  源坐标系SRID代码
     * @return 转换后的几何图形对象
     */
    public Geometry transform(Geometry geometry, int srcSrid) {
        return sridConvertOpt.convert(geometry, srcSrid, 3857);
    }

    @Override
    public int convertSeparateAxisYToEqualAxisY(
            int separateAxisY, int zoom, AbstractWgs84TileConverter.RoundingType roundingType) {
        return separateAxisY;
    }

    @Override
    public int convertEqualAxisYToSeparateAxisY(
            int equalAxisY, int zoom, AbstractWgs84TileConverter.RoundingType roundingType) {
        return equalAxisY;
    }

    /**
     * 根据最大分辨率层级获取瓦片元数据（支持自定义瓦片尺寸和DPI）
     *
     * @param thisZoom      最大分辨率层级（最大缩放级别）
     * @param tilePixelSize 瓦片像素尺寸（例如：256、512）
     * @param dpi           屏幕DPI（例如：72、96、300）
     * @return 瓦片层级元数据对象
     */
    public TileLevelMetadata getTileLevelMetadata(int thisZoom, int tilePixelSize, double dpi) {
        validateXyz(thisZoom, 0, 0);

        Double tileWidth = Math.pow(2.0, thisZoom);
        Double tileHeight = Math.pow(2.0, thisZoom);

        double tileSize = 2 * MAX_MERCATOR / tileWidth;  // 每个瓦片的实际地理尺寸（米）


        // 计算该层级下的瓦片总数
        long totalTiles = (long) (tileWidth * tileHeight);

        // 计算该层级的地面分辨率（每像素代表的米数）
        double groundResolution = tileSize / tilePixelSize;

        // 计算该层级的比例尺
        // 公式：Scale = (Pixel Size in Meters) * DPI / 0.0254
        // 其中 1英寸 = 0.0254米
        double scale = groundResolution * dpi / 0.0254;

        // 计算每像素代表的实际长度（毫米）
        double mmPerPixel = groundResolution * 1000;

        return new TileLevelMetadata(
                thisZoom,
                tileWidth.intValue(),
                tileHeight.intValue(),
                tileSize,
                groundResolution,
                groundResolution,
                scale,
                totalTiles,
                tilePixelSize,
                dpi,
                mmPerPixel,
                new Envelope(-MAX_MERCATOR, MAX_MERCATOR, -MAX_MERCATOR, MAX_MERCATOR),
                "EPSG:3857");
    }

    /**
     * 根据最大分辨率层级获取瓦片元数据（使用默认配置）
     *
     * @param thisZoom 最大分辨率层级
     * @return 瓦片层级元数据对象
     */
    public TileLevelMetadata getTileLevelMetadata(int thisZoom) {
        int defaultTileSize = advToolsConfig.getTilePixelSize() > 0
                ? advToolsConfig.getTilePixelSize() : 256;
        int defaultDpi = advToolsConfig.getDpi() > 0
                ? advToolsConfig.getDpi() : 96;
        return getTileLevelMetadata(thisZoom, defaultTileSize, defaultDpi);
    }

    /**
     * 批量获取多个层级的瓦片元数据
     *
     * @param minZoom       最小层级
     * @param maxZoom       最大层级
     * @param tilePixelSize 瓦片像素尺寸
     * @param dpi           屏幕DPI
     * @return 层级元数据列表
     */
    public List<TileLevelMetadata> getTileLevelMetadataList(int minZoom, int maxZoom,
                                                            int tilePixelSize, double dpi) {
        if (minZoom < 0 || maxZoom < minZoom) {
            throw new IllegalArgumentException("层级参数无效: minZoom=" + minZoom + ", maxZoom=" + maxZoom);
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
     * @param tilePixelSize    瓦片像素尺寸
     * @return 最合适的瓦片层级
     */
    public int getZoomByResolution(double targetResolution, int tilePixelSize) {
        if (targetResolution <= 0) {
            throw new IllegalArgumentException("分辨率必须大于0");
        }

        // 计算每个层级的地面分辨率，找到最接近的
        for (int z = 0; z <= 22; z++) {  // 最大支持到22级
            double tileSize = 2 * MAX_MERCATOR / Math.pow(2, z);
            double resolution = tileSize / tilePixelSize;

            if (resolution <= targetResolution) {
                return z;
            }
        }
        return 22;  // 返回最大层级
    }

    /**
     * 根据比例尺反推合适的瓦片层级
     *
     * @param targetScale   目标比例尺（例如：10000 表示 1:10000）
     * @param tilePixelSize 瓦片像素尺寸
     * @param dpi           屏幕DPI
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
    public BoxReferencedEnvelope boundsFromTileRange(long minTileX, long maxTileX, long minTileY, long maxTileY, int zoom, int targetSrid) {
//        log.info("minx:{},maxx:{}", minTileX, maxTileX);
//        log.info("minTileY:{},maxTileY:{}", minTileY, maxTileY);
//        TileLevelMetadata tileLevelMetadata = getTileLevelMetadata(zoom);
//        double width = tileLevelMetadata.getResolution() * 256;
//        double height = tileLevelMetadata.getResolution() * 256;
//        double[] tileOrigin = {-20037508.3427892, 20037508.3427892};
//        double minx = tileOrigin[0] + width * minTileX;
//        double maxx = tileOrigin[0] + width * (maxTileX);
//
//        double miny = height * (tileLevelMetadata.getNumTilesHigh()-minTileY) - tileOrigin[1];
//        double maxy = height * ( tileLevelMetadata.getNumTilesHigh()-maxTileY) - tileOrigin[1];
//        BoxReferencedEnvelope boxReferencedEnvelope = new BoxReferencedEnvelope(sridConvertOpt.convert(new Envelope(minx, maxx, miny, maxy), 3857, targetSrid), targetSrid);
//        System.out.println("===================");
//        System.out.println(boxReferencedEnvelope.getWktString(4326));
//        System.out.println("===================");
//
//        return boxReferencedEnvelope;
        validateXyz(zoom, (int) minTileX, (int) minTileY);


        // 计算四个角的瓦片边界
        // 左下角瓦片
        double minX = tileXToCoordinateX((int) minTileX, zoom);
        double minY = tileYToCoordinateY((int) (maxTileY), zoom);  // 注意Y轴方向

        // 右上角瓦片
        double maxX = tileXToCoordinateX((int) (maxTileX), zoom);
        double maxY = tileYToCoordinateY((int) minTileY, zoom);

        Envelope envelope3857 = new Envelope(minX, maxX, minY, maxY);
        Envelope converted = sridConvertOpt.convert(envelope3857, 3857, targetSrid);
        return new BoxReferencedEnvelope(converted, targetSrid);
    }

    public static void main(String[] args) {
        Test3857();
        System.out.println();

        Test4326();
    }

    public static void Test3857() {
        TileConverter3857Utils converter = new TileConverter3857Utils(ToolsConfig.of());
        TileLevelMetadata metadata1 = converter.getTileLevelMetadata(10);
        System.out.println(metadata1);

        // 示例2：自定义瓦片尺寸和DPI
        TileLevelMetadata metadata2 = converter.getTileLevelMetadata(10, 256, 96);
        System.out.println(metadata2);

        // 示例3：批量获取多个层级的元数据（打印质量）
        List<TileLevelMetadata> metadataList = converter.getTileLevelMetadataList(0, 10, 256, 96);
        for (TileLevelMetadata meta : metadataList) {
            System.out.println(meta);
        }

        // 示例4：根据地面分辨率找层级
        double targetResolution = 0.1;  // 希望每像素0.1米
        int zoom = converter.getZoomByResolution(targetResolution, 256);
        System.out.println("分辨率 " + targetResolution + " m/px 对应的层级: " + zoom);

        // 示例5：根据比例尺找层级
        int zoomByScale = converter.getZoomByScale(10000, 256, 96);  // 1:10000比例尺
        System.out.println("1:10000比例尺对应的层级: " + zoomByScale);
    }

    public static void Test4326() {
        Wgs84SeparateAxisTileUtils converter = new Wgs84SeparateAxisTileUtils(ToolsConfig.of());
        TileLevelMetadata metadata1 = converter.getTileLevelMetadata(10);
        System.out.println(metadata1);

        // 示例2：自定义瓦片尺寸和DPI
        TileLevelMetadata metadata2 = converter.getTileLevelMetadata(10, 256, 96);
        System.out.println(metadata2);

        // 示例3：批量获取多个层级的元数据（打印质量）
        List<TileLevelMetadata> metadataList = converter.getTileLevelMetadataList(0, 10, 256, 96);
        for (TileLevelMetadata meta : metadataList) {
            System.out.println(meta);
        }

//        // 示例4：根据地面分辨率找层级
//        double targetResolution = 0.1;  // 希望每像素0.1米
//        int zoom = converter.getZoomByResolution(targetResolution, 256);
//        System.out.println("分辨率 " + targetResolution + " m/px 对应的层级: " + zoom);
//
//        // 示例5：根据比例尺找层级
//        int zoomByScale = converter.getZoomByScale(10000, 256, 96);  // 1:10000比例尺
//        System.out.println("1:10000比例尺对应的层级: " + zoomByScale);
    }

}
