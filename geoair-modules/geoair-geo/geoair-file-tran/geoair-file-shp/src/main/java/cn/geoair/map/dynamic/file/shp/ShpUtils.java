package cn.geoair.map.dynamic.file.shp;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * @author ：张俊
 * @date ：Created in 2026/3/25 14:27
 * @description： TODO
 */
public class ShpUtils {
    private Class<? extends Geometry> getActualGeometryClass(SimpleFeatureType type) {
        if (Point.class.isAssignableFrom(type.getGeometryDescriptor().getType().getBinding())) {
            return Point.class;
        } else if (LineString.class.isAssignableFrom(type.getGeometryDescriptor().getType().getBinding())) {
            return LineString.class;
        } else if (Polygon.class.isAssignableFrom(type.getGeometryDescriptor().getType().getBinding())) {
            return Polygon.class;
        } else {
            return Point.class; // 默认 fallback
        }
    }
}

