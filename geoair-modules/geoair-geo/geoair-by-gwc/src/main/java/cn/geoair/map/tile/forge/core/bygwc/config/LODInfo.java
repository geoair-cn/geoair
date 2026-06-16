
package cn.geoair.map.tile.forge.core.bygwc.config;

import lombok.Setter;

/**
 * 表示ArcGIS瓦片缓存配置文件中的{@code LODInfo}（细节层次信息）元素。
 *
 * <p>XML表示形式:
 *
 * <pre>
 * <code>
 *       &lt;LODInfo xsi:type='typens:LODInfo'&gt;
 *         &lt;LevelID&gt;1&lt;/LevelID&gt;
 *         &lt;Scale&gt;6000000&lt;/Scale&gt;
 *         &lt;Resolution&gt;1587.5031750063501&lt;/Resolution&gt;
 *       &lt;/LODInfo&gt;
 * </code>
 * </pre>
 *
 * @author Gabriel Roldan
 * @version 1.0
 */
@Setter
public class LODInfo  implements   java.io.Serializable {

    /**
     * 级别ID，表示瓦片的缩放级别
     */
    private int levelID;

    /**
     * 比例尺，表示该级别的地图比例尺
     */
    private double scale;

    /**
     * 分辨率，表示该级别下每个像素代表的实际距离
     */
    private double resolution;

    /**
     * 获取级别ID
     *
     * @return 级别ID
     */
    public int getLevelID() {
        return levelID;
    }

    /**
     * 获取比例尺
     *
     * @return 比例尺值
     */
    public double getScale() {
        return scale;
    }

    /**
     * 获取分辨率
     *
     * @return 分辨率值
     */
    public double getResolution() {
        return resolution;
    }
}
