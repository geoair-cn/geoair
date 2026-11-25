package cn.geoair.map.tile.forge.fuser.fuser;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;

/** 将经纬度边界按目标网格的像素中心转换成裁切路径。 */
final class TileBoundaryMask {
    private final STRtree edges = new STRtree();
    private final double east;

    TileBoundaryMask(Geometry geometry) {
        east = geometry.getEnvelopeInternal().getMaxX();
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            Polygon polygon = (Polygon) geometry.getGeometryN(i);
            index(polygon.getExteriorRing().getCoordinates());
            for (int hole = 0; hole < polygon.getNumInteriorRing(); hole++)
                index(polygon.getInteriorRingN(hole).getCoordinates());
        }
        edges.build();
    }

    private void index(Coordinate[] ring) {
        for (int i = 1; i < ring.length; i++) {
            Coordinate a = ring[i - 1], b = ring[i];
            if (a.y != b.y) edges.insert(new Envelope(a, b), new LineSegment(a, b));
        }
    }

    /** null 表示整张参与，空路径表示没有任何像素参与。 */
    Path2D mask(Envelope tile, boolean mercator, int size, boolean outside) {
        double[] latitudes = latitudes(tile, mercator, size);
        boolean[] eastInside = new boolean[size];
        List<List<Integer>> cuts = new ArrayList<>(Collections.nCopies(size, null));
        double west = tile.getMinX(), right = tile.getMaxX();
        double scale = size / tile.getWidth();
        // 从选区右侧沿每行向左扫描，跨过外环或孔洞的边都翻转一次。
        Envelope rayRange =
                new Envelope(west, Math.max(right, east), tile.getMinY(), tile.getMaxY());
        for (Object item : edges.query(rayRange)) {
            LineSegment edge = (LineSegment) item;
            Coordinate a = edge.p0, b = edge.p1;
            int start = firstBelow(latitudes, Math.max(a.y, b.y));
            int end = firstBelow(latitudes, Math.min(a.y, b.y));
            for (int row = start; row < end; row++) {
                double x = a.x + (latitudes[row] - a.y) / (b.y - a.y) * (b.x - a.x);
                if (x < west) continue;
                if (x >= right) {
                    eastInside[row] = !eastInside[row];
                    continue;
                }
                List<Integer> rowCuts = cuts.get(row);
                if (rowCuts == null) {
                    rowCuts = new ArrayList<>();
                    cuts.set(row, rowCuts);
                }
                rowCuts.add((int) Math.ceil((x - west) * scale - .5));
            }
        }
        Path2D path = new Path2D.Double();
        int pixels = 0;
        for (int row = 0; row < size; row++) {
            List<Integer> rowCuts = cuts.get(row);
            boolean visible = eastInside[row] ^ outside;
            int end = size;
            if (rowCuts != null) {
                rowCuts.sort(Collections.reverseOrder());
                for (int cut : rowCuts) {
                    if (visible) pixels += append(path, cut, end, row);
                    end = cut;
                    visible = !visible;
                }
            }
            if (visible) pixels += append(path, 0, end, row);
        }
        return pixels == size * size ? null : path;
    }

    private static int append(Path2D path, int left, int right, int row) {
        if (right == left) return 0;
        path.moveTo(left, row);
        path.lineTo(right, row);
        path.lineTo(right, row + 1);
        path.lineTo(left, row + 1);
        path.closePath();
        return right - left;
    }

    private static int firstBelow(double[] values, double limit) {
        int first = 0, end = values.length;
        while (first < end) {
            int mid = (first + end) >>> 1;
            if (values[mid] < limit) end = mid;
            else first = mid + 1;
        }
        return first;
    }

    private static double[] latitudes(Envelope tile, boolean mercator, int size) {
        double north = mercator ? mercatorY(tile.getMaxY()) : tile.getMaxY();
        double south = mercator ? mercatorY(tile.getMinY()) : tile.getMinY();
        double[] rows = new double[size];
        for (int row = 0; row < size; row++) {
            double y = north - (row + .5) / size * (north - south);
            rows[row] = mercator ? Math.toDegrees(Math.atan(Math.sinh(y))) : y;
        }
        return rows;
    }

    private static double mercatorY(double latitude) {
        return Math.log(Math.tan(Math.PI / 4 + Math.toRadians(latitude) / 2));
    }
}
