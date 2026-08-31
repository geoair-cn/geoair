package cn.geoair.map.tile.forge.core.bygwc.config;

/**
 * 表示ArcGIS缓存配置文件中的{@code TileOrigin}元素。
 *
 * <p>瓦片网格的左上角点。瓦片原点通常不是开始创建瓦片的位置； 这只发生在地图的完整范围内。通常瓦片原点位于地图很远的外部，
 * 以确保地图区域能够被覆盖，并且具有相同瓦片原点的其他缓存可以叠加到您的缓存上。
 *
 * <p>XML结构:
 *
 * <pre>
 * <code>
 *     &lt;TileOrigin xsi:type='typens:PointN'&gt;
 *       &lt;X&gt;-4020900&lt;/X&gt;
 *       &lt;Y&gt;19998100&lt;/Y&gt;
 *     &lt;/TileOrigin&gt;
 * </code>
 * </pre>
 *
 * @author Gabriel Roldan
 */
public class TileOrigin implements java.io.Serializable {

    /** X坐标值 */
    private double X;

    /** Y坐标值 */
    private double Y;

    /**
     * 获取X坐标值
     *
     * @return X坐标值
     */
    public double getX() {
        return X;
    }

    /**
     * 获取Y坐标值
     *
     * @return Y坐标值
     */
    public double getY() {
        return Y;
    }

    /**
     * 设置X坐标值
     *
     * @param x X坐标值
     */
    public void setX(double x) {
        X = x;
    }

    /**
     * 设置Y坐标值
     *
     * @param y Y坐标值
     */
    public void setY(double y) {
        Y = y;
    }
}
