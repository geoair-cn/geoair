package cn.geoair.map.dynamic.file.csv;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

final class CsvGeometrySupport {

    private CsvGeometrySupport() {}

    static Geometry readGeometry(CsvLinkInfo linkInfo, List<String> headers, String[] values) {
        if (linkInfo.getGeometryMode() == CsvGeometryMode.NONE) {
            return null;
        }
        if (linkInfo.getGeometryMode() == CsvGeometryMode.WKT) {
            int wktIndex = CsvSchemaSupport.findColumnIndex(headers, linkInfo.getWktColumnName());
            if (wktIndex < 0 || wktIndex >= values.length) {
                throw new IllegalArgumentException("未找到 WKT 列：" + linkInfo.getWktColumnName());
            }
            String wkt = values[wktIndex];
            if (wkt == null || wkt.trim().isEmpty()) {
                return null;
            }
            Geometry geometry =
                    GirGeoTools.defaultInstance().getFormatOpt().wktToJtsGeometry(wkt, false);
            if (geometry != null) {
                geometry.setSRID(linkInfo.getSrid());
            }
            return geometry;
        }

        int lonIndex = CsvSchemaSupport.findColumnIndex(headers, linkInfo.getLongitudeColumnName());
        int latIndex = CsvSchemaSupport.findColumnIndex(headers, linkInfo.getLatitudeColumnName());
        if (lonIndex < 0
                || latIndex < 0
                || lonIndex >= values.length
                || latIndex >= values.length) {
            throw new IllegalArgumentException(
                    "未找到经纬度列："
                            + linkInfo.getLongitudeColumnName()
                            + "/"
                            + linkInfo.getLatitudeColumnName());
        }
        String lonValue = values[lonIndex];
        String latValue = values[latIndex];
        if (lonValue == null
                || lonValue.trim().isEmpty()
                || latValue == null
                || latValue.trim().isEmpty()) {
            return null;
        }
        double lon = Double.parseDouble(lonValue.trim());
        double lat = Double.parseDouble(latValue.trim());
        Point point =
                GirGeoTools.defaultInstance()
                        .getGeom2ArrayOpt()
                        .doubleArrayToPoint(new double[] {lon, lat});
        point.setSRID(linkInfo.getSrid());
        return point;
    }

    static String writeGeometryValue(CsvLinkInfo linkInfo, Geometry geometry, boolean longitude) {
        if (geometry == null) {
            return "";
        }
        if (linkInfo.getGeometryMode() == CsvGeometryMode.WKT) {
            return GirGeoTools.defaultInstance()
                    .getFormatOpt()
                    .jtsGeometryToWktString(geometry, false);
        }
        if (!(geometry instanceof Point)) {
            throw new IllegalArgumentException("经纬度模式仅支持 Point 几何导出");
        }
        Coordinate coordinate = ((Point) geometry).getCoordinate();
        return String.valueOf(longitude ? coordinate.getX() : coordinate.getY());
    }
}
