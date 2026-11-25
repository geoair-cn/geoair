package cn.geoair.map.dynamic.tools.grid.dto;

import lombok.Data;

import org.locationtech.jts.geom.Envelope;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/14 18:28 @description： 瓦片索引范围。
 *     <p>四个索引均为闭区间：{@code [minX, maxX] × [minY, maxY]}。因此使用本对象 枚举瓦片时，最大 X/Y
 *     必须参与遍历。该约定与既有融合、预缓存和瓦片列表链路一致。
 */
@Data
public class RangeApo {

    /** 包含的最小 X 索引。 */
    private int minX;

    /** 包含的最大 X 索引。 */
    private int maxX;

    /** 包含的最小 Y 索引。 */
    private int minY;

    /** 包含的最大 Y 索引。 */
    private int maxY;

    /** 缩放级别。 */
    private int z;

    private Envelope envelope;

    public RangeApo(double xMinNum, double xMaxNum, double yMinNum, double yMaxNum, int z) {
        Envelope envelope = new Envelope(xMinNum, xMaxNum, yMinNum, yMaxNum);
        this.envelope = envelope;
        this.minX = (int) envelope.getMinX();
        this.maxX = (int) envelope.getMaxX();
        this.minY = (int) envelope.getMinY();
        this.maxY = (int) envelope.getMaxY();
        this.z = z;
    }

    public RangeApo(int xMinNum, int xMaxNum, int yMinNum, int yMaxNum, int z) {
        Envelope envelope = new Envelope(xMinNum, xMaxNum, yMinNum, yMaxNum);
        this.envelope = envelope;
        this.minX = (int) envelope.getMinX();
        this.maxX = (int) envelope.getMaxX();
        this.minY = (int) envelope.getMinY();
        this.maxY = (int) envelope.getMaxY();
        this.z = z;
    }

    public RangeApo(long xMinNum, long xMaxNum, long yMinNum, long yMaxNum, int z) {
        Envelope envelope = new Envelope(xMinNum, xMaxNum, yMinNum, yMaxNum);
        this.envelope = envelope;
        this.minX = (int) envelope.getMinX();
        this.maxX = (int) envelope.getMaxX();
        this.minY = (int) envelope.getMinY();
        this.maxY = (int) envelope.getMaxY();
        this.z = z;
    }

    public int getZ() {
        return z;
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

    public Envelope getEnvelope() {
        return envelope;
    }
}
