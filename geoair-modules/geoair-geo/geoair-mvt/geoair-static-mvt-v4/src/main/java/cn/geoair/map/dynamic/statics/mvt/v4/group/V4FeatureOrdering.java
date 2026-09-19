package cn.geoair.map.dynamic.statics.mvt.v4.group;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.V3TileFeatureGroup;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 瓦片内要素的排序策略，对应 tippecanoe 的 {@code --preserve-input-order} /
 * {@code --reorder} / {@code --hilbert}。
 *
 * <p>三者的优先级：{@code hilbert} &gt; {@code reorder} &gt; {@code preserveInputOrder} &gt; 不排序。
 * 空间排序与"保持输入顺序"本身是互斥的诉求，同时打开没有意义，因此这里定成固定优先级
 * 而不是报错 —— 报错会让"顺手都勾上"的配置直接失败，而这时用户想要的很明显是空间重排。</p>
 *
 * @author 张逢吉
 */
public final class V4FeatureOrdering {

    /** Hilbert 曲线量化到 2^16 网格（16 位，够区分同瓦片内的要素）。 */
    private static final int HILBERT_BITS = 16;

    private V4FeatureOrdering() {
    }

    /**
     * 按配置对同一图层的要素排序。
     *
     * @param rows         待排序的行（会被就地排序）
     * @param hilbert      是否按 Hilbert 曲线序
     * @param reorder      是否按空间网格序
     * @param keepInputOrder 是否保持输入顺序
     * @param layer        图层参数（取几何字段名）
     * @param envelope     瓦片范围（空间排序的归一化基准）
     */
    public static void sort(List<V4OrderedRow> rows, boolean hilbert, boolean reorder,
            boolean keepInputOrder, MvtLayerSliceParameter layer, Envelope envelope) {
        if (rows == null || rows.size() < 2) {
            return;
        }
        if (hilbert) {
            rows.sort(Comparator.comparingLong(
                    ordered -> hilbertIndex(ordered, layer, envelope)));
            return;
        }
        if (reorder) {
            rows.sort(Comparator.comparing(
                    ordered -> gridKey(ordered, layer, envelope)));
            return;
        }
        if (keepInputOrder) {
            rows.sort(Comparator.comparingLong(V4OrderedRow::getSequence));
        }
    }

    /** Hilbert 曲线索引：先把要素质心归一化到 2^16 网格，再算曲线序。 */
    private static long hilbertIndex(V4OrderedRow ordered, MvtLayerSliceParameter layer, Envelope envelope) {
        Coordinate centroid = centroidOf(ordered, layer, envelope);
        int x = quantize(centroid.x, envelope.getMinX(), envelope.getWidth());
        int y = quantize(centroid.y, envelope.getMinY(), envelope.getHeight());
        return hilbertXY2D(HILBERT_BITS, x, y);
    }

    /**
     * 空间网格序：把归一化坐标交错成可比较的字符串键。
     * <p>等价于按"先纬度后经度"的网格序，比 Hilbert 便宜（无曲线计算），
     * 但相邻要素的聚集程度略差。</p>
     */
    private static String gridKey(V4OrderedRow ordered, MvtLayerSliceParameter layer, Envelope envelope) {
        Coordinate centroid = centroidOf(ordered, layer, envelope);
        int x = quantize(centroid.x, envelope.getMinX(), envelope.getWidth());
        int y = quantize(centroid.y, envelope.getMinY(), envelope.getHeight());
        return String.format("%05d:%05d", y, x);
    }

    /** 取要素质心；几何不可用时退化为瓦片左下角，保证排序稳定不抛异常。 */
    private static Coordinate centroidOf(V4OrderedRow ordered, MvtLayerSliceParameter layer, Envelope envelope) {
        Geometry geometry = ordered.getRow().getGeometry(layer.getGeomFieldName());
        if (geometry == null || geometry.isEmpty()) {
            return new Coordinate(envelope.getMinX(), envelope.getMinY());
        }
        Coordinate centroid = geometry.getCentroid().getCoordinate();
        return centroid == null ? new Coordinate(envelope.getMinX(), envelope.getMinY()) : centroid;
    }

    /** 把世界坐标归一化到 [0, 2^bits-1]。 */
    private static int quantize(double value, double origin, double span) {
        if (span <= 0 || Double.isNaN(span)) {
            return 0;
        }
        double ratio = (value - origin) / span;
        if (ratio <= 0) {
            return 0;
        }
        if (ratio >= 1) {
            return (1 << HILBERT_BITS) - 1;
        }
        return (int) Math.floor(ratio * ((1 << HILBERT_BITS) - 1));
    }

    /** 标准 Hilbert d↔(x,y) 映射（迭代版，避免递归开销）。 */
    private static long hilbertXY2D(int bits, int x, int y) {
        long index = 0L;
        for (int s = bits - 1; s >= 0; s--) {
            int rx = (x >> s) & 1;
            int ry = (y >> s) & 1;
            index += (1L << s) * (1L << s) * ((3 * rx) ^ ry);
            // 旋转象限
            if (ry == 0) {
                if (rx == 1) {
                    x = (1 << bits) - 1 - x;
                    y = (1 << bits) - 1 - y;
                }
                int tmp = x;
                x = y;
                y = tmp;
            }
        }
        return index;
    }

    /** 把带序号的行还原成编码器需要的行列表。 */
    public static List<GirAdvOneRow> toRows(List<V4OrderedRow> ordered) {
        List<GirAdvOneRow> rows = new ArrayList<>(ordered.size());
        for (V4OrderedRow item : ordered) {
            rows.add(item.getRow());
        }
        return rows;
    }

    /** 该图层下当前这些行是否还需要排序（避免无谓的排序开销）。 */
    public static boolean needsSort(boolean hilbert, boolean reorder, boolean keepInputOrder, int size) {
        return size > 1 && (hilbert || reorder || keepInputOrder);
    }

    /** 供 {@link V3TileFeatureGroup} 无法表达的场景兜底：空行列表。 */
    public static List<GirAdvOneRow> emptyRows() {
        return new ArrayList<>();
    }
}
