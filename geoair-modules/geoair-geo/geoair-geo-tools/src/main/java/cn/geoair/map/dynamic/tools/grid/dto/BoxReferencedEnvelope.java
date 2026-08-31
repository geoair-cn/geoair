package cn.geoair.map.dynamic.tools.grid.dto;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import lombok.Getter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.MismatchedDimensionException;

/**
 * 携带 SRID 的 JTS 包围盒。
 *
 * <p>构造时会根据 {@code thisSrid} 解析 CRS。该类型仅表示轴对齐范围；转换到其他 SRID 时通过 {@link #getWktString(int)}
 * 先转换包围盒，再将其构造成矩形几何对象。
 *
 * @author 张逢吉
 */
@Getter
public class BoxReferencedEnvelope extends ReferencedEnvelope {

    /** 当前包围盒坐标的 EPSG SRID。 */
    private final int thisSrid;

    /**
     * 创建携带坐标参考系的包围盒。
     *
     * @param envelope 包围盒坐标
     * @param thisSrid 包围盒坐标的 EPSG SRID
     * @throws MismatchedDimensionException 包围盒维度与 CRS 不匹配时抛出
     */
    public BoxReferencedEnvelope(Envelope envelope, int thisSrid)
            throws MismatchedDimensionException {
        super(envelope, GirGeoTools.defaultInstance().getSridOpt().getCRS(thisSrid));
        this.thisSrid = thisSrid;
    }

    /**
     * 获取指定 SRID 下的矩形边界 WKT。
     *
     * @param targetSrid 输出坐标的 EPSG SRID
     * @return 转换后包围盒构成的多边形 WKT
     */
    public String getWktString(int targetSrid) {
        Geometry geometry =
                GirGeoTools.defaultInstance()
                        .getSridOpt()
                        .convertToGeom(this, thisSrid, targetSrid);
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(geometry, true);
    }

    /** @return 当前对象的 OpenGIS BoundingBox 视图。 */
    public BoundingBox getBoundingBox() {
        return this;
    }

    /** @return 当前对象的 OpenGIS Envelope 视图。 */
    public org.opengis.geometry.Envelope getOpenGisEnvelope() {
        return this;
    }

    /** @return 当前对象的 JTS Envelope 视图。 */
    public Envelope getJtsEnvelope() {
        return this;
    }

    @Override
    public String toString() {
        Geometry geometry = GirGeoTools.defaultInstance().getSridOpt().convertToGeom(this);
        return this.getThisSrid()
                + ";"
                + GirGeoTools.defaultInstance()
                        .getFormatOpt()
                        .jtsGeometryToWktString(geometry, true);
    }
}
