package cn.geoair.map.dynamic.statics.mvt.v4.stats;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.FieldStatUtils;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json.AttributeStat;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json.LayerStat;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.statistics.json.TileStats;
import cn.geoair.map.dynamic.statics.mvt.v4.group.V4EmittedRowListener;
import cn.geoair.map.dynamic.statics.mvt.v4.group.V4OrderedRow;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * V4 的瓦片要素统计（tippecanoe 的 {@code tilestats}）。
 *
 * <p>统计对象是<b>源要素</b>：每个图层的要素数、几何类型，以及每个输出字段的类型、
 * 取值分布与数值范围。产出对象直接复用 V1/V2 写库那套 DTO（{@link TileStats}），
 * 因此 JSON 结构与历史统计完全同构。</p>
 *
 * <p><b>去重怎么做的：不用往行里塞标记。</b></p>
 * <p>V1/V2 必须给每行打一个跨分区的唯一 Id 才能在统计时区分"同一个要素被铺进了多块瓦片"
 * —— 因为它们的统计发生在瓦片铺开之后的 RDD 上。V4 是单机顺序读取，读取时给每行编的
 * 输入序号（{@link V4OrderedRow#getSequence()}）本身就是<b>源要素</b>的编号：
 * 同一个要素的所有瓦片行共用同一个序号。于是去重退化成"序号查重"，
 * 一个要素只占 1 bit，既不用往行对象里写东西（写进去就会变成 PBF 属性），也不用存字符串。</p>
 *
 * <p><b>内存上限：</b>序号位图与要素数成正比（约 1 bit/要素），字段取值表每字段最多
 * {@link #MAX_VALUE_COUNT_PER_FIELD} 项 —— 与 V1/V2 的取值上限同值。统计全程只保留
 * "计数与少量样本值"，不保留任何行。</p>
 *
 * @author 张逢吉
 */
public final class V4TileStats implements V4EmittedRowListener {

    /** 每个字段最多记录这么多个不同取值（与 V1/V2 的 {@code MAX_VALUE_COUNT_PER_FIELD} 一致）。 */
    public static final int MAX_VALUE_COUNT_PER_FIELD = 100;

    /** 按图层配置顺序保存的统计。 */
    private final Map<String, LayerStats> layers = new LinkedHashMap<>();

    public V4TileStats(List<MvtLayerSliceParameter> layerParameters) {
        if (layerParameters == null) {
            return;
        }
        for (MvtLayerSliceParameter layer : layerParameters) {
            if (layer != null && layer.getLayerName() != null) {
                layers.put(layer.getLayerName(), new LayerStats(layer));
            }
        }
    }

    @Override
    public void onRows(String layerName, List<V4OrderedRow> rows) {
        LayerStats stats = layers.get(layerName);
        if (stats == null || rows == null || rows.isEmpty()) {
            return;
        }
        for (V4OrderedRow ordered : rows) {
            // 同一个源要素在多块瓦片里出现多次，只统计第一次见到的那一行
            if (stats.markSeen(ordered.getSequence())) {
                stats.accept(ordered.getRow());
            }
        }
    }

    /** 产出统计主体（JSON 结构与 V1/V2 的 {@code TileStatRoot.tilestats} 相同）。 */
    public TileStats toTileStats() {
        List<LayerStat> layerStats = new ArrayList<>(layers.size());
        for (LayerStats stats : layers.values()) {
            layerStats.add(stats.toLayerStat());
        }
        TileStats result = new TileStats();
        result.setLayerCount(layerStats.size());
        result.setLayers(layerStats);
        return result;
    }

    /** 某图层输出字段的类型表（字段名 -&gt; Number/Boolean/String），供 {@code vector_layers.fields} 使用。 */
    public Map<String, String> fieldTypes(String layerName) {
        LayerStats stats = layers.get(layerName);
        return stats == null ? new LinkedHashMap<>() : stats.fieldTypes();
    }

    /** 一行摘要，供任务日志使用。 */
    public String summary() {
        StringBuilder summary = new StringBuilder();
        for (LayerStats stats : layers.values()) {
            if (summary.length() > 0) {
                summary.append("；");
            }
            summary.append(stats.getLayerName())
                    .append(": 要素=").append(stats.featureCount())
                    .append("，几何=").append(stats.geometryType())
                    .append("，字段=").append(stats.fieldCount());
        }
        return summary.length() == 0 ? "无图层" : summary.toString();
    }

    // ------------------------------------------------------------------
    // 单图层统计
    // ------------------------------------------------------------------

    /** 单个图层的统计。 */
    static final class LayerStats {

        private final MvtLayerSliceParameter layer;
        private final String layerName;
        private final String geomFieldName;
        private final SequenceSet seen = new SequenceSet();
        private final Map<String, FieldStats> fields = new LinkedHashMap<>();
        private String geometryType;

        private LayerStats(MvtLayerSliceParameter layer) {
            this.layer = layer;
            this.layerName = layer.getLayerName();
            this.geomFieldName = layer.getGeomFieldName();
        }

        private String getLayerName() {
            return layerName;
        }

        private long featureCount() {
            return seen.size();
        }

        private int fieldCount() {
            return fields.size();
        }

        private String geometryType() {
            return geometryType == null ? "Unknown" : geometryType;
        }

        /** 返回 true 表示这个序号是第一次出现（该要素需要计入统计）。 */
        private boolean markSeen(long sequence) {
            return seen.add(sequence);
        }

        private void accept(GirAdvOneRow row) {
            if (geometryType == null) {
                Geometry geometry = row.getGeometry(geomFieldName);
                if (geometry != null && !geometry.isEmpty()) {
                    geometryType = geometry.getGeometryType();
                }
            }
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String field = entry.getKey();
                if (isOutputField(field)) {
                    fields.computeIfAbsent(field, FieldStats::new).accept(entry.getValue());
                }
            }
        }

        /**
         * 该字段是否会出现在 PBF 属性里。
         *
         * <p>判据与 {@code MultiLayerMvtEncoderV3.getAttributes} 逐条对齐：几何字段排除、
         * 排除字段优先、{@code includeFields} 为空表示全保留、id 字段强制保留。
         * 统计"实际写出去的东西"，避免报出瓦片里根本没有的字段。</p>
         */
        private boolean isOutputField(String field) {
            if (field == null || field.equals(geomFieldName)) {
                return false;
            }
            List<String> excludeFields = layer.getExcludeFields();
            if (excludeFields != null && excludeFields.contains(field)) {
                return false;
            }
            String idField = layer.getIdFieldName();
            if (idField != null && !idField.trim().isEmpty() && idField.equals(field)) {
                return true;
            }
            List<String> includeFields = layer.getIncludeFields();
            if (includeFields == null || includeFields.isEmpty()) {
                return true;
            }
            return includeFields.contains(field)
                    || (layer.getSysIncludeFields() != null && layer.getSysIncludeFields().contains(field));
        }

        private Map<String, String> fieldTypes() {
            Map<String, String> types = new LinkedHashMap<>();
            for (FieldStats stats : fields.values()) {
                types.put(stats.getName(), stats.getType());
            }
            return types;
        }

        private LayerStat toLayerStat() {
            List<AttributeStat> attributes = new ArrayList<>(fields.size());
            for (FieldStats stats : fields.values()) {
                attributes.add(stats.toAttributeStat());
            }
            LayerStat stat = new LayerStat();
            stat.setLayer(layerName);
            stat.setCount(featureCount());
            stat.setGeometry(geometryType());
            stat.setAttributeCount(attributes.size());
            stat.setAttributes(attributes);
            return stat;
        }
    }

    // ------------------------------------------------------------------
    // 单字段统计
    // ------------------------------------------------------------------

    /** 单个字段的统计。 */
    static final class FieldStats {

        private final String name;
        private final Map<Object, Long> valueCounts = new LinkedHashMap<>();
        private long count;
        private String type;
        private Double min;
        private Double max;

        private FieldStats(String name) {
            this.name = name;
        }

        private String getName() {
            return name;
        }

        private String getType() {
            return type == null ? "String" : type;
        }

        private void accept(Object raw) {
            Object value = raw == null ? "" : raw;
            count++;
            if (type == null) {
                type = FieldStatUtils.getFieldType(value);
            }
            Long existing = valueCounts.get(value);
            if (existing != null) {
                // 已达取值上限时仍然累计已知值的次数，只是不再记新值 ——
                // 这样"出现最多的值"不会因为上限丢样本（V1/V2 是上限一到就整块停止累计）
                valueCounts.put(value, existing + 1L);
            } else if (valueCounts.size() < MAX_VALUE_COUNT_PER_FIELD) {
                valueCounts.put(value, 1L);
            }
            if (value instanceof Number) {
                double number = ((Number) value).doubleValue();
                if (!Double.isNaN(number)) {
                    min = min == null ? number : Math.min(min, number);
                    max = max == null ? number : Math.max(max, number);
                }
            }
        }

        private AttributeStat toAttributeStat() {
            AttributeStat stat = new AttributeStat();
            stat.setAttribute(name);
            stat.setCount(count);
            stat.setType(getType());
            stat.setValues(new ArrayList<>(valueCounts.keySet()));
            stat.setStatics(new ArrayList<>(valueCounts.values()));
            stat.setMin(min);
            stat.setMax(max);
            return stat;
        }
    }

    // ------------------------------------------------------------------
    // 序号去重
    // ------------------------------------------------------------------

    /**
     * 输入序号的去重位图。
     *
     * <p>序号是从 0 起的稠密整数，因此可以直接当位图下标用：容量按需翻倍，
     * 一个要素 1 bit。到 {@link #MAX_FEATURES} 之后不再扩容（也不再去重），
     * 避免异常大的序号把位图撑爆。</p>
     */
    static final class SequenceSet {

        /** 位图容量上限：2^30 个要素（按上限约 128 MB）。 */
        static final long MAX_FEATURES = 1L << 30;

        private static final int MAX_WORDS = (int) (MAX_FEATURES >>> 6);

        private long[] words;
        private long count;

        /** 返回 true 表示这个序号是第一次出现。 */
        private boolean add(long sequence) {
            if (sequence < 0L) {
                return true;
            }
            int word = (int) (sequence >>> 6);
            if (word >= MAX_WORDS) {
                return true;
            }
            if (words == null) {
                words = new long[16];
            }
            if (word >= words.length) {
                int size = words.length;
                while (size <= word && size < MAX_WORDS) {
                    size <<= 1;
                }
                words = Arrays.copyOf(words, Math.min(size, MAX_WORDS));
            }
            long mask = 1L << (sequence & 63L);
            if ((words[word] & mask) != 0L) {
                return false;
            }
            words[word] |= mask;
            count++;
            return true;
        }

        private long size() {
            return count;
        }
    }
}
