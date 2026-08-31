package cn.geoair.base.percent;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilPercent;

/**
 * 进度上报器：把原始"总量 + 当前完成量"上报，节流成按步长跳变的百分比，再回调给 {@link GiProgressListener}。
 *
 * <p>典型用法：
 *
 * <pre>
 * GiProgressListener listener = (total) -&gt; {}, (percent) -&gt; {}; // 见 GiProgressListener
 * GirProgressReporter reporter = new GirProgressReporter(listener);
 *
 * // 任务循环中
 * reporter.report(total, current);
 * </pre>
 *
 * 第一次 {@link #report} 会自动触发 {@link GiProgressListener#onStart}； 若上报的总量发生变化（例如分批流式任务），会自动重新启动。
 *
 * @author ：张俊
 * @date ：Created in 2026/7/13 11:24
 */
public class GirProgressReporter implements GiProgressReporter {

    private static final GiLogger log = GirLoggerFactory.getLogger();

    /** 进度更新监听器 */
    private final GiProgressListener listener;

    /** 步长（百分比），<= 0 表示每次变化都回调 */
    private final double step;

    /** 上次已发布的进度（0 ~ 100） */
    private double lastPercent = 0.0;

    /** 是否已启动 */
    private boolean started = false;

    /** 当前记录的总数 */
    private Number totalCount = null;

    /**
     * 构造函数 - 使用默认步长（{@link GutilPercent#DEFAULT_STEP_DOUBLE}）
     *
     * @param listener 进度更新监听器，不能为 null
     */
    public GirProgressReporter(GiProgressListener listener) {
        this(GutilPercent.DEFAULT_STEP_DOUBLE, listener);
    }

    /**
     * 构造函数 - 自定义步长
     *
     * @param step 步长（百分比），<= 0 表示每次变化都回调
     * @param listener 进度更新监听器，不能为 null
     */
    public GirProgressReporter(double step, GiProgressListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("GiProgressListener 不能为 null");
        }
        this.step = step;
        this.listener = listener;
    }

    /**
     * 上报进度：自动完成首次启动、总数变化重启、步长节流与监听器回调。
     *
     * <p>无效上报（total 或 current 为 null、total <= 0）会被忽略并记录警告。
     *
     * @param total 任务总量
     * @param current 当前已完成量
     */
    @Override
    public void report(Long total, Long current) {
        if (total == null || current == null || total.doubleValue() <= 0) {
            log.warn("忽略无效进度上报：total={}, current={}", total, current);
            return;
        }

        // 首次上报或总数变化时自动启动
        if (!started || !total.equals(totalCount)) {
            doStart(total);
        }

        double percent = GutilPercent.getNextPercent(current, total, step, lastPercent);
        if (percent >= 0) {
            lastPercent = percent;
            notifyUpdate(percent);
        }
    }

    /**
     * 手动启动进度（可选，用于提前初始化）
     *
     * @param total 任务总量，不能为 null 且必须大于 0
     */
    public void start(Number total) {
        if (total == null || total.doubleValue() <= 0) {
            log.warn("启动失败：总数为 null 或 <= 0");
            return;
        }
        doStart(total);
    }

    /** 重置进度状态 */
    public void reset() {
        lastPercent = 0.0;
        started = false;
        totalCount = null;
        log.trace("进度状态已重置");
    }

    /**
     * 重置进度状态并重新启动
     *
     * @param total 新的任务总量
     */
    public void resetAndStart(Number total) {
        reset();
        start(total);
    }

    /** 是否已启动 */
    public boolean isStarted() {
        return started;
    }

    /** 获取当前记录的总数 */
    public Number getTotalCount() {
        return totalCount;
    }

    /** 获取当前进度（0 ~ 100，未启动时为 0） */
    public double getCurrentPercent() {
        return lastPercent;
    }

    /** 是否已完成（进度达到 100） */
    public boolean isComplete() {
        return lastPercent >= 100.0;
    }

    /** 执行启动逻辑：触发监听器 {@link GiProgressListener#onStart} 并重置节流状态 */
    private void doStart(Number total) {
        lastPercent = 0.0;
        totalCount = total;
        started = true;
        try {
            listener.onStart(total);
        } catch (Exception e) {
            log.error("进度监听器 onStart 回调异常", e);
        }
        log.trace("进度已启动，总数: {}", total);
    }

    /** 节流回调监听器 {@link GiProgressListener#onUpdate}，避免展示逻辑异常影响任务执行 */
    private void notifyUpdate(double percent) {
        try {
            listener.onUpdate(percent);
        } catch (Exception e) {
            log.error("进度监听器 onUpdate 回调异常", e);
        }
        log.trace("进度更新: {}%", percent);
    }
}
