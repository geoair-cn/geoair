package cn.geoair.map.dynamic.statics.mvt.v4.group;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * V4 的「一个瓦片里保留哪些要素」算子：按 <b>(身份排名, 输入序号, 批内下标)</b> 取最小的 N 个。
 *
 * <p>这是 V4 与 V3 在聚合阶段最本质的一处不同。V3 把削减放在 Spark 的 {@code reduceByKey} 里，
 * 同一块瓦片每来一批记录就削一次，于是"最终留下多少"取决于<b>削了多少次</b>
 * （实测不溢写时触发 1,373 次、每瓦片停在 N~2N；V4 溢写时只触发 648 次、每瓦片被压到恰好 N，
 * 同一份数据产出差 18%）。那是 V3 的结构决定的，V4 没有这个包袱，就不该继承这个毛病。</p>
 *
 * <p>V4 的做法是把它定义成一个<b>与顺序、次数、溢写位置都无关</b>的算子：</p>
 * <ul>
 *   <li><b>全序而不是偏序</b>：先比身份排名，再比输入序号，最后比"同一要素落在这块瓦片上的第几行"。
 *       三元组唯一确定一个行，所以没有并列、没有平局。</li>
 *   <li><b>可结合、可交换、幂等</b>：{@code 取最小 N 个} 满足这三条，
 *       因此"削 N 次"和"削一次"结果相同，"先溢写再削"和"直接削"结果也相同 ——
 *       溢写对产物完全透明，这是 V4 要的性质（见 {@code V4开发计划.md} 7.8）。</li>
 *   <li><b>有界</b>：选取用容量为 N 的最大堆，流式喂入，内存只有 N 行。
 *       归并时不再需要"先把整块瓦片的并集堆起来再削"（实测 62 个 run 时那样会把堆撑爆）。</li>
 * </ul>
 *
 * <p>身份取值规则与 V3 的 {@code V3FeatureUtils.resolveIdentity} 对齐（{@code idFieldName} 优先，
 * 否则取行内第一个非几何字段），排名同样做一次雪崩混合 —— 数据源的 id 常常是连号
 * （GeoJSON 的 {@code gid} 就是 805022、805023 连着编的），不混合的话"取最小的 N 个"
 * 会退化成"取一段连续的 id"。V3 那份是包级私有的，V4 按第五节"共用类只读"的原则自己写一份。</p>
 *
 * @author 张逢吉
 */
public final class V4IdentitySelector {

    /** 取不到身份值时的排名：排到最后，优先被削掉。 */
    public static final int MISSING_RANK = Integer.MAX_VALUE;

    private V4IdentitySelector() {
    }

    /** 计算一行的身份排名。同一次任务内对同一行恒定（纯函数）。 */
    public static int rankOf(GirAdvOneRow row, MvtLayerSliceParameter layer) {
        Object identity = resolveIdentity(row, layer.getGeomFieldName(), layer.getIdFieldName());
        return identity == null ? MISSING_RANK : spread(identity.hashCode());
    }

