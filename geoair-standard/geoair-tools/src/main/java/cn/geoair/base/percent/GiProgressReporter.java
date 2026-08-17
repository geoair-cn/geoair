package cn.geoair.base.percent;

import java.io.Serializable;

/**
 * 进度上报回调接口。
 * <p>
 * 任务执行方在循环中调用 {@link #report(Long, Long)} 上报"总量 + 当前完成量"，
 * 由实现方决定如何呈现进度（例如 {@link GirProgressReporter} 会按步长节流后转发给 {@link GiProgressListener}）。
 * <p>
 * 继承 {@link Serializable}，便于在 Spark 闭包等分布式环境中传递。
 *
 * <pre>
 * // 任务循环中
 * progressReporter.report(total, current);
 * </pre>
 *
 * @see GirProgressReporter
 * @see GiProgressListener
 */
@FunctionalInterface
public interface GiProgressReporter extends Serializable {

    /**
     * 上报一次进度
     *
     * @param total   任务总量
     * @param current 当前已完成量
     */
    void report(Long total, Long current);
}
