/**
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * <p>You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 * <p>Copyright 2019
 */
package cn.geoair.map.tile.forge.core.bygwc.compact;





import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * ArcGIS 10.3 紧凑型缓存的实现
 *
 * <p>紧凑型缓存由 bundle 文件(*.bundle)组成，这些文件包含索引和实际的图像数据。每个.bundle文件以64字节的头部开始。
 * 头部之后是一个128x128矩阵(16384个瓦片)的8字节字。每个字的前5个字节是指向同一.bundle文件内瓦片图像数据的偏移量。
 * 接下来的3个字节是图像数据的大小。图像数据的大小在偏移量减4的位置以4字节字重复存储。未使用的索引条目使用
 * 04|00|00|00|00|00|00|00。如果大小为零，则表示没有可用的图像数据，索引条目为空。如果地图缓存超过128行或列，
 * 则会被分割成多个.bundle文件。
 *
 * @author Bjoern Saxe
 */
public class ArcGISCompactCacheV2 extends ArcGISCompactCache {
    private static final int COMPACT_CACHE_HEADER_LENGTH = 64;

    private BundlxCache indexCache;

    /**
     * Constructs new ArcGIS 10.3 compact cache.
     *
     * @param pathToCacheRoot Path to compact cache directory (usually ".../_alllayers/"). Path must
     *     contain directories for zoom levels (named "Lxx").
     */
    public ArcGISCompactCacheV2(String pathToCacheRoot) {
        if (pathToCacheRoot.endsWith("" + File.separatorChar))
            this.pathToCacheRoot = pathToCacheRoot;
        else this.pathToCacheRoot = pathToCacheRoot + File.separatorChar;

        indexCache = new BundlxCache(10000);
    }

    @Override
    public BundleFileResource getBundleFileResource(int zoom, int row, int col) {
        if (zoom < 0 || col < 0 || row < 0) return null;

        BundlxCache.CacheKey key = new BundlxCache.CacheKey(zoom, row, col);
        BundlxCache.CacheEntry entry = null;

        BundleFileResource res = null;

        if ((entry = indexCache.get(key)) != null) {
            if (entry.size > 0)
                res = new BundleFileResource(entry.pathToBundleFile, entry.offset, entry.size);
        } else {

            String basePath = buildBundleFilePath(zoom, row, col);
            String pathToBundleFile = basePath + BUNDLE_EXT;

            if (!(new File(pathToBundleFile)).exists()) return null;

            entry = createCacheEntry(pathToBundleFile, row, col);

            if (entry.size > 0)
                res = new BundleFileResource(pathToBundleFile, entry.offset, entry.size);

            indexCache.put(key, entry);
        }

        return res;
    }

    private BundlxCache.CacheEntry createCacheEntry(String bundleFile, int row, int col) {
        // col and row are inverted for 10.3 caches
        int index = BUNDLX_MAXIDX * (row % BUNDLX_MAXIDX) + (col % BUNDLX_MAXIDX);

        // to save one addtional read, we read all 8 bytes in one read
        ByteBuffer offsetAndSize =
                readFromLittleEndianFile(bundleFile, (index * 8) + COMPACT_CACHE_HEADER_LENGTH, 8);

        byte[] offsetBytes = new byte[8];
        byte[] sizeBytes = new byte[4];

        offsetAndSize.get(offsetBytes, 0, 5);
        offsetAndSize.get(sizeBytes, 0, 3);

        long tileOffset = ByteBuffer.wrap(offsetBytes).order(ByteOrder.LITTLE_ENDIAN).getLong();
        int tileSize = ByteBuffer.wrap(sizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

        return new BundlxCache.CacheEntry(bundleFile, tileOffset, tileSize);
    }
}
