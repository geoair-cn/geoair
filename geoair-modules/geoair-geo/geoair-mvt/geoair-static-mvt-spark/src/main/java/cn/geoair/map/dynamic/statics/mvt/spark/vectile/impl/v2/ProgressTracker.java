package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v2;

import cn.hutool.core.io.unit.DataSizeUtil;
import org.apache.spark.scheduler.SparkListener;
import org.apache.spark.scheduler.SparkListenerStageCompleted;
import org.apache.spark.scheduler.SparkListenerStageSubmitted;
import org.apache.spark.scheduler.SparkListenerTaskEnd;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.util.LongAccumulator;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于 SparkListener 的分布式进度跟踪器。
 * <p>
 * 通过 {@link ProgressSparkListener} 在 driver 侧捕获 Stage/Task 事件，
 * 结合 {@link LongAccumulator} 汇总 executor 侧的瓦片写入数据。
 *
 * @author generated
 */
public class ProgressTracker implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int BAR_WIDTH = 20;

    // ===================== 累加器（Executor 写入，Driver 读取）=====================
    private LongAccumulator tilesWritten;
    private LongAccumulator batchesWritten;
    private LongAccumulator bytesWritten;
    private LongAccumulator featuresRead;

    // ===================== Driver 侧状态 =====================
    private final long startTime;
    private final int totalStages;
    private final AtomicInteger currentStageIndex = new AtomicInteger(0);
    private final Map<Integer, String> stageNames = new ConcurrentHashMap<>();

    // ===================== SparkListener（transient，仅 driver 侧使用，不参与闭包序列化）=====================
    private   ProgressSparkListener listener;

    private ProgressTracker(SparkSession sparkSession, int totalStages) {
        this.startTime = System.currentTimeMillis();
        this.totalStages = totalStages;

        this.tilesWritten = sparkSession.sparkContext().longAccumulator("tilesWritten");
        this.batchesWritten = sparkSession.sparkContext().longAccumulator("batchesWritten");
        this.bytesWritten = sparkSession.sparkContext().longAccumulator("bytesWritten");
        this.featuresRead = sparkSession.sparkContext().longAccumulator("featuresRead");

        this.listener = new ProgressSparkListener();
        sparkSession.sparkContext().addSparkListener(listener);
    }

    public static ProgressTracker init(SparkSession sparkSession, int totalStages) {
        return new ProgressTracker(sparkSession, totalStages);
    }

    // ===================== Driver 侧 API =====================

    public void setStageName(String name) {
        int idx = currentStageIndex.get();
        stageNames.put(idx, name);
        listener.setCurrentStageName(name);
        System.out.println("[阶段 " + (idx + 1) + "/" + totalStages + "] " + name + " 开始...");
    }

    public void completeStage(String... extraInfo) {
        int idx = currentStageIndex.get();
        String name = stageNames.getOrDefault(idx, "阶段" + (idx + 1));
        long elapsed = System.currentTimeMillis() - startTime;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[阶段 %d/%d] ▓▓▓▓▓▓▓▓▓▓ 100%% | %-10s | 耗时: %s",
                idx + 1, totalStages, name, formatDuration(elapsed)));

        for (String info : extraInfo) {
            sb.append(" | ").append(info);
        }

        long tiles = tilesWritten.value();
        long batches = batchesWritten.value();
        long bytes = bytesWritten.value();
        if (tiles > 0) sb.append(" | 瓦片: ").append(String.format("%,d", tiles));
        if (batches > 0) sb.append(" | 批次: ").append(String.format("%,d", batches));
        if (bytes > 0) sb.append(" | 数据量: ").append(DataSizeUtil.format(bytes));

        System.out.println(sb);
        currentStageIndex.incrementAndGet();
    }

    public void printSummary() {
        long totalElapsed = System.currentTimeMillis() - startTime;
        long tiles = tilesWritten.value();
        long batches = batchesWritten.value();
        long bytes = bytesWritten.value();
        long features = featuresRead.value();

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  切片任务完成汇总");
        System.out.println("═══════════════════════════════════════════════════════════════");
        if (features > 0) System.out.printf("  要素总数:   %,d%n", features);
        if (tiles > 0) System.out.printf("  写入瓦片:   %,d%n", tiles);
        if (batches > 0) System.out.printf("  批次总数:   %,d%n", batches);
        if (bytes > 0) System.out.printf("  数据总量:   %s%n", DataSizeUtil.format(bytes));
        System.out.printf("  总耗时:     %s%n", formatDuration(totalElapsed));
        if (tiles > 0 && totalElapsed > 0) {
            System.out.printf("  平均速度:   %.0f 瓦片/秒%n", tiles * 1000.0 / totalElapsed);
        }
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    // ===================== Getters =====================

    public LongAccumulator getTilesWritten() {
        return tilesWritten;
    }

    public LongAccumulator getBatchesWritten() {
        return batchesWritten;
    }

    public LongAccumulator getBytesWritten() {
        return bytesWritten;
    }

    public LongAccumulator getFeaturesRead() {
        return featuresRead;
    }

    // ===================== 工具方法 =====================

    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes > 0 ? String.format("%dm %ds", minutes, seconds) : String.format("%ds", seconds);
    }

    private static String renderBar(double progress) {
        int filled = Math.max(0, Math.min(BAR_WIDTH, (int) (progress * BAR_WIDTH)));
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filled; i++) bar.append('▓');
        for (int i = 0; i < BAR_WIDTH - filled; i++) bar.append('░');
        return bar.toString();
    }

    // ===================== SparkListener =====================

    public static class ProgressSparkListener extends SparkListener implements   Serializable {

        private final Map<Integer, StageTracker> stageTrackers = new ConcurrentHashMap<>();
        private final AtomicInteger totalTasksCompleted = new AtomicInteger(0);
        private String currentStageName = "";
        private int printInterval = 50;

        void setCurrentStageName(String name) {
            this.currentStageName = name;
        }

        @Override
        public void onStageSubmitted(SparkListenerStageSubmitted stageSubmitted) {
            int stageId = stageSubmitted.stageInfo().stageId();
            int numTasks = stageSubmitted.stageInfo().numTasks();
            String desc = stageSubmitted.stageInfo().name();
            stageTrackers.put(stageId, new StageTracker(stageId, numTasks, desc, currentStageName));
        }

        @Override
        public void onStageCompleted(SparkListenerStageCompleted stageCompleted) {
            int stageId = stageCompleted.stageInfo().stageId();
            StageTracker tracker = stageTrackers.get(stageId);
            if (tracker == null) return;
            long elapsed = System.currentTimeMillis() - tracker.startTime;
            System.out.printf("[Stage %d] ▓▓▓▓▓▓▓▓▓▓ 100%% | %-10s | 任务: %d/%d | 耗时: %s%n",
                    stageId,
                    tracker.customName.isEmpty() ? "Stage-" + stageId : tracker.customName,
                    tracker.completedTasks.get(), tracker.totalTasks,
                    formatDuration(elapsed));
        }

        @Override
        public void onTaskEnd(SparkListenerTaskEnd taskEnd) {
            if (taskEnd.stageId() < 0) return;
            totalTasksCompleted.incrementAndGet();
            StageTracker tracker = stageTrackers.get(taskEnd.stageId());
            if (tracker == null) return;
            int completed = tracker.completedTasks.incrementAndGet();
            if (completed % printInterval == 0 && completed < tracker.totalTasks) {
                double progress = (double) completed / tracker.totalTasks;
                long elapsed = System.currentTimeMillis() - tracker.startTime;
                System.out.printf("[Stage %d] %s %3d%% | %-10s | 任务: %d/%d | 耗时: %s%n",
                        taskEnd.stageId(),
                        renderBar(progress),
                        (int) (progress * 100),
                        tracker.customName.isEmpty() ? "Stage-" + taskEnd.stageId() : tracker.customName,
                        completed, tracker.totalTasks,
                        formatDuration(elapsed));
            }
        }
    }

    private static class StageTracker {
        final int stageId;
        final int totalTasks;
        final String description;
        final String customName;
        final AtomicInteger completedTasks = new AtomicInteger(0);
        final long startTime;

        StageTracker(int stageId, int totalTasks, String description, String customName) {
            this.stageId = stageId;
            this.totalTasks = totalTasks;
            this.description = description;
            this.customName = customName;
            this.startTime = System.currentTimeMillis();
        }
    }
}
