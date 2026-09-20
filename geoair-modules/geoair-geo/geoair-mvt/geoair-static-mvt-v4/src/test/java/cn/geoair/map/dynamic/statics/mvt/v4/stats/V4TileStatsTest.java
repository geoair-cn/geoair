package cn.geoair.map.dynamic.statics.mvt.v4.stats;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json.AttributeStat;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json.LayerStat;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json.TileStats;
import cn.geoair.map.dynamic.statics.mvt.v4.group.V4OrderedRow;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * {@link V4TileStats} 的口径测试。
 *
 * <p>锁住三件容易在一次"顺手优化"里被改坏的事：</p>
 * <ol>
 *   <li><b>一个源要素只计一次</b>：同一序号在多块瓦片里重复出现（这是切片的常态，
 *       不是异常），统计不能跟着放大；</li>
 *   <li><b>取值上限只挡新值</b>：已达上限后已知值的次数还要继续累计，
 *       否则"出现最多的值"会因为上限而失真；</li>
 *   <li><b>统计的字段 = 实际写进 PBF 的字段</b>：判据必须与多图层编码器一致。</li>
 * </ol>
 *
 * @author 张逢吉
 */
public class V4TileStatsTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private static MvtLayerSliceParameter layer() {
        MvtLayerSliceParameter layer = new MvtLayerSliceParameter();
        layer.setLayerName("poi");
        layer.setGeomFieldName("geom");
        return layer;
    }

    private static Point point(double x, double y) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(x, y));
    }

    private static V4OrderedRow row(long sequence, Point geometry, Object... keyValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            values.put((String) keyValues[i], keyValues[i + 1]);
        }
        if (geometry != null) {
            values.put("geom", geometry);
        }
        return new V4OrderedRow(sequence, 0, GirAdvOneRow.ofByMap(values));
    }

    private static LayerStat onlyLayer(TileStats stats) {
        assertNotNull(stats);
        assertEquals(1, stats.getLayerCount());
        assertEquals(1, stats.getLayers().size());
        return stats.getLayers().get(0);
    }

    private static AttributeStat attribute(LayerStat layer, String name) {
        for (AttributeStat stat : layer.getAttributes()) {
            if (name.equals(stat.getAttribute())) {
                return stat;
            }
        }
        throw new AssertionError("统计里没有字段 " + name + "，实际有 " + layer.getAttributes().size() + " 个");
    }

    @Test
    public void countsEachSourceFeatureOnceAcrossTiles() {
        V4TileStats stats = new V4TileStats(Collections.singletonList(layer()));

        // 同一个要素（序号 7）落进三块瓦片，每块里还是两行（一个要素被切成了两行）
        stats.onRows("poi", Arrays.asList(row(7, point(1, 1), "gid", 1), row(7, point(1, 1), "gid", 1)));
        stats.onRows("poi", Collections.singletonList(row(7, point(1, 1), "gid", 1)));
        stats.onRows("poi", Collections.singletonList(row(8, point(2, 2), "gid", 2)));

        LayerStat stat = onlyLayer(stats.toTileStats());
        assertEquals("源要素数应为 2，而不是瓦片行数 4", 2L, stat.getCount());
        assertEquals("gid 的出现次数也应只有 2", 2L, attribute(stat, "gid").getCount());
    }

    @Test
    public void keepsCountingKnownValuesAfterCap() {
        V4TileStats stats = new V4TileStats(Collections.singletonList(layer()));

        List<V4OrderedRow> rows = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            rows.add(row(i, null, "gid", i));
        }
        // 上限之后，再出现的已知值仍然要累加
        for (int i = 0; i < 3; i++) {
            rows.add(row(1000 + i, null, "gid", 0));
        }
        stats.onRows("poi", rows);

        AttributeStat gid = attribute(onlyLayer(stats.toTileStats()), "gid");
        assertEquals(153L, gid.getCount());
        assertEquals(V4TileStats.MAX_VALUE_COUNT_PER_FIELD, gid.getValues().size());
        assertEquals("已知值在达到上限后仍须累加", 4L, (long) gid.getStatics().get(0));
        assertEquals(0, gid.getValues().get(0));
    }

    @Test
    public void reportsTypeAndNumericRange() {
        V4TileStats stats = new V4TileStats(Collections.singletonList(layer()));
        stats.onRows("poi", Arrays.asList(
                row(0, null, "height", 12.5, "name", "a"),
                row(1, null, "height", 3, "name", "b"),
                row(2, null, "height", 40.25, "name", "a")));

        LayerStat stat = onlyLayer(stats.toTileStats());
        AttributeStat height = attribute(stat, "height");
        assertEquals("Number", height.getType());
        assertEquals(3.0, height.getMin().doubleValue(), 0.0);
        assertEquals(40.25, height.getMax().doubleValue(), 0.0);

        AttributeStat name = attribute(stat, "name");
        assertEquals("String", name.getType());
        assertEquals("字符串字段没有数值范围", null, name.getMin());
        assertEquals("a 出现两次", 2L, (long) name.getStatics().get(0));
    }

    @Test
    public void reportsGeometryTypeOnce() {
        V4TileStats stats = new V4TileStats(Collections.singletonList(layer()));
        stats.onRows("poi", Arrays.asList(row(0, point(1, 1), "gid", 1), row(1, point(2, 2), "gid", 2)));
        assertEquals("Point", onlyLayer(stats.toTileStats()).getGeometry());

        // 几何整列缺失时如实退回 Unknown，而不是假装成 Point
        V4TileStats noGeometry = new V4TileStats(Collections.singletonList(layer()));
        noGeometry.onRows("poi", Collections.singletonList(row(0, null, "gid", 1)));
        assertEquals("Unknown", onlyLayer(noGeometry.toTileStats()).getGeometry());
    }

    @Test
    public void countsOnlyFieldsThatGoIntoPbf() {
        MvtLayerSliceParameter filtered = layer();
        filtered.setExcludeFields(new ArrayList<>(Collections.singletonList("secret")));

        V4TileStats stats = new V4TileStats(Collections.singletonList(filtered));
        stats.onRows("poi", Collections.singletonList(
                row(0, point(1, 1), "gid", 1, "secret", "x", "name", "a")));

        LayerStat stat = onlyLayer(stats.toTileStats());
        List<String> fields = new ArrayList<>();
        for (AttributeStat attribute : stat.getAttributes()) {
            fields.add(attribute.getAttribute());
        }
        assertTrue("排除字段不该出现在统计里", fields.contains("gid") && fields.contains("name"));
        assertFalse(fields.contains("secret"));
        assertFalse("几何字段不是输出属性", fields.contains("geom"));

        // 白名单模式：只报 includeFields 与 sysIncludeFields 里的字段，id 字段强制保留
        MvtLayerSliceParameter whitelist = layer();
        whitelist.setIncludeFields(new ArrayList<>(Collections.singletonList("name")));
        whitelist.setSysIncludeFields(new java.util.LinkedHashSet<>(Collections.singletonList("sys")));
        whitelist.setIdFieldName("code");
        V4TileStats limited = new V4TileStats(Collections.singletonList(whitelist));
        limited.onRows("poi", Collections.singletonList(
                row(0, point(1, 1), "gid", 1, "name", "a", "sys", "s", "code", "c", "other", "o")));

        List<String> limitedFields = new ArrayList<>();
        for (AttributeStat attribute : onlyLayer(limited.toTileStats()).getAttributes()) {
            limitedFields.add(attribute.getAttribute());
        }
        assertTrue(limitedFields.contains("name"));
        assertTrue("系统字段始终保留", limitedFields.contains("sys"));
        assertTrue("id 字段无条件保留", limitedFields.contains("code"));
        assertFalse(limitedFields.contains("other"));
    }

    @Test
    public void dedupsLargeSequences() {
        V4TileStats stats = new V4TileStats(Collections.singletonList(layer()));
        stats.onRows("poi", Collections.singletonList(row(5_000_000L, point(1, 1), "gid", 1)));
        stats.onRows("poi", Collections.singletonList(row(5_000_000L, point(1, 1), "gid", 1)));
        assertEquals(1L, onlyLayer(stats.toTileStats()).getCount());
    }
}
