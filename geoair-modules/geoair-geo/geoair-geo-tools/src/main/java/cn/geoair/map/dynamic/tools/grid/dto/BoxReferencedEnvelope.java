package cn.geoair.map.dynamic.tools.grid.dto;

import cn.geoair.map.dynamic.tools.GirGeoTools;

import org.geotools.api.geometry.MismatchedDimensionException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;

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
        super(envelope, GirGeoTools.me().getSridOpt().getCRS(srid));
        this.srid = srid;
    }

    public String getWktString(int targetSrid) {
        Geometry geometry = GirGeoTools.me().getSridOpt().convertToGeom(this, srid, targetSrid);
        return GirGeoTools.me().getFormatOpt().jtsGeometryToWktString(geometry, true);
    }

    @Override
    public String toString() {
        Geometry geometry = GirGeoTools.me().getSridOpt().convertToGeom(this);
        return this.getSrid()
                + ";"
                + GirGeoTools.me().getFormatOpt().jtsGeometryToWktString(geometry, true);
    }
}