    /** 身份取值：idFieldName 优先，否则取行内第一个非几何字段。 */
    private static Object resolveIdentity(GirAdvOneRow row, String geomField, String idField) {
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

    /**
     * 把身份哈希做一次雪崩混合（Murmur3 finalizer）。
     * <p>不能直接用 {@code hashCode()}：连号 id 直接取模会得到"一段一段"的区间，
     * 配上按位置顺序编号的数据，抽出来照样是连片的要素，等于没削。混合之后连续输入才会落到互不相邻的位置。</p>
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

    /**
     * 一次性选取：保留优先序最小的 {@code keep} 行。
     *
     * <p>不足 {@code keep} 行时原样返回（不复制），这是最常见的分支 —— 高层级每块瓦片通常远远不到上限。</p>
     */
    public static List<V4OrderedRow> keepSmallest(
            List<V4OrderedRow> rows, int keep, MvtLayerSliceParameter layer) {
        if (rows == null || rows.isEmpty()) {
            return rows == null ? new ArrayList<>() : rows;
        }
        if (keep <= 0) {
            return new ArrayList<>();
        }
        if (rows.size() <= keep) {
            return rows;
        }
        Bounded bounded = new Bounded(keep, layer);
        bounded.offerAll(rows);
        return bounded.toList();
    }

    /**
     * 有界选择器：容量 N，流式喂入，始终只持有优先序最小的 N 行。
     *
     * <p>堆里存的是"当前最差的排在最前"，来一行比堆顶更好就换掉堆顶，比堆顶更差就直接丢 ——
     * 单行成本 O(log N)，内存 O(N)。这样"归并一路 tile 的所有 run"与"只归并一个 run"
     * 在内存上没有区别，差别只在 CPU。</p>
     */
    public static final class Bounded {

        /** 堆元素：把排名算一次记下来，避免比较时反复算哈希。 */
        private static final class Entry {

            private final int rank;
            private final long sequence;
            private final int rowIndex;
            private final GirAdvOneRow row;

            private Entry(int rank, long sequence, int rowIndex, GirAdvOneRow row) {
                this.rank = rank;
                this.sequence = sequence;
                this.rowIndex = rowIndex;
                this.row = row;
            }
        }

        /**
         * 优先序升序：排名 → 输入序号 → 批内下标。三元组唯一，所以这是全序，没有并列。
         * <p>越小越该被保留 —— {@link Bounded} 的堆是它的<b>倒序</b>（堆顶 = 当前最差的一行）。</p>
         */
        private static int compareByPriority(Entry left, Entry right) {
            if (left.rank != right.rank) {
                return Integer.compare(left.rank, right.rank);
            }
            if (left.sequence != right.sequence) {
                return Long.compare(left.sequence, right.sequence);
            }
            return Integer.compare(left.rowIndex, right.rowIndex);
        }

        /** 倒序比较：让 {@link PriorityQueue} 的堆顶是"最差"的那一行。 */
        private static int compareWorstFirst(Entry left, Entry right) {
            return compareByPriority(right, left);
        }

        private final int capacity;
        private final MvtLayerSliceParameter layer;
        private final PriorityQueue<Entry> worstFirst;

        public Bounded(int capacity, MvtLayerSliceParameter layer) {
            this.capacity = Math.max(1, capacity);
            this.layer = layer;
            this.worstFirst = new PriorityQueue<>(this.capacity, Bounded::compareWorstFirst);
        }

        public void offer(V4OrderedRow ordered) {
            Entry candidate = new Entry(
                    rankOf(ordered.getRow(), layer), ordered.getSequence(), ordered.getRowIndex(), ordered.getRow());
            if (worstFirst.size() < capacity) {
                worstFirst.add(candidate);
                return;
            }
            Entry worst = worstFirst.peek();
            // 只有"比当前最差的还靠前"才换进来 —— 这一行写反过一次：写反时留下的是 N 个最差的行，
            // 而且因为削减时机随溢写位置变化，产物会跟着溢写跑（表现是"同配置两次一致、换配置就不一致"）。
            if (worst != null && compareByPriority(candidate, worst) < 0) {
                worstFirst.poll();
                worstFirst.add(candidate);
            }
        }

        public void offerAll(List<V4OrderedRow> rows) {
            if (rows == null) {
                return;
            }
            for (V4OrderedRow ordered : rows) {
                offer(ordered);
            }
        }

        public int size() {
            return worstFirst.size();
        }

        /** 按优先序升序返回当前保留的行（新建列表；容量不足时就是喂进来的全部）。 */
        public List<V4OrderedRow> toList() {
            List<Entry> ordered = new ArrayList<>(worstFirst);
            ordered.sort(Bounded::compareByPriority);
            List<V4OrderedRow> rows = new ArrayList<>(ordered.size());
            for (Entry entry : ordered) {
                rows.add(new V4OrderedRow(entry.sequence, entry.rowIndex, entry.row));
            }
            return rows;
        }
    }
}
