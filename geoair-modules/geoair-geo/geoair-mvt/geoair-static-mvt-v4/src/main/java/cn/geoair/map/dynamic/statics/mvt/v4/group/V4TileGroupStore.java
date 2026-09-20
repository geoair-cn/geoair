package cn.geoair.map.dynamic.statics.mvt.v4.group;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.MvtLayerSliceParameter;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.V3TileFeatureGroup;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.utils.TileUtils;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import org.locationtech.jts.geom.Envelope;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeSet;

/**
 * V4 的按瓦片聚合容器：内存缓冲 + 溢写落盘 + 有界归并。
 *
 * <p>这一层是 V4 用来替代 Spark {@code reduceByKey} 的部分，思路是外部排序聚合：</p>
 * <ol>
 *   <li>要素按瓦片键追加进内存缓冲（<b>不做逐条 merge</b>，避免 V3 里
 *       {@code reduceByKey} 那种"每来一条就复制一遍列表"的开销）；</li>
 *   <li>缓冲行数超过阈值时，把缓冲按瓦片键排序落成一个 run 文件，然后清空内存；</li>
 *   <li>全部要素读完后，若没有 run 文件就直接逐瓦片回调；否则把剩余缓冲也落成一个 run，
 *       再对所有 run 做 k 路归并，同一个瓦片键的记录合并后回调一次。</li>
 * </ol>
 *
 * <p><b>保留哪些行：全局确定性，与溢写无关。</b>每个图层的行不超过 {@code hardFeatureLimit}（记作 N），
 * 选取规则见 {@link V4IdentitySelector} —— 按 (身份排名, 输入序号, 批内下标) 取最小的 N 个。
 * 该算子可结合、可交换、幂等，因此三处施加（累积时、归并时、回调前）结果完全一致，
 * 溢写、run 数、到达顺序都不影响产物。这是 V4 与 V3 的关键差别：V3 的削减次数决定了保留量，
 * 同一份数据在不同运行时配置下能差 18%（见 {@code V4开发计划.md} 7.8）。</p>
 *
 * <p><b>内存取向：宁可多算，不要多存。</b></p>
 * <ul>
 *   <li>缓冲里每个瓦片每个图层 ≤ 2N 行，超了立刻用有界选择削到 N；</li>
 *   <li>归并时同一个瓦片键的多个 run 记录<b>流式喂进容量 N 的堆</b>，不再"先 addAll 成并集再削"。
 *       实测旧写法在 62 个 run 时会把堆撑到 4 GB 上限；有界之后归并的内存与 run 数无关，
 *       多出来的只是 CPU（每行 O(log N) 比较）；</li>
 *   <li>回调前排序与构建聚合单元只作用在 ≤ N 行上。</li>
 * </ul>
 *
 * <p><b>排序的位置</b>：{@code --preserve-input-order} / {@code --reorder} / {@code --hilbert}
 * 放在削减<b>之后</b>（顺序号由 {@link V4OrderedRow} 带着走，不需要额外的映射表），
 * 否则用户要的顺序会被削减打乱。</p>
 *
 * @author 张逢吉
 */
public final class V4TileGroupStore implements Closeable {

    private static final GiLogger LOG = GirLoggerFactory.getLogger();

    private final Map<String, MvtLayerSliceParameter> layersByName;
    private final int outGridSrid;
    private final int spillRowThreshold;
    private final Path spillDir;
    private final boolean ownsSpillDir;

    private final boolean hilbert;
    private final boolean reorder;
    private final boolean keepInputOrder;

    /** 瓦片键 -> 图层名 -> 要素（内存缓冲）。 */
    private final Map<String, Map<String, List<V4OrderedRow>>> buffer = new HashMap<>();

    private final List<Path> runs = new ArrayList<>();

    private long bufferedRows;
    private long totalRows;
    private long spillBytes;
    private int runSeq;
    private long capEvents;
    private boolean finished;

    public V4TileGroupStore(List<MvtLayerSliceParameter> layers, int outGridSrid,
            int spillRowThreshold, String spillDirectory,
            boolean hilbert, boolean reorder, boolean keepInputOrder) throws IOException {
        this.layersByName = new LinkedHashMap<>();
        for (MvtLayerSliceParameter layer : layers) {
            this.layersByName.put(layer.getLayerName(), layer);
        }
        this.outGridSrid = outGridSrid;
        this.spillRowThreshold = Math.max(1000, spillRowThreshold);
        this.hilbert = hilbert;
        this.reorder = reorder;
        this.keepInputOrder = keepInputOrder;
        if (spillDirectory == null || spillDirectory.trim().isEmpty()) {
            // 用 java.io.tmpdir 下的固定父目录再建临时子目录：直接 createTempDirectory() 依赖
            // java.io.tmpdir 已存在，而生产环境里它未必存在（实测 NoSuchFileException）
            Path parent = Paths.get(System.getProperty("java.io.tmpdir", ".")).resolve("v4-spill");
            Files.createDirectories(parent);
            this.spillDir = Files.createTempDirectory(parent, "run-");
            this.ownsSpillDir = true;
        } else {
            this.spillDir = Paths.get(spillDirectory);
            Files.createDirectories(this.spillDir);
            this.ownsSpillDir = false;
        }
    }

