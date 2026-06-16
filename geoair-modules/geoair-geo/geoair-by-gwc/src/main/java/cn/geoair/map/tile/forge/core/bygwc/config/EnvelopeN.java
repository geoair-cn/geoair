
package cn.geoair.map.tile.forge.core.bygwc.config;

public class EnvelopeN  implements   java.io.Serializable {

    // 最小x坐标值
    private double xmin;

    // 最小y坐标值
    private double ymin;

    // 最大x坐标值
    private double xmax;

    // 最大y坐标值
    private double ymax;

    // 空间参考系统
    private SpatialReference spatialReference;

    /**
     * 获取最小x坐标值
     * @return xmin 最小x坐标值
     */
    public double getXmin() {
        return xmin;
    }

    /**
     * 获取最小y坐标值
     * @return ymin 最小y坐标值
     */
    public double getYmin() {
        return ymin;
    }

    /**
     * 获取最大x坐标值
     * @return xmax 最大x坐标值
     */
    public double getXmax() {
        return xmax;
    }

    /**
     * 获取最大y坐标值
     * @return ymax 最大y坐标值
     */
    public double getYmax() {
        return ymax;
    }

    /**
     * 获取空间参考系统
     * @return spatialReference 空间参考系统
     */
    public SpatialReference getSpatialReference() {
        return spatialReference;
    }
}
