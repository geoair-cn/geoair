package cn.geoair.map.dynamic.tools.grid.dto;

/**
 * 语义明确的瓦片索引范围。
 *
 * <p>本类型的四个边界均为闭区间，即 {@code [minX, maxX] × [minY, maxY]}。
 * 它用于新代码，避免 {@link RangeApo} 的历史上下界语义被误用。旧 API 保持不变，
 * 以兼容既有瓦片融合任务。</p>
 *
 * @author 张逢吉
 */
public final class TileRange {

    /** 缩放级别。 */
    private final int z;

    /** Y 行号原点约定。 */
    private final TileYAxis yAxis;

    /** 包含的最小 X 索引。 */
    private final int minX;

    /** 包含的最大 X 索引。 */
    private final int maxX;

    /** 包含的最小 Y 索引。 */
    private final int minY;

    /** 包含的最大 Y 索引。 */
    private final int maxY;

    private TileRange(int z, int minX, int maxX, int minY, int maxY, TileYAxis yAxis) {
        if (z < 0 || minX > maxX || minY > maxY) {
            throw new IllegalArgumentException("非法的闭区间瓦片范围");
        }
        if (yAxis == null) {
            throw new IllegalArgumentException("Y轴约定不能为空");
        }
        this.z = z;
        this.yAxis = yAxis;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    /** 创建闭区间瓦片范围。 */
    public static TileRange closed(int z, int minX, int maxX, int minY, int maxY) {
        return closed(z, minX, maxX, minY, maxY, TileYAxis.XYZ);
    }

    /** 创建带有明确 Y 轴约定的闭区间瓦片范围。 */
    public static TileRange closed(
            int z, int minX, int maxX, int minY, int maxY, TileYAxis yAxis) {
        return new TileRange(z, minX, maxX, minY, maxY, yAxis);
    }

    /**
     * 将历史范围转换为闭区间。
     *
     * <p>geo-tools 的历史 {@code tileRangeByBox/tileRangeByGeom} 使用
     * {@code ceil} 产生上界，因此此处将最大 X/Y 视为排除上界。该转换方法只供
     * 新增的 V2 API 使用，不能用于 fuser 自行构造的 {@link RangeApo}。</p>
     */
    public static TileRange fromGeoToolsExclusiveMax(RangeApo legacyRange) {
        if (legacyRange == null) {
            throw new IllegalArgumentException("历史瓦片范围不能为空");
        }
        return closed(legacyRange.getZ(), legacyRange.getMinX(), legacyRange.getMaxX() - 1,
                legacyRange.getMinY(), legacyRange.getMaxY() - 1);
    }

    /** 返回闭区间覆盖的瓦片数量。 */
    public long getTileCount() {
        return ((long) maxX - minX + 1) * ((long) maxY - minY + 1);
    }

    /**
     * 返回 Y 轴翻转后的闭区间范围。
     *
     * @param tileRowCount 当前层级的总行数
     * @return 使用相反 Y 轴原点的瓦片范围
     */
    public TileRange reverseY(int tileRowCount) {
        if (tileRowCount <= maxY) {
            throw new IllegalArgumentException("瓦片总行数小于范围最大Y索引");
        }
        TileYAxis targetYAxis = yAxis == TileYAxis.XYZ ? TileYAxis.TMS : TileYAxis.XYZ;
        return closed(z, minX, maxX, tileRowCount - 1 - maxY, tileRowCount - 1 - minY, targetYAxis);
    }

    public int getZ() {
        return z;
    }

    public TileYAxis getYAxis() {
        return yAxis;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }
}
