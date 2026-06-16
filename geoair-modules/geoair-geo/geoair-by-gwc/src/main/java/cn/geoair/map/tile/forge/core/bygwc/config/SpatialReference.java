
package cn.geoair.map.tile.forge.core.bygwc.config;

import lombok.Data;

/**
 * 表示缓存配置文件中的 {@code SpatialReference} 元素。
 *
 * <p>XML 结构示例: <code>
 * <pre>
 * &lt;SpatialReference xsi:type='typens:ProjectedCoordinateSystem'&gt;
 *       &lt;WKT&gt;PROJCS[&quot;NZGD_2000_New_Zealand_Transverse_Mercator&quot;,GEOGCS[&quot;GCS_NZGD_2000&quot;,DATUM[&quot;D_NZGD_2000&quot;,SPHEROID[&quot;GRS_1980&quot;,6378137.0,298.257222101]],PRIMEM[&quot;Greenwich&quot;,0.0],UNIT[&quot;Degree&quot;,0.0174532925199433]],PROJECTION[&quot;Transverse_Mercator&quot;],PARAMETER[&quot;False_Easting&quot;,1600000.0],PARAMETER[&quot;False_Northing&quot;,10000000.0],PARAMETER[&quot;Central_Meridian&quot;,173.0],PARAMETER[&quot;Scale_Factor&quot;,0.9996],PARAMETER[&quot;Latitude_Of_Origin&quot;,0.0],UNIT[&quot;Meter&quot;,1.0],AUTHORITY[&quot;EPSG&quot;,2193]]&lt;/WKT&gt;
 *       &lt;XOrigin&gt;-4020900&lt;/XOrigin&gt;
 *       &lt;YOrigin&gt;1900&lt;/YOrigin&gt;
 *       &lt;XYScale&gt;450445547.3910538&lt;/XYScale&gt;
 *       &lt;ZOrigin&gt;0&lt;/ZOrigin&gt;
 *       &lt;ZScale&gt;1&lt;/ZScale&gt;
 *       &lt;MOrigin&gt;-100000&lt;/MOrigin&gt;
 *       &lt;MScale&gt;10000&lt;/MScale&gt;
 *       &lt;XYTolerance&gt;0.0037383177570093459&lt;/XYTolerance&gt;
 *       &lt;ZTolerance&gt;2&lt;/ZTolerance&gt;
 *       &lt;MTolerance&gt;2&lt;/MTolerance&gt;
 *       &lt;HighPrecision&gt;true&lt;/HighPrecision&gt;
 *       &lt;WKID&gt;2193&lt;/WKID&gt;
 *       &lt;LatestWKID&gt;2193&lt;/LatestWKID&gt;
 * &lt;/SpatialReference&gt;
 * </pre>
 * </code>
 *
 * @author Gabriel Roldan
 */
@Data
public class SpatialReference   implements   java.io.Serializable{

    /**
     * 空间参考系统的 Well-Known Text (WKT) 表示形式
     */
    private String WKT;

    /**
     * X 坐标的原点值
     */
    private double XOrigin;

    /**
     * Y 坐标的原点值
     */
    private double YOrigin;

    /**
     * XY 坐标的比例因子
     */
    private double XYScale;

    /**
     * Z 坐标的原点值
     */
    private double ZOrigin;

    /**
     * Z 坐标的比例因子
     */
    private double ZScale;

    /**
     * M 值的原点
     */
    private double MOrigin;

    /**
     * M 值的比例因子
     */
    private double MScale;

    /**
     * XY 坐标的容差值
     */
    private double XYTolerance;

    /**
     * Z 坐标的容差值
     */
    private double ZTolerance;

    /**
     * M 值的容差
     */
    private double MTolerance;

    /**
     * 是否启用高精度坐标
     */
    private boolean HighPrecision;

    /**
     * 坐标系的 WKID 标识符
     */
    private int WKID;

    /**
     * 最新的 WKID 标识符（ArcGIS 10.1+版本新增）
     */
    private int LatestWKID;

    /**
     * 左侧经度值（似乎仅在 ArcGIS 9.2 格式中存在）
     */
    private double LeftLongitude;

    /**
     * 获取空间参考系统的 WKT 表示形式
     *
     * @return WKT 字符串
     */
    public String getWKT() {
        return WKT;
    }

    /**
     * 获取 X 坐标的原点值
     *
     * @return X 原点值
     */
    public double getXOrigin() {
        return XOrigin;
    }

    /**
     * 获取 Y 坐标的原点值
     *
     * @return Y 原点值
     */
    public double getYOrigin() {
        return YOrigin;
    }

    /**
     * 获取 XY 坐标的比例因子
     *
     * @return XY 比例因子
     */
    public double getXYScale() {
        return XYScale;
    }

    /**
     * 获取 Z 坐标的原点值
     *
     * @return Z 原点值
     */
    public double getZOrigin() {
        return ZOrigin;
    }

    /**
     * 获取 Z 坐标的比例因子
     *
     * @return Z 比例因子
     */
    public double getZScale() {
        return ZScale;
    }

    /**
     * 获取 M 值的原点
     *
     * @return M 原点值
     */
    public double getMOrigin() {
        return MOrigin;
    }

    /**
     * 获取 M 值的比例因子
     *
     * @return M 比例因子
     */
    public double getMScale() {
        return MScale;
    }

    /**
     * 获取 XY 坐标的容差值
     *
     * @return XY 容差值
     */
    public double getXYTolerance() {
        return XYTolerance;
    }

    /**
     * 获取 Z 坐标的容差值
     *
     * @return Z 容差值
     */
    public double getZTolerance() {
        return ZTolerance;
    }

    /**
     * 获取 M 值的容差
     *
     * @return M 容差值
     */
    public double getMTolerance() {
        return MTolerance;
    }

    /**
     * 判断是否启用高精度坐标
     *
     * @return 如果启用高精度则返回 true，否则返回 false
     */
    public boolean isHighPrecision() {
        return HighPrecision;
    }

    /**
     * 获取坐标系的 WKID 标识符
     *
     * @return WKID 标识符
     */
    public int getWKID() {
        return WKID;
    }

    /**
     * 获取最新的 WKID 标识符（ArcGIS 10.1+版本新增）
     *
     * @return 最新的 WKID 标识符
     */
    public int getLatestWKID() {
        return LatestWKID;
    }

    /**
     * 获取左侧经度值（似乎仅在 ArcGIS 9.2 格式中存在）
     *
     * @return 左侧经度值
     */
    public double getLeftLongitude() {
        return LeftLongitude;
    }
}
