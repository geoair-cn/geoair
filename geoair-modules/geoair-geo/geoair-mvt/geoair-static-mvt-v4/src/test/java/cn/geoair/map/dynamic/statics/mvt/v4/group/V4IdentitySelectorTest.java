package cn.geoair.map.dynamic.statics.mvt.v4.group;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * {@link V4IdentitySelector} 的性质测试。
 *
 * <p>这个算子承担两件事，都必须是"性质"而不是"实现细节"，所以测试也按性质写：</p>
 * <ol>
 *   <li><b>保留的确实是最靠前的那一批</b>：任何被保留的行，优先序都不劣于任何被丢弃的行；</li>
 *   <li><b>与喂入方式无关</b>：分块喂（模拟溢写）与一次性喂、打乱顺序喂，结果相同 ——
 *       这条是"溢写不影响产物"的根据（见 {@code V4开发计划.md} 7.8）。</li>
 * </ol>
 *
 * <p>之所以写这条测试：堆的比较方向写反过一次，症状是"同一配置两次跑结果一致、换溢写阈值结果就变"，
 * 非常容易误判成"溢写本来就会影响产物"。</p>
 *
 * @author 张逢吉
 */
public class V4IdentitySelectorTest {

    private static final MvtLayerSliceParameter LAYER = layer();

    private static MvtLayerSliceParameter layer() {
        MvtLayerSliceParameter layer = new MvtLayerSliceParameter();
        layer.setLayerName("poi");
        layer.setGeomFieldName("geom");
        return layer;
    }

    private static V4OrderedRow row(int gid, long sequence, int rowIndex) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("gid", gid);
        return new V4OrderedRow(sequence, rowIndex, GirAdvOneRow.ofByMap(values));
    }

    /** 优先序（排名 → 输入序号 → 批内下标），越小越该保留。 */
    private static Comparator<V4OrderedRow> priority() {
        return (left, right) -> {
            int rankLeft = V4IdentitySelector.rankOf(left.getRow(), LAYER);
            int rankRight = V4IdentitySelector.rankOf(right.getRow(), LAYER);
            if (rankLeft != rankRight) {
                return Integer.compare(rankLeft, rankRight);
            }
            if (left.getSequence() != right.getSequence()) {
                return Long.compare(left.getSequence(), right.getSequence());
            }
            return Integer.compare(left.getRowIndex(), right.getRowIndex());
        };
    }

    private static List<V4OrderedRow> sampleRows(int count) {
        Random random = new Random(20260920L);
        List<V4OrderedRow> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            // gid 连号（与真实数据一致：GeoJSON 的 gid 就是连着编的）
            rows.add(row(805022 + i, i, random.nextInt(3)));
        }
        return rows;
    }

    private static Set<Integer> gidsOf(List<V4OrderedRow> rows) {
        Set<Integer> gids = new LinkedHashSet<>();
        for (V4OrderedRow ordered : rows) {
            gids.add((Integer) ordered.getRow().get("gid"));
        }
        return gids;
    }

    @Test
    public void keepsExactlyLimitAndDominates() {
        List<V4OrderedRow> rows = sampleRows(5000);
        int limit = 500;

        List<V4OrderedRow> kept = V4IdentitySelector.keepSmallest(rows, limit, LAYER);
        assertEquals(limit, kept.size());

        // 注意：选择结果会重新包装 V4OrderedRow，不能用对象身份判断"谁被留下了"，按 gid 比
        Set<Integer> keptGids = new HashSet<>(gidsOf(kept));
        Comparator<V4OrderedRow> priority = priority();
        V4OrderedRow worstKept = null;
        V4OrderedRow bestDropped = null;
        for (V4OrderedRow candidate : rows) {
            if (keptGids.contains((Integer) candidate.getRow().get("gid"))) {
                if (worstKept == null || priority.compare(candidate, worstKept) > 0) {
                    worstKept = candidate;
                }
            } else if (bestDropped == null || priority.compare(candidate, bestDropped) < 0) {
                bestDropped = candidate;
            }
        }
        assertTrue("保留的行比丢弃的行更靠后，说明取的不是最小的 N 个", bestDropped == null
                || priority.compare(worstKept, bestDropped) <= 0);
    }

    @Test
    public void chunkedFeedMatchesSingleFeed() {
        List<V4OrderedRow> rows = sampleRows(5000);
        int limit = 500;
        Set<Integer> expected = gidsOf(V4IdentitySelector.keepSmallest(rows, limit, LAYER));

        // 分块喂：模拟"每写一个 run 就削一次"
        V4IdentitySelector.Bounded bounded = new V4IdentitySelector.Bounded(limit, LAYER);
        int chunk = 137;
        for (int i = 0; i < rows.size(); i += chunk) {
            bounded.offerAll(new ArrayList<>(rows.subList(i, Math.min(rows.size(), i + chunk))));
        }
        assertEquals(expected, gidsOf(bounded.toList()));
    }

    @Test
    public void shuffledInputMatchesOriginalOrder() {
        List<V4OrderedRow> rows = sampleRows(5000);
        int limit = 500;
        Set<Integer> expected = gidsOf(V4IdentitySelector.keepSmallest(rows, limit, LAYER));

        List<V4OrderedRow> shuffled = new ArrayList<>(rows);
        Collections.shuffle(shuffled, new Random(7L));
        assertEquals(expected, gidsOf(V4IdentitySelector.keepSmallest(shuffled, limit, LAYER)));
    }

    @Test
    public void missingIdentityFallsBackToSequence() {
        // 取不到身份值时排名全是 MISSING_RANK，此时优先序退化成 (输入序号, 批内下标) —— 仍然是确定的全序
        List<V4OrderedRow> rows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            rows.add(new V4OrderedRow(i, 0, GirAdvOneRow.ofByMap(new LinkedHashMap<>())));
        }
        List<V4OrderedRow> kept = V4IdentitySelector.keepSmallest(rows, 4, LAYER);
        assertEquals(4, kept.size());
        assertEquals(0L, kept.get(0).getSequence());
        assertEquals(3L, kept.get(3).getSequence());
    }
}
