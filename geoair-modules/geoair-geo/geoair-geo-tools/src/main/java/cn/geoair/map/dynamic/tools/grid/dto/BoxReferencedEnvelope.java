package cn.geoair.map.dynamic.tools.grid.dto;


import cn.geoair.map.dynamic.tools.GirGeoTools;
import lombok.Getter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.geotools.api.geometry.BoundingBox;

import org.geotools.api.geometry.MismatchedDimensionException;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/14 19:33 @description： 包围框的实现类
 */
@Getter
public class BoxReferencedEnvelope extends ReferencedEnvelope {

    /**
     * 当前的范围框的坐标系
     */
    int thisSrid;

    public BoxReferencedEnvelope(Envelope envelope, int thisSrid)
            throws MismatchedDimensionException {
        super(envelope, GirGeoTools.defaultInstance().getSridOpt().getCRS(thisSrid));
        this.thisSrid = thisSrid;
    }

    public String getWktString(int targetSrid) {
        Geometry geometry = GirGeoTools.defaultInstance().getSridOpt().convertToGeom(this, thisSrid, targetSrid);
        return GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(geometry, true);
    }

    public BoundingBox getBoundingBox() {
        return this;
    }

    public org.geotools.api.geometry.Bounds getOpenGisEnvelope() {
        return this;
    }

    public Envelope getJtsEnvelope() {
        return this;
    }

    @Override
    public String toString() {
        Geometry geometry = GirGeoTools.defaultInstance().getSridOpt().convertToGeom(this);
        return this.getThisSrid()
                + ";"
                + GirGeoTools.defaultInstance().getFormatOpt().jtsGeometryToWktString(geometry, true);
    }
}
