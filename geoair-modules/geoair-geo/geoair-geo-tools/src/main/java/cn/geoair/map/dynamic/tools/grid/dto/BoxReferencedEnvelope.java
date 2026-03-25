package cn.geoair.map.dynamic.tools.grid.dto;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.geometry.MismatchedDimensionException;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/14 19:33 @description： 包围框的实现类
 */
public class BoxReferencedEnvelope extends ReferencedEnvelope {

    int srid;

    public int getSrid() {
        return srid;
    }

    public BoxReferencedEnvelope(org.locationtech.jts.geom.Envelope envelope, int srid)
            throws MismatchedDimensionException {
        super(envelope, GirAdvTools.getSridOpt().getCRS(srid));
        this.srid = srid;
    }

    public String getWktString(int targetSrid) {
        Geometry geometry = GirAdvTools.getSridOpt().convertToGeom(this, srid, targetSrid);
        return GirAdvTools.getFormatOpt().jtsGeometryToWktString(geometry, true);
    }

    @Override
    public String toString() {
        Geometry geometry = GirAdvTools.getSridOpt().convertToGeom(this);
        return this.getSrid()
                + ";"
                + GirAdvTools.getFormatOpt().jtsGeometryToWktString(geometry, true);
    }
}
