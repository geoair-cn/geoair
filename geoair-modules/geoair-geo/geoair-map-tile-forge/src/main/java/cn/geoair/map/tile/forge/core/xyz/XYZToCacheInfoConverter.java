package cn.geoair.map.tile.forge.core.xyz;

import cn.geoair.map.tile.forge.core.bygwc.config.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 从XYZ图层参数生成ArcGIS CacheInfo对象的工具类
 */
public class XYZToCacheInfoConverter {

    static XYZToCacheInfoConverter instance;

    public static XYZToCacheInfoConverter getInstance() {
        return instance == null ? instance = new XYZToCacheInfoConverter() : instance;
    }


    // Web Mercator（EPSG:3857）的基本参数
    private static final double WMTS_ORIGIN_X = -20037508.342789244;
    private static final double WMTS_ORIGIN_Y = 20037508.342789244;
    private static final double WMTS_MAX_EXTENT = 20037508.342789244;
    private static final double WMTS_FULL_EXTENT = 2 * WMTS_MAX_EXTENT;
    private static final int DEFAULT_TILE_SIZE = 256; // XYZ瓦片默认尺寸
    private static final int DPI = 96; // 标准DPI
    private static final double METERS_PER_INCH = 0.0254; // 米/英寸

    /**
     * 生成Web Mercator投影的XYZ图层对应的CacheInfo对象
     *
     * @param minZoom  最小缩放级别（如0）
     * @param maxZoom  最大缩放级别（如18）
     * @param tileSize 瓦片像素尺寸（默认256）
     * @return 完整的CacheInfo对象
     */
    public CacheInfo createCacheInfo(int minZoom, int maxZoom, int tileSize) {
        if (tileSize <= 0) {
            tileSize = DEFAULT_TILE_SIZE;
        }

        // 1. 构建TileCacheInfo（核心缓存配置）
        TileCacheInfo tileCacheInfo = new TileCacheInfo();

        // 设置空间参考（EPSG:3857）
        SpatialReference spatialRef = createWebMercatorSpatialReference();
        tileCacheInfo.setSpatialReference(spatialRef);

        // 设置瓦片原点（Web Mercator左上角）
        TileOrigin tileOrigin = new TileOrigin();
        tileOrigin.setX(WMTS_ORIGIN_X);
        tileOrigin.setY(WMTS_ORIGIN_Y);
        tileCacheInfo.setTileOrigin(tileOrigin);

        // 设置瓦片尺寸
        tileCacheInfo.setTileCols(tileSize);
        tileCacheInfo.setTileRows(tileSize);

        // 设置DPI
        tileCacheInfo.setDPI(DPI);
        tileCacheInfo.setPreciseDPI(DPI);

        // 构建LODInfos（层级信息）
        List<LODInfo> lodInfos = createLODInfos(minZoom, maxZoom, tileSize);
        tileCacheInfo.setLodInfos(lodInfos);

        // 2. 构建TileImageInfo（瓦片图像配置）
        TileImageInfo tileImageInfo = new TileImageInfo();
        tileImageInfo.setCacheTileFormat("PNG");
        tileImageInfo.setCompressionQuality(80); // 默认压缩质量
        tileImageInfo.setAntialiasing(true);

        // 3. 构建CacheStorageInfo（存储配置）
        CacheStorageInfo storageInfo = new CacheStorageInfo();
        storageInfo.setStorageFormat("esriMapCacheStorageModeExploded"); // 松散格式（对应XYZ目录结构）
        storageInfo.setPacketSize(0);

        // 4. 组装CacheInfo
        CacheInfo cacheInfo = new CacheInfo();
        cacheInfo.setTileCacheInfo(tileCacheInfo);
        cacheInfo.setTileImageInfo(tileImageInfo);
        cacheInfo.setCacheStorageInfo(storageInfo);

        return cacheInfo;
    }

    /**
     * 创建Web Mercator（EPSG:3857）的空间参考对象
     */
    private static SpatialReference createWebMercatorSpatialReference() {
        SpatialReference spatialRef = new SpatialReference();
        spatialRef.setWKID(3857); // EPSG:3857的Wkid
        spatialRef.setLatestWKID(3857);
        spatialRef.setWKT(
                "PROJCS[\"WGS_1984_Web_Mercator_Auxiliary_Sphere\"," +
                        "GEOGCS[\"GCS_WGS_1984\"," +
                        "DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]]," +
                        "PRIMEM[\"Greenwich\",0.0]," +
                        "UNIT[\"Degree\",0.0174532925199433]]," +
                        "PROJECTION[\"Mercator_Auxiliary_Sphere\"]," +
                        "PARAMETER[\"False_Easting\",0.0]," +
                        "PARAMETER[\"False_Northing\",0.0]," +
                        "PARAMETER[\"Central_Meridian\",0.0]," +
                        "PARAMETER[\"Standard_Parallel_1\",0.0]," +
                        "PARAMETER[\"Auxiliary_Sphere_Type\",0.0]," +
                        "UNIT[\"Meter\",1.0]]"
        );
        return spatialRef;
    }

    /**
     * 生成各缩放级别的LODInfo列表
     *
     * @param minZoom  最小级别
     * @param maxZoom  最大级别
     * @param tileSize 瓦片尺寸
     * @return LODInfo列表
     */
    private static List<LODInfo> createLODInfos(int minZoom, int maxZoom, int tileSize) {
        List<LODInfo> lodInfos = new ArrayList<>();
//        for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
        for (int zoom = 0; zoom <= 19; zoom++) {
            LODInfo lod = new LODInfo();
            lod.setLevelID(zoom);

            // 标准分辨率计算：周长 / (瓦片总数 * 瓦片尺寸)
            double tileCount = Math.pow(2, zoom);
            double resolution = WMTS_FULL_EXTENT / (tileCount * tileSize);
            lod.setResolution(resolution);

            // 标准比例尺计算：分辨率 * DPI / 米/英寸
            double scale = resolution * DPI / METERS_PER_INCH;
            lod.setScale(scale);

            lodInfos.add(lod);
        }

        return lodInfos;
    }

    /**
     * 重载方法：使用默认瓦片尺寸（256）
     */
    public CacheInfo createCacheInfo(int minZoom, int maxZoom) {
        return createCacheInfo(minZoom, maxZoom, DEFAULT_TILE_SIZE);
    }

    // 测试示例
    public static void main(String[] args) {
        // 生成0-18级的XYZ图层CacheInfo
        CacheInfo cacheInfo = XYZToCacheInfoConverter.getInstance().createCacheInfo(0, 18);

        // 验证结果
        System.out.println("空间参考Wkid: " + cacheInfo.getTileCacheInfo().getSpatialReference().getWKID());
        System.out.println("瓦片原点: " + cacheInfo.getTileCacheInfo().getTileOrigin().getX() + ", " +
                cacheInfo.getTileCacheInfo().getTileOrigin().getY());
        System.out.println("瓦片尺寸: " + cacheInfo.getTileCacheInfo().getTileCols() + "x" +
                cacheInfo.getTileCacheInfo().getTileRows());
        System.out.println("层级数量: " + cacheInfo.getTileCacheInfo().getLodInfos().size());
        System.out.println("第0级分辨率: " + cacheInfo.getTileCacheInfo().getLodInfos().get(0).getResolution());
    }
}
