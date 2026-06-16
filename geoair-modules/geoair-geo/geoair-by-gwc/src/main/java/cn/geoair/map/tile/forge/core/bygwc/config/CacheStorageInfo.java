
package cn.geoair.map.tile.forge.core.bygwc.config;

import lombok.Setter;

/**
 * 表示ArcGIS瓦片缓存配置文件中的{@code CacheStorageInfo}元素。
 *
 * <p>此元素从ArcGIS 10.0开始存在，用于定义缓存是"exploded"（展开）格式还是"compact"（紧凑）格式。
 * 由于ESRI未公开"compact"格式的文档，我们仅支持"exploded"格式。
 *
 * <p>XML表示形式:
 *
 * <pre>
 * <code>
 *   &lt;CacheStorageInfo xsi:type='typens:CacheStorageInfo'&gt;
 *     &lt;StorageFormat&gt;esriMapCacheStorageModeExploded&lt;/StorageFormat&gt;
 *     &lt;PacketSize&gt;0&lt;/PacketSize&gt;
 *   &lt;/CacheStorageInfo&gt;
 * </code>
 * </pre>
 *
 * @author Gabriel Roldan
 */
@Setter
public class CacheStorageInfo  implements   java.io.Serializable {

    /** 展开格式代码 */
    public static final String EXPLODED_FORMAT_CODE = "esriMapCacheStorageModeExploded";

    /** 紧凑格式代码 */
    public static final String COMPACT_FORMAT_CODE = "esriMapCacheStorageModeCompact";

    /** 紧凑格式代码V2版本 */
    public static final String COMPACT_FORMAT_CODE_V2 = "esriMapCacheStorageModeCompactV2";

    /** 存储格式 */
    private String storageFormat;

    /** 数据包大小 */
    private int packetSize;

    /**
     * 对象反序列化后调用的方法，用于初始化默认值
     *
     * @return 初始化后的对象实例
     */
    private Object readResolve() {
        if (storageFormat == null) {
            storageFormat = EXPLODED_FORMAT_CODE;
        }
        return this;
    }

    /**
     * 获取配置文件中定义的存储格式，默认为 {@link #EXPLODED_FORMAT_CODE 展开格式}
     *
     * @return 存储格式字符串
     */
    public String getStorageFormat() {
        return storageFormat;
    }

    /**
     * 获取数据包大小
     *
     * @return 数据包大小值
     */
    public int getPacketSize() {
        return packetSize;
    }
}
