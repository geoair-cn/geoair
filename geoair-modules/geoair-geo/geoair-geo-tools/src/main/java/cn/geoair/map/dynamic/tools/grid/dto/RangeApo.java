package cn.geoair.map.dynamic.tools.grid.dto;

import org.locationtech.jts.geom.Envelope;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/14 18:28 @description： 瓦片索引范围 （xmin/xmax: 瓦片X索引；ymin/ymax: 瓦片Y索引）
 */
public class RangeApo {

    private int minX;

    private int maxX;

    private int minY;

    private int maxY;
    private int z;

    private Envelope envelope;

    public RangeApo(double xMinNum, double xMaxNum, double yMinNum, double yMaxNum,int z) {
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

    @Override
    public String toString() {
        return "RangeApo{"
                + "maxY="
                + maxY
                + ", minY="
                + minY
                + ", maxX="
                + maxX
                + ", minX="
                + minX
                + '}';
    }
}
