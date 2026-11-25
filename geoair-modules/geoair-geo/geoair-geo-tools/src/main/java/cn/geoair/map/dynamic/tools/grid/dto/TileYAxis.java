package cn.geoair.map.dynamic.tools.grid.dto;

/**
 * 瓦片 Y 行号的原点约定。
 *
 * <p>本枚举只描述 Y 轴方向，不描述瓦片服务协议、坐标参考系或缓存格式： {@link #XYZ} 的 {@code y=0} 位于网格顶部，{@link #TMS} 的 {@code
 * y=0} 位于网格底部。
 *
 * @author 张逢吉
 */
public enum TileYAxis {

    /** XYZ / Google 风格，Y 行号原点位于左上角。 */
    XYZ,

    /** TMS 风格，Y 行号原点位于左下角。 */
    TMS;

    /**
     * 将当前约定下的 Y 行号转换为目标约定。
     *
     * @param y 当前 Y 行号
     * @param tileRowCount 当前层级的总行数，必须来自实际网格定义
     * @param target 目标 Y 轴约定
     * @return 目标约定下的 Y 行号
     */
    public int convertY(int y, int tileRowCount, TileYAxis target) {
        if (target == null) {
            throw new IllegalArgumentException("目标Y轴约定不能为空");
        }
        if (tileRowCount <= 0 || y < 0 || y >= tileRowCount) {
            throw new IllegalArgumentException("Y行号或瓦片总行数不合法");
        }
        return this == target ? y : tileRowCount - 1 - y;
    }

    /**
     * 将瓦片边界线的 Y 索引转换为目标约定。
     *
     * <p>与 {@link #convertY(int, int, TileYAxis)} 不同，边界索引允许取到 {@code tileRowCount}，用于计算瓦片上、下边界坐标。
     */
    public int convertBoundaryY(int y, int tileRowCount, TileYAxis target) {
        if (target == null) {
            throw new IllegalArgumentException("目标Y轴约定不能为空");
        }
        if (tileRowCount <= 0 || y < 0 || y > tileRowCount) {
            throw new IllegalArgumentException("Y边界索引或瓦片总行数不合法");
        }
        return this == target ? y : tileRowCount - y;
    }

    /**
     * @return 当前约定的 Y 行号原点是否位于网格顶部。
     */
    public boolean isTopLeft() {
        return this == XYZ;
    }
}