    /**
     * 追加一个要素在某个瓦片上的落点。
     *
     * @param tileKey   Bing QuadKey
     * @param layerName MVT 内部图层名
     * @param rows     该要素落在这块瓦片上的行（一个要素可能被切成多行）
     * @param sequence 输入顺序号
     */
    public void add(String tileKey, String layerName, List<GirAdvOneRow> rows, long sequence) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<String, List<V4OrderedRow>> byLayer = buffer
                .computeIfAbsent(tileKey, key -> new LinkedHashMap<>());
        List<V4OrderedRow> target = byLayer.computeIfAbsent(layerName, key -> new ArrayList<>());
        int rowIndex = 0;
        for (GirAdvOneRow row : rows) {
            target.add(new V4OrderedRow(sequence, rowIndex++, row));
        }
        bufferedRows += rows.size();
        totalRows += rows.size();
        trimTileIfNeeded(layerName, byLayer);
    }

    /**
     * 单个瓦片某个图层的行数超过 「N × 2」时就地削到 N。
     *
     * <p>这一步不能省：只在回调前削的话，低层级瓦片会在内存里堆到几百万行。</p>
     *
     * <p>触发线取 2N 而不是 N，是为了摊薄削减本身的开销 —— 削一次要按优先序重排一遍，
     * 每来一行就削一次会把 O(n log N) 变成 O(n² log N)。这与 V3 的 2N 触发线相同，
     * 但因为 V3 的最终保留量取决于触发次数、V4 的最终保留量由<b>回调前那一次</b>统一决定，
     * 所以这里削到哪一步都不影响产物。</p>
     */
    private void trimTileIfNeeded(String layerName, Map<String, List<V4OrderedRow>> byLayer) {
        MvtLayerSliceParameter layer = layersByName.get(layerName);
        int limit = hardLimitOf(layer);
        if (limit <= 0) {
            return;
        }
        List<V4OrderedRow> rows = byLayer.get(layerName);
        if (rows == null || rows.size() <= limit * 2L) {
            return;
        }
        List<V4OrderedRow> kept = V4IdentitySelector.keepSmallest(rows, limit, layer);
        byLayer.put(layerName, kept);
        bufferedRows -= (rows.size() - kept.size());
        capEvents++;
        LOG.debug("V4 瓦片预削：{} -> {} 行（第 {} 次）", rows.size(), kept.size(), capEvents);
    }

    /** 缓冲超过阈值时溢写；返回是否真的溢写了一次。 */
    public boolean spillIfNeeded() throws IOException {
        if (bufferedRows < spillRowThreshold) {
            return false;
        }
        spill();
        return true;
    }

    /**
     * 结束写入并逐瓦片回调。
     *
     * @return 本次聚合的统计
     */
    public V4GroupStats finish(V4TileConsumer consumer) throws Exception {
        if (finished) {
            throw new IllegalStateException("V4TileGroupStore 已经结束过，不可重复调用");
        }
        finished = true;
        if (runs.isEmpty()) {
            for (String tileKey : new TreeSet<>(buffer.keySet())) {
                emit(tileKey, buffer.get(tileKey), consumer);
            }
            buffer.clear();
            return new V4GroupStats(totalRows, 0, spillBytes, capEvents);
        }
        spill();
        int mergedRuns = runs.size();
        mergeRuns(consumer);
        return new V4GroupStats(totalRows, mergedRuns, spillBytes, capEvents);
    }

    // ------------------------------------------------------------------
    // 溢写与归并
    // ------------------------------------------------------------------

    /** 把内存缓冲按瓦片键排序落成一个 run 文件，然后清空缓冲。 */
    private void spill() throws IOException {
        Path run = spillDir.resolve(String.format("run-%05d.bin", runSeq++));
        long bytes = 0L;
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(run)))) {
            for (String tileKey : new TreeSet<>(buffer.keySet())) {
                // writeUnshared + reset：避免序列化器为"已写过的对象"建引用表，
                // 否则百万级行的 run 文件会让堆在写出阶段持续增长
                out.writeUnshared(new Object[]{tileKey, buffer.get(tileKey)});
                out.reset();
            }
            out.flush();
            bytes = Files.size(run);
        }
        runs.add(run);
        spillBytes += bytes;
        LOG.info("V4 溢写完成：{} 行 -> {}（{} 字节）", bufferedRows, run.getFileName(), bytes);
        buffer.clear();
        bufferedRows = 0L;
    }

    /**
     * 对全部 run 做 k 路归并，同一个瓦片键的记录合并后回调一次。
     *
     * <p>合并是<b>有界</b>的：同一瓦片键的记录不落成并集，而是逐条喂进容量 N 的选择器
     * （{@link MergeAccumulator}），因此归并阶段的内存与 run 数无关。</p>
     */
    private void mergeRuns(V4TileConsumer consumer) throws Exception {
        List<RunReader> readers = new ArrayList<>(runs.size());
        try {
            for (Path run : runs) {
                readers.add(new RunReader(run));
            }
            PriorityQueue<RunReader> queue = new PriorityQueue<>(
                    Comparator.comparing(RunReader::currentKey));
            for (RunReader reader : readers) {
                if (reader.currentKey() != null) {
                    queue.add(reader);
                }
            }
            while (!queue.isEmpty()) {
                String tileKey = queue.peek().currentKey();
                MergeAccumulator accumulator = new MergeAccumulator(layersByName);
                while (!queue.isEmpty() && tileKey.equals(queue.peek().currentKey())) {
                    RunReader reader = queue.poll();
                    accumulator.accept(reader.currentValue());
                    if (reader.advance()) {
                        queue.add(reader);
                    }
                }
                emit(tileKey, accumulator.toMap(), consumer);
            }
        } finally {
            for (RunReader reader : readers) {
                reader.closeQuietly();
            }
        }
    }

    /**
     * 归并累加器：配置了安全上限的图层走容量 N 的有界选择，没配置的按原样累加。
     *
     * <p>没配置上限（{@code hardFeatureLimit} 为空/≤0）的图层与 V3 行为一致 —— 不削。
     * 这时缓冲与归并的内存不受约束，低层级任务需要用户自己把上限配上。</p>
     */
    private static final class MergeAccumulator {

        private final Map<String, MvtLayerSliceParameter> layersByName;
        private final Map<String, List<V4OrderedRow>> unbounded = new LinkedHashMap<>();
        private final Map<String, V4IdentitySelector.Bounded> bounded = new LinkedHashMap<>();

        private MergeAccumulator(Map<String, MvtLayerSliceParameter> layersByName) {
            this.layersByName = layersByName;
        }

        private void accept(Map<String, List<V4OrderedRow>> source) {
            if (source == null) {
                return;
            }
            for (Map.Entry<String, List<V4OrderedRow>> entry : source.entrySet()) {
                String layerName = entry.getKey();
                MvtLayerSliceParameter layer = layersByName.get(layerName);
                int limit = hardLimitOf(layer);
                if (limit <= 0) {
                    unbounded.computeIfAbsent(layerName, key -> new ArrayList<>()).addAll(entry.getValue());
                    continue;
                }
                bounded.computeIfAbsent(layerName, key -> new V4IdentitySelector.Bounded(limit, layer))
                        .offerAll(entry.getValue());
            }
        }

        private Map<String, List<V4OrderedRow>> toMap() {
            Map<String, List<V4OrderedRow>> result = new LinkedHashMap<>(unbounded);
            for (Map.Entry<String, V4IdentitySelector.Bounded> entry : bounded.entrySet()) {
                result.put(entry.getKey(), entry.getValue().toList());
            }
            return result;
        }
    }

    // ------------------------------------------------------------------
    // 回调：统一削减 + 排序
    // ------------------------------------------------------------------

    /**
     * 回调前的最后一道：把每个图层的行统一削到不超过 N，再按用户指定的顺序排好，最后交给编码器。
     *
     * <p>这一道是"产物口径"的唯一定义处。前面累积与归并阶段的削减只影响内存，
     * 不影响结果 —— 因为"取优先序最小的 N 个"可结合、可交换、幂等。</p>
     */
    private void emit(String tileKey, Map<String, List<V4OrderedRow>> byLayer,
            V4TileConsumer consumer) throws Exception {
        if (byLayer == null || byLayer.isEmpty()) {
            return;
        }
        Envelope envelope = envelopeOf(tileKey);
        Map<String, List<GirAdvOneRow>> features = new LinkedHashMap<>();
        for (Map.Entry<String, List<V4OrderedRow>> entry : byLayer.entrySet()) {
            String layerName = entry.getKey();
            MvtLayerSliceParameter layer = layersByName.get(layerName);
            List<V4OrderedRow> rows = entry.getValue();
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            int limit = hardLimitOf(layer);
            if (limit > 0) {
                rows = V4IdentitySelector.keepSmallest(rows, limit, layer);
            }
            if (layer != null && V4FeatureOrdering.needsSort(hilbert, reorder, keepInputOrder, rows.size())) {
                V4FeatureOrdering.sort(rows, hilbert, reorder, keepInputOrder, layer, envelope);
            }
            features.put(layerName, V4FeatureOrdering.toRows(rows));
        }
        if (features.isEmpty()) {
            return;
        }
        V3TileFeatureGroup group = rebuild(features);
        if (group != null) {
            consumer.accept(tileKey, group);
        }
    }

    /** 用（已削到上限的）图层要素重建聚合单元，交给复用的 V3 编码器。 */
    private V3TileFeatureGroup rebuild(Map<String, List<GirAdvOneRow>> features) {
        V3TileFeatureGroup group = null;
        for (Map.Entry<String, List<GirAdvOneRow>> entry : features.entrySet()) {
            V3TileFeatureGroup single = V3TileFeatureGroup.single(entry.getKey(), entry.getValue());
            group = group == null ? single : group.merge(single, layersByName, outGridSrid);
        }
        return group == null ? null : group.merge(null, layersByName, outGridSrid);
    }

    /** 图层的安全上限 N；未配置时返回 0 表示不削。 */
    private static int hardLimitOf(MvtLayerSliceParameter layer) {
        if (layer == null || layer.getHardFeatureLimit() == null || layer.getHardFeatureLimit() <= 0) {
            return 0;
        }
        return layer.getHardFeatureLimit();
    }

    private Envelope envelopeOf(String tileKey) {
        TileZxyApo zxy = GirGeoTools.defaultInstance().getTileGridBingMapOpt().quadKeyToXyz(tileKey);
        return TileUtils.getTileEnvelope(zxy.getZ(), zxy.getX(), zxy.getY(), outGridSrid);
    }

    @Override
    public void close() throws IOException {
        buffer.clear();
        for (Path run : runs) {
            try {
                Files.deleteIfExists(run);
            } catch (IOException e) {
                LOG.warn("V4 溢写文件删除失败: {} - {}", run, e.getMessage());
            }
        }
        runs.clear();
        if (ownsSpillDir) {
            try {
                Files.deleteIfExists(spillDir);
            } catch (IOException e) {
                LOG.warn("V4 溢写目录删除失败: {} - {}", spillDir, e.getMessage());
            }
        }
    }

    /** 单个 run 文件的顺序读取器。 */
    private static final class RunReader implements Closeable {

        private final Path path;
        private final ObjectInputStream in;
        private String currentKey;
        private Map<String, List<V4OrderedRow>> currentValue;

        @SuppressWarnings("unchecked")
        private RunReader(Path path) throws IOException {
            this.path = path;
            this.in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(path)));
            advance();
        }

        private String currentKey() {
            return currentKey;
        }

        private Map<String, List<V4OrderedRow>> currentValue() {
            return currentValue;
        }

        @SuppressWarnings("unchecked")
        private boolean advance() {
            try {
                // 与写出端的 writeUnshared 对应：不走引用表，内存占用与已读记录数无关
                Object[] record = (Object[]) in.readUnshared();
                currentKey = (String) record[0];
                currentValue = (Map<String, List<V4OrderedRow>>) record[1];
                return true;
            } catch (EOFException e) {
                currentKey = null;
                currentValue = null;
                return false;
            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException("读取 V4 溢写文件失败: " + path, e);
            }
        }

        private void closeQuietly() {
            try {
                in.close();
            } catch (IOException ignored) {
                // 读取已完成，关闭失败不影响结果
            }
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    /** 聚合阶段统计：总行数、run 文件数、溢写字节数。 */
    public static final class V4GroupStats {

        private final long totalRows;
        private final int runFiles;
        private final long spillBytes;
        private final long capEvents;

        V4GroupStats(long totalRows, int runFiles, long spillBytes, long capEvents) {
            this.totalRows = totalRows;
            this.runFiles = runFiles;
            this.spillBytes = spillBytes;
            this.capEvents = capEvents;
        }

        public long getCapEvents() {
            return capEvents;
        }

        public long getTotalRows() {
            return totalRows;
        }

        public int getRunFiles() {
            return runFiles;
        }

        public long getSpillBytes() {
            return spillBytes;
        }

        @Override
        public String toString() {
            return String.format("聚合行数=%d，安全阀次数=%d，溢写run数=%d，溢写字节=%d",
                    totalRows, capEvents, runFiles, spillBytes);
        }
    }
}
