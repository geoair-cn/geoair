
package cn.geoair.map.tile.forge.core.bygwc.compact;


import java.io.File;
import java.nio.ByteBuffer;

/**
 * ArcGIS 10.0 - 10.2版本的紧凑型缓存实现
 *
 * <p>紧凑型缓存由包索引文件(*.bundlx)和包文件(*.bundle)组成，其中包含实际的图像数据。
 * 每个.bundlx文件包含16字节的头部和16字节的尾部。在头部和尾部之间是一个128x128矩阵（16384个瓦片），
 * 每个矩阵元素为5字节的偏移量。每个偏移量指向相应.bundle文件中的一个4字节字段，该字段包含瓦片图像数据的大小。
 * 实际图像数据从offset+4位置开始。如果大小为零，则表示没有可用的图像数据，该索引条目未被使用。
 * 如果地图缓存超过128行或128列，则会被分割成多个.bundlx和.bundle文件。
 *
 * @author Bjoern Saxe
 */
public class ArcGISCompactCacheV1 extends ArcGISCompactCache {
    /**
     * 紧凑型缓存头部长度
     */
    private static final int COMPACT_CACHE_HEADER_LENGTH = 16;

    /**
     * 索引缓存，用于缓存已读取的bundlx文件索引信息
     */
    private BundlxCache indexCache;

    /**
     * 构造一个新的ArcGIS 10.0-10.2紧凑型缓存对象
     *
     * @param pathToCacheRoot 紧凑型缓存目录路径（通常是".../_alllayers/"）。路径必须包含缩放级别目录（命名为"Lxx"）
     */
    public ArcGISCompactCacheV1(String pathToCacheRoot) {
        // 确保路径以文件分隔符结尾
        if (pathToCacheRoot.endsWith("" + File.separatorChar))
            this.pathToCacheRoot = pathToCacheRoot;
        else this.pathToCacheRoot = pathToCacheRoot + File.separatorChar;

        // 初始化索引缓存，缓存容量为10000
        indexCache = new BundlxCache(10000);
    }

    /**
     * 获取指定缩放级别、行号和列号的瓦片资源
     *
     * @param zoom 缩放级别
     * @param row 行号
     * @param col 列号
     * @return BundleFileResource对象，包含瓦片文件路径、偏移量和大小信息；如果未找到则返回null
     */
    @Override
    public BundleFileResource getBundleFileResource(int zoom, int row, int col) {
        // 参数有效性检查
        if (zoom < 0 || col < 0 || row < 0) return null;

        // 创建缓存键值
        BundlxCache.CacheKey key = new BundlxCache.CacheKey(zoom, row, col);
        BundlxCache.CacheEntry entry = null;

        BundleFileResource res = null;

        // 首先尝试从缓存中获取
        if ((entry = indexCache.get(key)) != null) {
            // 如果缓存项存在且大小大于0，则创建资源对象
            if (entry.size > 0)
                res = new BundleFileResource(entry.pathToBundleFile, entry.offset, entry.size);
        } else {
            // 缓存中不存在，需要从文件系统中读取

            // 构建基础路径
            String basePath = buildBundleFilePath(zoom, row, col);
            String pathToBundlxFile = basePath + BUNDLX_EXT;
            String pathToBundleFile = basePath + BUNDLE_EXT;

            // 检查必要的文件是否存在
            if (!(new File(pathToBundleFile)).exists() || !(new File(pathToBundlxFile)).exists())
                return null;

            // 读取瓦片在bundle文件中的起始偏移量
            long tileOffset = readTileStartOffset(pathToBundlxFile, row, col);
            // 读取瓦片数据大小
            int tileSize = readTileSize(pathToBundleFile, tileOffset);

            // 实际数据从偏移量+4字节位置开始（前4字节存储的是大小信息）
            tileOffset += 4;

            // 如果瓦片大小大于0，创建资源对象
            if (tileSize > 0) res = new BundleFileResource(pathToBundleFile, tileOffset, tileSize);

            // 将读取的信息存入缓存
            entry = new BundlxCache.CacheEntry(pathToBundleFile, tileOffset, tileSize);

            indexCache.put(key, entry);
        }

        return res;
    }

    /**
     * 从bundlx文件中读取指定行列瓦片的起始偏移量
     *
     * @param bundlxFile bundlx文件路径
     * @param row 行号
     * @param col 列号
     * @return 瓦片数据在bundle文件中的起始偏移量
     */
    private long readTileStartOffset(String bundlxFile, int row, int col) {
        // 计算在128x128矩阵中的索引位置
        int index = BUNDLX_MAXIDX * (col % BUNDLX_MAXIDX) + (row % BUNDLX_MAXIDX);

        // 从文件中读取5字节的偏移量数据（小端序）
        ByteBuffer idxBytes =
                readFromLittleEndianFile(bundlxFile, (index * 5) + COMPACT_CACHE_HEADER_LENGTH, 5);

        // 返回偏移量值
        return idxBytes.getLong();
    }

    /**
     * 从bundle文件中读取指定偏移量位置的瓦片大小
     *
     * @param bundlxFile bundle文件路径
     * @param offset 偏移量位置
     * @return 瓦片数据大小
     */
    private int readTileSize(String bundlxFile, long offset) {
        // 从文件中读取4字节的大小数据（小端序）
        ByteBuffer tileSize = readFromLittleEndianFile(bundlxFile, offset, 4);

        // 返回大小值
        return tileSize.getInt();
    }
}
