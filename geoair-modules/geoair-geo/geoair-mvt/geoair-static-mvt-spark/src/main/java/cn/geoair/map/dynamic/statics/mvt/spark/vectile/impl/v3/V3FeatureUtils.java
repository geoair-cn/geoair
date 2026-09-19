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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
     * 按空间密度丢弃要素：低密度格子整格保留，高密度格子削到同一个上限。
     * <p>
     * <b>不能按"密度升序排序后取前 N 个"</b>：名额用尽时，排在后面的高密度格子会被整格丢弃。
     * 密度网格只有 10×10，一格就是瓦片的十分之一宽，地图上就是成片的空白块 ——
     * 和"低密度优先"本想避免的空洞是同一个后果。
     * <p>
     * 改为给每个格子求一个保留上限（取满足总量不超限的最大值）：稀疏格子一个不丢，
     * 密集格子按同一上限削，任何格子都不会整片消失，效果就是把密度拉平。
     * <p>
     * 格子内部按要素身份排名取最小的若干个，与聚合阶段安全阀的抽样口径一致；
     * 若改成按几何 WKT 排序取前缀，格子内会变成"按经度方向截断"，多出一层方向性偏差。
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

        Map<String, List<GirAdvOneRow>> cells = new HashMap<>();
        for (GirAdvOneRow row : rows) {
            Geometry geometry = row.getGeometry(geomField);
            String cellId = cellId(geometry, envelope, cellWidth, cellHeight);
            cells.computeIfAbsent(cellId, key -> new ArrayList<>()).add(row);
        }

        int cap = resolveCellCap(cells, limit);
        List<GirAdvOneRow> result = new ArrayList<>(Math.min(limit, rows.size()));
        for (List<GirAdvOneRow> cell : cells.values()) {
            if (cell.size() <= cap) {
                result.addAll(cell);
            } else {
                result.addAll(selectByCellRank(cell, cap, geomField, idField));
            }
        }
        return result;
    }

    /**
     * 求「每格保留 {@code min(格内要素数, cap)} 个」意义下、总量不超过 {@code limit} 的最大 cap。
     * <p>格子数远小于要素数，二分代价可以忽略。连 cap=1 都超限（格子数比 limit 还多）时返回 1
     * 并接受略微超出 —— 总比让一部分格子整片消失好。
     */
    private static int resolveCellCap(Map<String, List<GirAdvOneRow>> cells, int limit) {
        int max = 0;
        for (List<GirAdvOneRow> cell : cells.values()) {
            max = Math.max(max, cell.size());
        }
        int low = 1;
        int high = max;
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            if (cappedTotal(cells, mid) <= limit) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    private static long cappedTotal(Map<String, List<GirAdvOneRow>> cells, int cap) {
        long total = 0L;
        for (List<GirAdvOneRow> cell : cells.values()) {
            total += Math.min(cell.size(), cap);
        }
        return total;
    }

    /** 格子内部按身份排名取最小的 {@code keep} 个 */
    private static List<GirAdvOneRow> selectByCellRank(
            List<GirAdvOneRow> cell, int keep, String geomField, String idField) {
        List<RankedRow> ranked = new ArrayList<>(cell.size());
        for (GirAdvOneRow row : cell) {
            ranked.add(new RankedRow(identityRank(row, geomField, idField), row));
        }
        ranked.sort(null);
        List<GirAdvOneRow> result = new ArrayList<>(keep);
        for (int i = 0; i < keep; i++) {
            result.add(ranked.get(i).row);
        }
        return result;
    }

    /**
     * 取一行的身份标识：优先图层配置的 id 字段，取不到时退回该行第一个非几何字段。
     * <p>刻意不用几何字段做身份：{@code Geometry.hashCode} 虽有稳定的结构哈希，但要遍历全部
     * 坐标，而所有调用点都在削减热路径上，一个复杂多边形就足以拖垮整条链路。
     */
    static Object resolveIdentity(GirAdvOneRow row, String geomField, String idField) {
        if (idField != null && !idField.trim().isEmpty()) {
            Object id = row.get(idField);
            if (id != null) {
                return id;
            }
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (geomField != null && geomField.equals(entry.getKey())) {
                continue;
            }
            if (entry.getValue() != null) {
                return entry.getValue();
            }
        }
        return null;
    }

    /** 要素的身份排名；取不到身份值时排到最后，优先被削掉 */
    static int identityRank(GirAdvOneRow row, String geomField, String idField) {
        Object identity = resolveIdentity(row, geomField, idField);
        return identity == null ? Integer.MAX_VALUE : spread(identity.hashCode());
    }

    /**
     * 把身份哈希做一次雪崩混合（Murmur3 finalizer）。
     * <p>不能直接用 {@code hashCode()}：数据源的 id 常常逐个递增（GeoJSON 的 {@code gid}
     * 就是 805022、805023 这样连着编的），直接用得到的是连着的一段，抽出来照样是连片要素。
     */
    static int spread(int hash) {
        int h = hash;
        h ^= (h >>> 16);
        h *= 0x85ebca6b;
        h ^= (h >>> 13);
        h *= 0xc2b2ae35;
        h ^= (h >>> 16);
        return h & Integer.MAX_VALUE;
    }

    /** 身份排名与行的组合，用于按排名取最小的若干个 */
    static final class RankedRow implements Comparable<RankedRow> {

        final int rank;

        final GirAdvOneRow row;

        RankedRow(int rank, GirAdvOneRow row) {
            this.rank = rank;
            this.row = row;
        }

        @Override
        public int compareTo(RankedRow other) {
            return Integer.compare(rank, other.rank);
        }
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
}
