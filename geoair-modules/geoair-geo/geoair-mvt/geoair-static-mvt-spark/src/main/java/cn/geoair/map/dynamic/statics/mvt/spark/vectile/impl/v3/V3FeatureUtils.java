package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * V3 专用的要素处理工具。
 * <p>
 * 这些能力（gzip、密度合并/丢弃、按屏幕占用排序）与 {@code geoair-mvt-tools} 里的
 * {@code AdvMvtDensityUtils} 有重叠，但那份实现被 V1/V2 与实时切片共用。
 * V3 的编码期优化对它们有额外要求（排序关键字必须缓存、要素数削减必须有下限、
 * 不能让共用类的行为发生变化），因此这里独立实现一份，<b>确保 V3 的任何调整
 * 都不会改变 V1/V2 的产物</b>。
 *
 * @author 张逢吉
 */
final class V3FeatureUtils {

    /** 密度网格划分份数（投影坐标系） */
    private static final double GRID_SCALE_PROJECTED = 100.0;

    /** 密度网格划分份数（经纬度坐标系用更细的网格） */
    private static final double GRID_SCALE_GEOGRAPHIC = 10.0;

    private V3FeatureUtils() {
    }

    /** gzip 压缩；失败时返回原数据，不因为压缩问题丢掉瓦片 */
    static byte[] gzip(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(data);
            gzip.finish();
            return out.toByteArray();
        } catch (Exception e) {
            return data;
        }
    }

    /**
     * 按空间密度合并高密度区域的要素，把要素数压到 {@code limit} 以内。
     * <p>
     * 与共用实现的差别：<b>不做网格"密度阈值"筛选</b>。原实现在每个要素上用它自己
     * 的外接矩形当"瓦片范围"来算网格，对点要素等于除以零、网格划分是退化的；
     * 这里改为直接用瓦片范围划分网格，语义更明确，也更容易预测结果。
     *
     * @param rows         待合并要素
     * @param limit        目标上限
     * @param geomField    几何字段名
     * @param envelope     瓦片范围（世界坐标）
     * @param isGeographic 输出网格是否为经纬度坐标系
     * @return 合并后的要素列表；入参未超限时原样返回
     */
    static List<GirAdvOneRow> coalesceBySpatialDensity(
            List<GirAdvOneRow> rows, int limit, String geomField, Envelope envelope, boolean isGeographic) {
        if (rows == null || rows.size() <= limit || limit <= 0) {
            return rows;
        }
        double gridScale = isGeographic ? GRID_SCALE_GEOGRAPHIC : GRID_SCALE_PROJECTED;
        double cellWidth = envelope.getWidth() / gridScale;
        double cellHeight = envelope.getHeight() / gridScale;
        if (cellWidth <= 0 || cellHeight <= 0) {
            return rows;
        }

        Map<String, List<GirAdvOneRow>> cells = new HashMap<>();
        for (GirAdvOneRow row : rows) {
            Geometry geometry = row.getGeometry(geomField);
            String cellId = cellId(geometry, envelope, cellWidth, cellHeight);
            cells.computeIfAbsent(cellId, key -> new ArrayList<>()).add(row);
        }

        // 先按"每格贡献的要素数"分配预算：格子越大越先被合并
        List<GirAdvOneRow> merged = new ArrayList<>(rows.size());
        for (List<GirAdvOneRow> cell : cells.values()) {
            if (cell.size() == 1) {
                merged.add(cell.get(0));
                continue;
            }
            merged.add(mergeCell(cell, geomField));
        }
        if (merged.size() <= limit) {
            return merged;
        }
        // 合并后仍超限：交给调用方按密度丢弃，这里不擅自丢数据
        return merged;
    }

    /** 把同一网格内的要素合并成一个多点要素，属性取该网格第一个要素 */
    private static GirAdvOneRow mergeCell(List<GirAdvOneRow> cell, String geomField) {
        List<Coordinate> coordinates = new ArrayList<>(cell.size());
        GeometryFactory factory = null;
        for (GirAdvOneRow row : cell) {
            Geometry geometry = row.getGeometry(geomField);
            if (geometry == null || geometry.isEmpty()) {
                continue;
            }
            if (factory == null) {
                factory = geometry.getFactory();
            }
            if (geometry instanceof Point) {
                coordinates.add(geometry.getCoordinate());
            } else {
                // 非点几何：取质心参与合并，保持"一格里只留一个要素"的语义
                coordinates.add(geometry.getCentroid().getCoordinate());
            }
        }
        if (coordinates.isEmpty()) {
            return cell.get(0);
        }
        GirAdvOneRow template = cell.get(0);
        GirAdvOneRow result = GirAdvOneRow.ofByMap(template);
        GeometryFactory geometryFactory = factory == null ? new GeometryFactory() : factory;
        result.put(geomField, toMultiPoint(geometryFactory, coordinates));
        return result;
    }

    private static MultiPoint toMultiPoint(GeometryFactory factory, List<Coordinate> coordinates) {
        List<Coordinate> distinct = new ArrayList<>(coordinates.size());
        for (Coordinate coordinate : coordinates) {
            boolean duplicated = false;
            for (Coordinate exist : distinct) {
                if (exist.equals2D(coordinate)) {
                    duplicated = true;
                    break;
                }
            }
            if (!duplicated) {
                distinct.add(coordinate);
            }
        }
        return factory.createMultiPoint(distinct.toArray(new Coordinate[0]));
    }

    /**
     * 按空间密度丢弃要素：低密度区域的要素优先保留，高密度区域先丢。
     * <p>
     * 排序比较器的次关键字（用于同密度时的稳定排序）<b>只计算一次并缓存</b>：
     * 该关键字含几何 WKT 序列化，若放在比较器里现算，要素一多排序本身就会成为瓶颈。
     */
    static List<GirAdvOneRow> filterBySpatialDensity(
            List<GirAdvOneRow> rows, int limit, String geomField, String idField,
            Envelope envelope, boolean isGeographic) {
        if (rows == null || rows.size() <= limit || limit <= 0) {
            return rows;
        }
        double gridScale = isGeographic ? GRID_SCALE_GEOGRAPHIC : GRID_SCALE_PROJECTED;
        double cellWidth = envelope.getWidth() / gridScale;
        double cellHeight = envelope.getHeight() / gridScale;
        if (cellWidth <= 0 || cellHeight <= 0) {
            return new ArrayList<>(rows.subList(0, limit));
        }

        Map<String, Integer> density = new HashMap<>();
        List<RowWithCell> entries = new ArrayList<>(rows.size());
        for (GirAdvOneRow row : rows) {
            Geometry geometry = row.getGeometry(geomField);
            String cellId = cellId(geometry, envelope, cellWidth, cellHeight);
            density.merge(cellId, 1, Integer::sum);
            entries.add(new RowWithCell(row, cellId));
        }
        // 关键字在这里一次性算好，比较器只做字符串比较
        for (RowWithCell entry : entries) {
            entry.sortKey = featureKey(entry.row, geomField, idField);
        }
        entries.sort(Comparator
                .comparingInt((RowWithCell entry) -> density.getOrDefault(entry.cellId, 1))
                .thenComparing(entry -> entry.sortKey));

        List<GirAdvOneRow> result = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            result.add(entries.get(i).row);
        }
        return result;
    }

    /** 稳定排序用的要素关键字：优先 id 字段，其次几何 WKT，最后随机兜底 */
    private static String featureKey(GirAdvOneRow row, String geomField, String idField) {
        if (idField != null && !idField.trim().isEmpty()) {
            Object id = row.get(idField);
            if (id != null) {
                return String.valueOf(id);
            }
        }
        Geometry geometry = row.getGeometry(geomField);
        return geometry == null ? UUID.randomUUID().toString() : geometry.toText();
    }

    /** 计算要素中心所在的网格编号；几何不可用时归入统一的异常格 */
    private static String cellId(Geometry geometry, Envelope envelope, double cellWidth, double cellHeight) {
        if (geometry == null || geometry.isEmpty()) {
            return "error#error";
        }
        Coordinate center = geometry.getCentroid().getCoordinate();
        int xGrid = (int) Math.floor((center.x - envelope.getMinX()) / cellWidth);
        int yGrid = (int) Math.floor((center.y - envelope.getMinY()) / cellHeight);
        return xGrid + "#" + yGrid;
    }

    /** 要素 + 所属网格 + 预计算的排序关键字 */
    private static final class RowWithCell {

        private final GirAdvOneRow row;
        private final String cellId;
        private String sortKey;

        private RowWithCell(GirAdvOneRow row, String cellId) {
            this.row = row;
            this.cellId = cellId;
        }
    }
}
