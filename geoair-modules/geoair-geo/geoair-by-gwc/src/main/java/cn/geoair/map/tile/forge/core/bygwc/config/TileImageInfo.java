
package cn.geoair.map.tile.forge.core.bygwc.config;

import lombok.Setter;

/**
 * 表示ArcGIS瓦片缓存配置文件中的{@code TileImageInfo}元素。
 *
 * <p>XML表示形式:
 *
 * <pre>
 * <code>
 *   &lt;TileImageInfo xsi:type='typens:TileImageInfo'&gt;
 *     &lt;CacheTileFormat&gt;JPEG&lt;/CacheTileFormat&gt;
 *     &lt;CompressionQuality&gt;80&lt;/CompressionQuality&gt;
 *     &lt;Antialiasing&gt;true&lt;/Antialiasing&gt;
 *   &lt;/TileImageInfo&gt;
 * </code>
 * </pre>
 *
 * @author Gabriel Roldan
 */
@Setter
public class TileImageInfo   implements   java.io.Serializable{

    /**
     * 缓存瓦片格式
     */
    private String cacheTileFormat;

    /**
     * 压缩质量
     */
    private float compressionQuality;

    /**
     * 是否启用抗锯齿
     */
    private boolean antialiasing;

    /**
     * 波段数量
     */
    private int BandCount;

    /**
     * LERC误差值
     */
    private float LERCError;

    /**
     * 获取缓存瓦片格式，可选值包括{@code PNG8, PNG24, PNG32, JPEG, Mixed}
     *
     * <p>{@code Mixed}表示主要使用JPEG格式，但在缓存边界处使用32位格式
     *
     * @return 缓存瓦片格式
     */
    public String getCacheTileFormat() {
        return cacheTileFormat;
    }

    /**
     * 获取压缩质量
     *
     * @return 压缩质量值
     */
    public float getCompressionQuality() {
        return compressionQuality;
    }

    /**
     * 判断是否启用抗锯齿
     *
     * @return 如果启用抗锯齿则返回true，否则返回false
     */
    public boolean isAntialiasing() {
        return antialiasing;
    }

    /**
     * 获取波段数量
     *
     * @return 波段数量
     */
    public int getBandCount() {
        return BandCount;
    }

    /**
     * 获取LERC误差值
     *
     * @return LERC误差值
     */
    public float getLERCError() {
        return LERCError;
    }



}
