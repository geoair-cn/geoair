package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import cn.geoair.base.percent.GiProgressReporter;
import cn.hutool.core.io.unit.DataSizeUtil;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.spark.scheduler.SparkListener;
import org.apache.spark.scheduler.SparkListenerStageCompleted;
import org.apache.spark.scheduler.SparkListenerStageSubmitted;
import org.apache.spark.scheduler.SparkListenerTaskEnd;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.util.LongAccumulator;

/** V3 多图层切片专用的 Spark 执行进度跟踪器。 */
final class V3ProgressTracker implements Serializable {

    private static final long serialVersionUID = 1L;
    private final LongAccumulator tilesWritten;
    private final LongAccumulator batchesWritten;
    private final LongAccumulator bytesWritten;
    private final LongAccumulator featuresRead;
    private final long startTime = System.currentTimeMillis();
    private final int totalStages;
    private final AtomicInteger currentStageIndex = new AtomicInteger();
    private final Map<Integer, String> stageNames = new ConcurrentHashMap<>();
    private final V3ProgressSparkListener listener;

    private V3ProgressTracker(SparkSession sparkSession, int totalStages, GiProgressReporter reporter) {
        this.totalStages = totalStages;
        this.tilesWritten = sparkSession.sparkContext().longAccumulator("v3TilesWritten");
        this.batchesWritten = sparkSession.sparkContext().longAccumulator("v3BatchesWritten");
        this.bytesWritten = sparkSession.sparkContext().longAccumulator("v3BytesWritten");
        this.featuresRead = sparkSession.sparkContext().longAccumulator("v3FeaturesRead");
        this.listener = new V3ProgressSparkListener(reporter);
        sparkSession.sparkContext().addSparkListener(listener);
    }

    static V3ProgressTracker init(SparkSession sparkSession, int totalStages, GiProgressReporter reporter) {
        return new V3ProgressTracker(sparkSession, totalStages, reporter);
    }

    void setStageName(String name) {
        int index = currentStageIndex.get();
        stageNames.put(index, name);
        listener.setCurrentStageName(name);
        System.out.println("[阶段 " + (index + 1) + "/" + totalStages + "] " + name + " 开始...");
    }

    void completeStage(String... extraInfo) {
        int index = currentStageIndex.getAndIncrement();
        StringBuilder message = new StringBuilder(String.format("[阶段 %d/%d] ▓▓▓▓▓▓▓▓▓▓ 100%% | %-10s | 耗时: %s",
                index + 1, totalStages, stageNames.getOrDefault(index, "阶段" + (index + 1)), duration(System.currentTimeMillis() - startTime)));
        for (String info : extraInfo) {
            message.append(" | ").append(info);
        }
        if (tilesWritten.value() > 0) message.append(" | 瓦片: ").append(String.format("%,d", tilesWritten.value()));
        if (batchesWritten.value() > 0) message.append(" | 批次: ").append(String.format("%,d", batchesWritten.value()));
        if (bytesWritten.value() > 0) message.append(" | 数据量: ").append(DataSizeUtil.format(bytesWritten.value()));
        System.out.println(message);
    }

    void printSummary() {
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  V3 多图层切片任务完成汇总");
        if (featuresRead.value() > 0) System.out.printf("  要素总数:   %,d%n", featuresRead.value());
        if (tilesWritten.value() > 0) System.out.printf("  写入瓦片:   %,d%n", tilesWritten.value());
        if (batchesWritten.value() > 0) System.out.printf("  批次总数:   %,d%n", batchesWritten.value());
        if (bytesWritten.value() > 0) System.out.printf("  数据总量:   %s%n", DataSizeUtil.format(bytesWritten.value()));
        System.out.printf("  总耗时:     %s%n", duration(elapsed));
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    LongAccumulator getTilesWritten() { return tilesWritten; }
    LongAccumulator getBatchesWritten() { return batchesWritten; }
    LongAccumulator getBytesWritten() { return bytesWritten; }
    LongAccumulator getFeaturesRead() { return featuresRead; }

    private static String duration(long millis) {
        long seconds = millis / 1000;
        return seconds / 60 > 0 ? String.format("%dm %ds", seconds / 60, seconds % 60) : String.format("%ds", seconds);
    }

    /** 仅在 Driver 端监听 V3 Spark Stage，向调用方传递阶段进度。 */
    private static final class V3ProgressSparkListener extends SparkListener implements Serializable {
        private static final long serialVersionUID = 1L;
        private final GiProgressReporter reporter;
        private final Map<Integer, AtomicInteger> completed = new ConcurrentHashMap<>();
        private final Map<Integer, Integer> totals = new ConcurrentHashMap<>();
        private volatile String currentStageName = "";

        private V3ProgressSparkListener(GiProgressReporter reporter) { this.reporter = reporter; }
        private void setCurrentStageName(String currentStageName) { this.currentStageName = currentStageName; }

        @Override
        public void onStageSubmitted(SparkListenerStageSubmitted event) {
            int stageId = event.stageInfo().stageId();
            totals.put(stageId, event.stageInfo().numTasks());
            completed.put(stageId, new AtomicInteger());
        }

        @Override
        public void onTaskEnd(SparkListenerTaskEnd event) {
            AtomicInteger done = completed.get(event.stageId());
            Integer total = totals.get(event.stageId());
            if (done != null && total != null && reporter != null) reporter.report((long) total, (long) done.incrementAndGet());
        }

        @Override
        public void onStageCompleted(SparkListenerStageCompleted event) {
            int stageId = event.stageInfo().stageId();
            Integer total = totals.get(stageId);
            AtomicInteger done = completed.get(stageId);
            if (total != null && done != null) System.out.printf("[Stage %d] 完成 | %s | 任务: %d/%d%n", stageId, currentStageName, done.get(), total);
        }
    }
}
