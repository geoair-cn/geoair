package cn.geoair.map.dynamic.tools.grid.converter;


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

        // 1. 计算当前层级瓦片尺寸（3857平面坐标，单位：米）
        double tileSize = 2 * MAX_MERCATOR / Math.pow(2, z);

        // 2. 直接基于3857坐标计算瓦片索引
        double tileXmin = Math.floor((tileBox.getMinX() + MAX_MERCATOR) / tileSize);
        double tileXmax = Math.ceil((tileBox.getMaxX() + MAX_MERCATOR) / tileSize);
        double tileYmin = Math.floor((MAX_MERCATOR - tileBox.getMaxY()) / tileSize);
        double tileYmax = Math.ceil((MAX_MERCATOR - tileBox.getMinY()) / tileSize);

        // 3. 边界修正（确保瓦片索引在合法范围）
        int maxTileIndex = (1 << z) - 1;
        tileXmin = Math.max(0, Math.min(tileXmin, maxTileIndex));
        tileXmax = Math.max(0, Math.min(tileXmax, maxTileIndex));
        tileYmin = Math.max(0, Math.min(tileYmin, maxTileIndex));
        tileYmax = Math.max(0, Math.min(tileYmax, maxTileIndex));

        // 4. 返回瓦片索引范围
        return new RangeApo(tileXmin, tileXmax, tileYmin, tileYmax, z);
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
     * @param maxZoom       最大分辨率层级（最大缩放级别）
     * @param tilePixelSize 瓦片像素尺寸（例如：256、512）
     * @param dpi           屏幕DPI（例如：72、96、300）
     * @return 瓦片层级元数据对象
     */
    public TileLevelMetadata getTileLevelMetadata(int maxZoom, int tilePixelSize, double dpi) {
        validateXyz(maxZoom, 0, 0);

        double tileCount = Math.pow(2.0, maxZoom);
        double tileSize = 2 * MAX_MERCATOR / tileCount;  // 每个瓦片的实际地理尺寸（米）
        double totalWidth = 2 * MAX_MERCATOR;             // 整个3857平面的总宽度（米）

        // 计算该层级下的瓦片总数
        long totalTiles = (long) (tileCount * tileCount);

        // 计算该层级的地面分辨率（每像素代表的米数）
        double groundResolution = tileSize / tilePixelSize;

        // 计算该层级的比例尺
        // 公式：Scale = (Pixel Size in Meters) * DPI / 0.0254
        // 其中 1英寸 = 0.0254米
        double scale = groundResolution * dpi / 0.0254;

        // 计算每像素代表的实际长度（毫米）
        double mmPerPixel = groundResolution * 1000;

        return new TileLevelMetadata(
                maxZoom,
                tileCount,
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
     * @param maxZoom 最大分辨率层级
     * @return 瓦片层级元数据对象
     */
    public TileLevelMetadata getTileLevelMetadata(int maxZoom) {
        int defaultTileSize = advToolsConfig.getTilePixelSize() > 0
                ? advToolsConfig.getTilePixelSize() : 256;
        int defaultDpi = advToolsConfig.getDpi() > 0
                ? advToolsConfig.getDpi() : 96;
        return getTileLevelMetadata(maxZoom, defaultTileSize, defaultDpi);
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
        validateXyz(zoom, (int) minTileX, (int) minTileY);
        // 计算四个角的瓦片边界
        // 左下角瓦片
        double minX = tileXToCoordinateX((int) minTileX, zoom);
        double minY = tileYToCoordinateY((int) (maxTileY  ), zoom);  // 注意Y轴方向

        // 右上角瓦片
        double maxX = tileXToCoordinateX((int) (maxTileX  ), zoom);
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
        Wgs84EqualAxisTileUtils converter = new Wgs84EqualAxisTileUtils(ToolsConfig.of());
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
