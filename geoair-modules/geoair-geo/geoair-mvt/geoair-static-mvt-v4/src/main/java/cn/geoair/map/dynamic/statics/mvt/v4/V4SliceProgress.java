package cn.geoair.map.dynamic.statics.mvt.v4;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.percent.GiProgressReporter;

/**
 * V4 的执行进度与计数。
 *
 * <p>上报口径与 V3 一致：{@link GiProgressReporter#report(Long, Long)} 的
 * {@code total} 是总阶段数、{@code current} 是已完成阶段数，这样管理端换引擎时不用改前端。</p>
 *
 * <p>V3 的阶段进度靠 Spark Stage 监听器，V4 没有 Spark，所以由驱动自己在每一步结束时上报。</p>
 *
 * @author 张逢吉
 */
public final class V4SliceProgress {

    private static final GiLogger LOG = GirLoggerFactory.getLogger();

    private final GiProgressReporter reporter;
    private final int totalStages;
    private int completedStages;
    private long featuresRead;
    private long tilesWritten;
    private long bytesWritten;
    private long batchesWritten;

    public V4SliceProgress(GiProgressReporter reporter, int totalStages) {
        this.reporter = reporter;
        this.totalStages = Math.max(1, totalStages);
    }

    /** 进入一个阶段。 */
    public void stage(String name) {
        LOG.info("V4 阶段开始：{}", name);
    }

    /** 结束一个阶段并上报进度。 */
    public void completeStage(String detail) {
        completedStages++;
        LOG.info("V4 阶段完成（{}/{}）：{}", completedStages, totalStages, detail);
        report();
    }

    /** 读取到一个要素。 */
    public void addFeature() {
        featuresRead++;
    }

    /** 写出一个瓦片。 */
    public void addTile(int bytes) {
        tilesWritten++;
        bytesWritten += bytes;
    }

    /** 批量写出若干瓦片（PostgreSQL 分批提交时用）。 */
    public void addTiles(int count, long bytes) {
        tilesWritten += count;
        bytesWritten += bytes;
    }

    /** 完成一批写出（PostgreSQL 分批提交时用）。 */
    public void addBatch() {
        batchesWritten++;
    }

    /** 提交当前进度（读取阶段按固定间隔调用，避免刷爆上报通道）。 */
    public void report() {
        if (reporter != null) {
            reporter.report((long) totalStages, (long) completedStages);
        }
    }

    public long getFeaturesRead() {
        return featuresRead;
    }

    public long getTilesWritten() {
        return tilesWritten;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public long getBatchesWritten() {
        return batchesWritten;
    }
}
