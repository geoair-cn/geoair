
package cn.geoair.map.tile.forge.core.bygwc.config;

import lombok.Setter;

/**
 * 表示ArcGIS瓦片缓存配置文件。
 *
 * <p>XML结构:
 *
 * <pre>
 * <code>
 * &lt;CacheInfo xsi:type='typens:CacheInfo' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xs='http://www.w3.org/2001/XMLSchema'
 *   xmlns:typens='http://www.esri.com/schemas/ArcGIS/10.0'&gt;
 *   &lt;TileCacheInfo xsi:type='typens:TileCacheInfo'&gt;
 *     &lt;SpatialReference xsi:type='typens:ProjectedCoordinateSystem'&gt;
 *     ....
 *     &lt;/SpatialReference&gt;
 *     &lt;TileOrigin xsi:type='typens:PointN'&gt;
 *       &lt;X&gt;-4020900&lt;/X&gt;
 *       &lt;Y&gt;19998100&lt;/Y&gt;
 *     &lt;/TileOrigin&gt;
 *     &lt;TileCols&gt;512&lt;/TileCols&gt;
 *     &lt;TileRows&gt;512&lt;/TileRows&gt;
 *     &lt;DPI&gt;96&lt;/DPI&gt;
 *     &lt;LODInfos xsi:type='typens:ArrayOfLODInfo'&gt;
 *       &lt;LODInfo xsi:type='typens:LODInfo'&gt;
 *         &lt;LevelID&gt;0&lt;/LevelID&gt;
 *         &lt;Scale&gt;8000000&lt;/Scale&gt;
 *         &lt;Resolution&gt;2116.670900008467&lt;/Resolution&gt;
 *       &lt;/LODInfo&gt;
 *
 *       ....
 *
 *     &lt;/LODInfos&gt;
 *   &lt;/TileCacheInfo&gt;
 *   &lt;TileImageInfo xsi:type='typens:TileImageInfo'&gt;
 *     &lt;CacheTileFormat&gt;JPEG&lt;/CacheTileFormat&gt;
 *     &lt;CompressionQuality&gt;80&lt;/CompressionQuality&gt;
 *     &lt;Antialiasing&gt;true&lt;/Antialiasing&gt;
 *   &lt;/TileImageInfo&gt;
 *   &lt;!-- this element is new in 10.0 --&gt;
 *   &lt;CacheStorageInfo xsi:type='typens:CacheStorageInfo'&gt;
 *     &lt;StorageFormat&gt;esriMapCacheStorageModeExploded&lt;/StorageFormat&gt;
 *     &lt;PacketSize&gt;0&lt;/PacketSize&gt;
 *   &lt;/CacheStorageInfo&gt;
 * &lt;/CacheInfo&gt;
 * </code>
 * </pre>
 *
 * @author Gabriel Roldan
 * @see TileCacheInfo 瓦片缓存信息
 * @see SpatialReference 空间参考
 * @see LODInfo 层级细节信息
 * @see TileImageInfo 瓦片图像信息
 * @see CacheStorageInfo 缓存存储信息
 */
@Setter
public class CacheInfo   implements   java.io.Serializable{

    /**
     * 瓦片缓存信息
     */
    private TileCacheInfo tileCacheInfo;

    /**
     * 瓦片图像信息
     */
    private TileImageInfo tileImageInfo;

    /**
     * 缓存存储信息
     */
    private CacheStorageInfo cacheStorageInfo;

    /**
     * 对象序列化读取时的回调方法，用于初始化默认值
     *
     * @return 返回当前对象实例
     */
    private Object readResolve() {
        if (cacheStorageInfo == null) {
            cacheStorageInfo = new CacheStorageInfo();
        }
        return this;
    }

    /**
     * 获取瓦片缓存信息
     *
     * @return 瓦片缓存信息
     */
    public TileCacheInfo getTileCacheInfo() {
        return tileCacheInfo;
    }

    /**
     * 获取瓦片图像信息
     *
     * @return 瓦片图像信息
     */
    public TileImageInfo getTileImageInfo() {
        return tileImageInfo;
    }

    /**
     * 获取缓存存储信息
     *
     * @return 缓存存储信息
     */
    public CacheStorageInfo getCacheStorageInfo() {
        return cacheStorageInfo;
    }
}
