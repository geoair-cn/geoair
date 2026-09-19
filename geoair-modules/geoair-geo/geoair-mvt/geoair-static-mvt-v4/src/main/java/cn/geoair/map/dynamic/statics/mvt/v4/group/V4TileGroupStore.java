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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeSet;

/**
 * V4 的按瓦片聚合容器：内存缓冲 + 溢写落盘 + 归并。
 *
 * <p>这一层是 V4 用来替代 Spark {@code reduceByKey} 的部分，思路是经典的外部排序聚合：</p>
 * <ol>
 *   <li>要素按瓦片键直接追加进内存缓冲（<b>不做逐条 merge</b>，避免 V3 里
 *       {@code reduceByKey} 那种"每来一条就复制一遍列表"的平方级开销）；</li>
 *   <li>缓冲行数超过阈值时，把缓冲按瓦片键排序落成一个 run 文件，然后清空内存；</li>
 *   <li>全部要素读完后，若没有 run 文件就直接逐瓦片回调；否则把剩余缓冲也落成一个 run，
 *       再对所有 run 做 k 路归并，同一个瓦片键的记录合并后回调一次。</li>
 * </ol>
 *
 * <p><b>聚合安全阀</b>：回调前会用 {@link V3TileFeatureGroup#merge} 施加与 V3 同一份实现的
 * 要素上限削减，因此"每个瓦片最终留下哪些要素"与 V3 的收敛结果一致（见开发计划里的说明）。</p>
 *
 * <p><b>排序</b>：安全阀是按身份哈希排名取前 N 个，会打乱顺序，所以
 * {@code --preserve-input-order} / {@code --reorder} / {@code --hilbert} 的排序
 * 放在安全阀<b>之后</b>，否则用户要的顺序会被安全阀冲掉。</p>
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
        for (GirAdvOneRow row : rows) {
            target.add(new V4OrderedRow(sequence, row));
        }
        bufferedRows += rows.size();
        totalRows += rows.size();
        capTileIfNeeded(byLayer);
    }

    /**
     * 单个瓦片的要素超过「图层安全上限 × 2」时就地施加一次安全阀削减。
     *
     * <p>这一步不能省。只在编码前削的话，低层级瓦片会在内存里堆到几百万行 ——
     * 实测 poi z6-9 在 4GB 堆下归并阶段直接 OOM。提前削之后每个瓦片始终不超过
     * 2N 行，内存与溢写量都跟着降下来。</p>
     *
     * <p>削减用的是 {@link V3TileFeatureGroup#merge}（V3 自己的实现），
     * 因此"留下哪些要素"与 V3 一致：V3 在 {@code reduceByKey} 里反复削，每次都是
     * "取身份哈希最小的 N 个"，反复施加的收敛结果与只削一次相同。</p>
     */
    private void capTileIfNeeded(Map<String, List<V4OrderedRow>> byLayer) {
        boolean overLimit = false;
        for (Map.Entry<String, List<V4OrderedRow>> entry : byLayer.entrySet()) {
            MvtLayerSliceParameter layer = layersByName.get(entry.getKey());
            if (layer == null || layer.getHardFeatureLimit() == null || layer.getHardFeatureLimit() <= 0) {
                continue;
            }
            if (entry.getValue().size() > layer.getHardFeatureLimit() * 2L) {
                overLimit = true;
                break;
            }
        }
        if (!overLimit) {
            return;
        }
        // 削减会按身份哈希排名重排，序号得先按对象引用记录下来，削完再挂回去，
        // 否则 --preserve-input-order 会失去依据
        IdentityHashMap<GirAdvOneRow, Long> sequenceByRow = new IdentityHashMap<>();
        for (List<V4OrderedRow> rows : byLayer.values()) {
            for (V4OrderedRow ordered : rows) {
                sequenceByRow.put(ordered.getRow(), ordered.getSequence());
            }
        }
        V3TileFeatureGroup capped = groupOf(byLayer).merge(null, layersByName, outGridSrid);
        Map<String, List<GirAdvOneRow>> survivors = capped.copyFeaturesByLayer();
        long before = 0L;
        long after = 0L;
        for (Map.Entry<String, List<V4OrderedRow>> entry : byLayer.entrySet()) {
            before += entry.getValue().size();
            List<GirAdvOneRow> kept = survivors.get(entry.getKey());
            if (kept == null) {
                entry.setValue(new ArrayList<>());
                continue;
            }
            List<V4OrderedRow> restored = new ArrayList<>(kept.size());
            for (GirAdvOneRow row : kept) {
                Long sequence = sequenceByRow.get(row);
                restored.add(new V4OrderedRow(sequence == null ? 0L : sequence, row));
            }
            entry.setValue(restored);
            after += restored.size();
        }
        bufferedRows -= (before - after);
        capEvents++;
        LOG.debug("V4 瓦片安全阀：{} 行 -> {} 行（第 {} 次）", before, after, capEvents);
    }

    /** 把当前瓦片的图层要素拼成一个聚合单元（不施加削减）。 */
    private V3TileFeatureGroup groupOf(Map<String, List<V4OrderedRow>> byLayer) {
        V3TileFeatureGroup group = null;
        for (Map.Entry<String, List<V4OrderedRow>> entry : byLayer.entrySet()) {
            V3TileFeatureGroup single = V3TileFeatureGroup.single(
                    entry.getKey(), V4FeatureOrdering.toRows(entry.getValue()));
            group = group == null ? single : group.merge(single, layersByName, outGridSrid);
        }
        return group;
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

    /** 对全部 run 做 k 路归并，同一个瓦片键的记录合并后回调一次。 */
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
                Map<String, List<V4OrderedRow>> merged = new LinkedHashMap<>();
                while (!queue.isEmpty() && tileKey.equals(queue.peek().currentKey())) {
                    RunReader reader = queue.poll();
                    mergeInto(merged, reader.currentValue());
                    if (reader.advance()) {
                        queue.add(reader);
                    }
                }
                emit(tileKey, merged, consumer);
            }
        } finally {
            for (RunReader reader : readers) {
                reader.closeQuietly();
            }
        }
    }

    private static void mergeInto(
            Map<String, List<V4OrderedRow>> target, Map<String, List<V4OrderedRow>> source) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, List<V4OrderedRow>> entry : source.entrySet()) {
            target.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).addAll(entry.getValue());
        }
    }

    // ------------------------------------------------------------------
    // 回调：安全阀 + 排序
    // ------------------------------------------------------------------

    private void emit(String tileKey, Map<String, List<V4OrderedRow>> byLayer,
            V4TileConsumer consumer) throws Exception {
        if (byLayer == null || byLayer.isEmpty()) {
            return;
        }
        Envelope envelope = envelopeOf(tileKey);
        // 序号不能写进行对象（会被当属性输出），所以先按引用建立"行 -> 序号"的映射，
        // 等安全阀削完、顺序被打乱之后，仍能按原输入顺序把存活的行排回来
        Map<GirAdvOneRow, Long> sequenceByRow = keepInputOrder || hilbert || reorder
                ? new IdentityHashMap<>() : null;
        if (sequenceByRow != null) {
            for (List<V4OrderedRow> rows : byLayer.values()) {
                for (V4OrderedRow ordered : rows) {
                    sequenceByRow.put(ordered.getRow(), ordered.getSequence());
                }
            }
        }

        V3TileFeatureGroup capped = capByLayerSafety(byLayer);
        if (capped == null) {
            return;
        }
        Map<String, List<GirAdvOneRow>> features = capped.copyFeaturesByLayer();
        if (hilbert || reorder) {
            for (Map.Entry<String, List<GirAdvOneRow>> entry : features.entrySet()) {
                entry.setValue(sortSpatially(entry.getValue(), entry.getKey(), envelope));
            }
        } else if (keepInputOrder) {
            for (Map.Entry<String, List<GirAdvOneRow>> entry : features.entrySet()) {
                entry.setValue(sortByInputOrder(entry.getValue(), sequenceByRow));
            }
        }
        consumer.accept(tileKey, rebuild(features));
    }

    /** 用 V3 自己的 merge 施加聚合安全阀：先合并成一个组，再触发一次上限削减。 */
    private V3TileFeatureGroup capByLayerSafety(Map<String, List<V4OrderedRow>> byLayer) {
        V3TileFeatureGroup group = null;
        for (Map.Entry<String, List<V4OrderedRow>> entry : byLayer.entrySet()) {
            List<GirAdvOneRow> rows = V4FeatureOrdering.toRows(entry.getValue());
            V3TileFeatureGroup single = V3TileFeatureGroup.single(entry.getKey(), rows);
            group = group == null ? single : group.merge(single, layersByName, outGridSrid);
        }
        if (group == null) {
            return null;
        }
        // 单图层时上面的循环不会走 merge，这里补一次以保证安全阀确实执行；
        // merge 对不超限的图层原样返回，因此对已削过的列表是幂等的
        return group.merge(null, layersByName, outGridSrid);
    }

    /** 用排好序的图层要素重建聚合单元（仍需走一次 merge 以保持与 V3 相同的构造路径）。 */
    private V3TileFeatureGroup rebuild(Map<String, List<GirAdvOneRow>> features) {
        V3TileFeatureGroup group = null;
        for (Map.Entry<String, List<GirAdvOneRow>> entry : features.entrySet()) {
            V3TileFeatureGroup single = V3TileFeatureGroup.single(entry.getKey(), entry.getValue());
            group = group == null ? single : group.merge(single, layersByName, outGridSrid);
        }
        return group == null ? null : group.merge(null, layersByName, outGridSrid);
    }

    private List<GirAdvOneRow> sortSpatially(List<GirAdvOneRow> rows, String layerName, Envelope envelope) {
        if (rows.size() < 2) {
            return rows;
        }
        MvtLayerSliceParameter layer = layersByName.get(layerName);
        if (layer == null) {
            return rows;
        }
        List<V4OrderedRow> ordered = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            ordered.add(new V4OrderedRow(i, rows.get(i)));
        }
        V4FeatureOrdering.sort(ordered, hilbert, reorder, false, layer, envelope);
        return V4FeatureOrdering.toRows(ordered);
    }

    private List<GirAdvOneRow> sortByInputOrder(List<GirAdvOneRow> rows, Map<GirAdvOneRow, Long> sequenceByRow) {
        if (rows.size() < 2 || sequenceByRow == null) {
            return rows;
        }
        List<GirAdvOneRow> copy = new ArrayList<>(rows);
        // 稳定排序 + 序号查不到时给最大值，保证顺序确定
        copy.sort(Comparator.comparingLong(
                row -> sequenceByRow.getOrDefault(row, Long.MAX_VALUE)));
        return copy;
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
