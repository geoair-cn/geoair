package cn.geoair.map.dynamic.adv.query.apo;


import cn.geoair.gtc.base.Gir;
import cn.geoair.map.dynamic.tools.GirAdvTools;

import org.locationtech.jts.geom.*;

import java.io.Serializable;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/11 08:57
 * @description： 查询边界的Api Object
 */

public class BBoxApo implements Serializable {

    public BBoxApo(double[] bboxArray, double[] bboxArrayGs, Integer thisSrid) {
        this.bboxArray = bboxArray;
        this.thisSrid = thisSrid == null ? 0 : thisSrid;
        this.envelope = new Envelope();
        if (bboxArray.length != 4) {
            Gir.log.info("Error: bboxArray.length!=4");
        } else {
            this.envelope.init(bboxArray[0], bboxArray[2], bboxArray[1], bboxArray[3]);
            this.maxx = this.envelope.getMaxX();
            this.maxy = this.envelope.getMaxY();
            this.minx = this.envelope.getMinX();
            this.miny = this.envelope.getMinY();
        }
        if (bboxArrayGs.length != 4) {
            Gir.log.info("Error: bboxArray.length!=4");
        } else {
            this.bboxArrayGs = bboxArrayGs;
            this.envelopeGs = new Envelope();
            this.envelopeGs.init(bboxArrayGs[0], bboxArrayGs[2], bboxArrayGs[1], bboxArrayGs[3]);
            this.maxxGs = this.envelopeGs.getMaxX();
            this.maxyGs = this.envelopeGs.getMaxY();
            this.minxGs = this.envelopeGs.getMinX();
            this.minyGs = this.envelopeGs.getMinY();
        }
    }

    /**
     * 当前的边界的坐标
     */
    int thisSrid;

    /**
     * 边界数组
     */
    double[] bboxArray;
    /**
     * 边界数组4326坐标
     */
    double[] bboxArrayGs;

    /**
     * geotools的边界
     */
    Envelope envelope;

    /**
     * geotools的边界4326坐标
     */
    Envelope envelopeGs;


    private double minx;


    private double maxx;


    private double miny;

    private double maxy;

    private double minxGs;


    private double maxxGs;


    private double minyGs;

    private double maxyGs;

    public double[] getBboxArrayGs() {
        return bboxArrayGs;
    }

    public Envelope getEnvelopeGs() {
        return envelopeGs;
    }

    public double getMinxGs() {
        return minxGs;
    }

    public double getMaxxGs() {
        return maxxGs;
    }

    public double getMinyGs() {
        return minyGs;
    }

    public double getMaxyGs() {
        return maxyGs;
    }

    public int getThisSrid() {
        return thisSrid;
    }

    public double[] getBboxArray() {
        return bboxArray;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public double getMinx() {
        return minx;
    }

    public double getMaxx() {
        return maxx;
    }

    public double getMiny() {
        return miny;
    }

    public double getMaxy() {
        return maxy;
    }

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    /**
     * 检查边界范围是否有效（minx < maxx 且 miny < maxy）
     *
     * @return 有效返回true，否则返回false
     */
    public boolean isValidBbox() {
        return (maxx > minx) && (maxy > miny);
    }

    public Polygon geJtsPolygon(int srid, Envelope envelope) {
        if (!isValidBbox()) {
            Gir.log.warn("无效的边界范围，无法转换为Polygon");
            return null;
        }
        // 构建矩形的4个顶点（顺时针顺序）
        Coordinate[] coordinates = new Coordinate[5];  // 第5个点与第1个点相同，用于闭合
        coordinates[0] = new Coordinate(envelope.getMinX(), envelope.getMinY());  // 左下
        coordinates[1] = new Coordinate(envelope.getMaxX(), envelope.getMinY());  // 右下
        coordinates[2] = new Coordinate(envelope.getMaxX(), envelope.getMaxY());  // 右上
        coordinates[3] = new Coordinate(envelope.getMinX(), envelope.getMaxY());  // 左上
        coordinates[4] = coordinates[0];              // 闭合点

        // 创建线性环（LinearRing）作为多边形的外环
        LinearRing linearRing = GEOMETRY_FACTORY.createLinearRing(coordinates);

        // 创建多边形（无内环）
        Polygon polygon = GEOMETRY_FACTORY.createPolygon(linearRing);

        // 设置SRID
        polygon.setSRID(srid);

        return polygon;
    }

    /**
     * 获取带SRID的WKT字符串（如：SRID=4326;POLYGON ((...))）
     * 用于需要显式声明坐标系的场景（如PostGIS空间查询）
     *
     * @return 带SRID的WKT字符串，若边界无效则返回空字符串
     */
    public String getWktStringWithSrid() {
        if (thisSrid <= 0) {
            Gir.log.warn("SRID未设置（或无效），无法生成带SRID的WKT");
            return getWktString();
        }
        String wkt = getWktString();
        return wkt.isEmpty() ? "" : "SRID=" + thisSrid + ";" + wkt;
    }

    /**
     * 将边界范围转换为WKT字符串（如：POLYGON ((minx miny, maxx miny, maxx maxy, minx maxy, minx miny))）
     *
     * @return WKT字符串，若边界无效则返回空字符串
     */
    public String getWktString() {
        Polygon polygon = geJtsPolygon(thisSrid, envelope);
        if (polygon == null) {
            return "";
        }
        // 使用WKTWriter转换为字符串
        return GirAdvTools.getFormatOpt().jtsGeometryToWktString(polygon, true);
    }

    /**
     * 将4326边界范围转换为WKT字符串（如：POLYGON ((minx miny, maxx miny, maxx maxy, minx maxy, minx miny))）
     *
     * @return WKT字符串，若边界无效则返回空字符串
     */
    public String getGsWktString() {
        Polygon polygon = geJtsPolygon(4326, envelopeGs);
        if (polygon == null) {
            return "";
        }
        // 使用WKTWriter转换为字符串
        return GirAdvTools.getFormatOpt().jtsGeometryToWktString(polygon, true);
    }
}
