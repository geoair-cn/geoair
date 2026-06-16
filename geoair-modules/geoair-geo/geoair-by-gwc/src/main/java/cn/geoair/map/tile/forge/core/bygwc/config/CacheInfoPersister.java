
package cn.geoair.map.tile.forge.core.bygwc.config;


import cn.geoair.map.tile.forge.core.bygwc.core.GeoWebCacheXStream;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import com.thoughtworks.xstream.XStream;

import java.util.ArrayList;

/**
 * 从ArcGIS Server瓦片缓存的{@code conf.xml}文件中加载{@link CacheInfo}对象
 *
 * @author Gabriel Roldan
 */
public class CacheInfoPersister {

    /**
     * 获取CacheInfoPersister实例
     * @return CacheInfoPersister实例
     */
    public static CacheInfoPersister getInstance() {
        return new CacheInfoPersister();
    }

    /**
     * 从XML字符串加载CacheInfo对象
     * @param reader 包含CacheInfo XML数据的字符串
     * @return 解析后的CacheInfo对象
     */
    public CacheInfo load(final String reader) {
        // 获取配置好的XStream实例
        XStream xs = getConfiguredXStream();
        // 从XML字符串中解析CacheInfo对象
        CacheInfo ci = (CacheInfo) xs.fromXML(reader);
        return ci;
    }

    /**
     * 获取并配置XStream实例，用于XML序列化和反序列化
     * @return 配置好的XStream实例
     */
    XStream getConfiguredXStream() {
        // 创建GeoWebCacheXStream实例
        XStream xs = new GeoWebCacheXStream();

        // 允许所有属于GWC包下的类进行反序列化
        xs.allowTypesByWildcard(new String[]{"cn.geoair.map.tile.forge.bygwc.**"});

        // 设置XStream模式为不使用引用
        xs.setMode(XStream.NO_REFERENCES);

        // 为各种类设置别名，简化XML表示
        xs.alias("SpatialReference", SpatialReference.class);
        xs.alias("TileOrigin", TileOrigin.class);

        xs.alias("TileCacheInfo", TileCacheInfo.class);
        xs.aliasField("SpatialReference", TileCacheInfo.class, "spatialReference");
        xs.aliasField("TileOrigin", TileCacheInfo.class, "tileOrigin");
        xs.aliasField("TileCols", TileCacheInfo.class, "tileCols");
        xs.aliasField("TileRows", TileCacheInfo.class, "tileRows");
        xs.aliasField("LODInfos", TileCacheInfo.class, "lodInfos");
        xs.alias("LODInfos", new ArrayList<LODInfo>().getClass());

        xs.alias("LODInfo", LODInfo.class);
        xs.aliasField("LevelID", LODInfo.class, "levelID");
        xs.aliasField("Scale", LODInfo.class, "scale");
        xs.aliasField("Resolution", LODInfo.class, "resolution");

        xs.alias("TileImageInfo", TileImageInfo.class);
        xs.aliasField("CacheTileFormat", TileImageInfo.class, "cacheTileFormat");
        xs.aliasField("CompressionQuality", TileImageInfo.class, "compressionQuality");
        xs.aliasField("Antialiasing", TileImageInfo.class, "antialiasing");

        xs.alias("CacheStorageInfo", CacheStorageInfo.class);
        xs.aliasField("StorageFormat", CacheStorageInfo.class, "storageFormat");
        xs.aliasField("PacketSize", CacheStorageInfo.class, "packetSize");

        xs.alias("CacheInfo", CacheInfo.class);
        xs.aliasField("TileCacheInfo", CacheInfo.class, "tileCacheInfo");
        xs.aliasField("TileImageInfo", CacheInfo.class, "tileImageInfo");
        xs.aliasField("CacheStorageInfo", CacheInfo.class, "cacheStorageInfo");

        xs.alias("EnvelopeN", EnvelopeN.class);
        xs.aliasField("XMin", EnvelopeN.class, "xmin");
        xs.aliasField("YMin", EnvelopeN.class, "ymin");
        xs.aliasField("XMax", EnvelopeN.class, "xmax");
        xs.aliasField("YMax", EnvelopeN.class, "ymax");
        xs.aliasField("SpatialReference", EnvelopeN.class, "spatialReference");

        return xs;
    }

    /**
     * 解析图层边界XML文件
     * @param layerBoundsFile 包含边界信息的XML字符串
     * @return 解析后的BoundingBox对象
     */
    public BoundingBox parseLayerBounds(final String layerBoundsFile) {
        // 使用配置好的XStream从XML字符串中解析EnvelopeN对象
        EnvelopeN envN = (EnvelopeN) getConfiguredXStream().fromXML(layerBoundsFile);

        // 根据解析的EnvelopeN对象创建BoundingBox对象
        BoundingBox bbox =
                new BoundingBox(envN.getXmin(), envN.getYmin(), envN.getXmax(), envN.getYmax());

        return bbox;
    }
}
