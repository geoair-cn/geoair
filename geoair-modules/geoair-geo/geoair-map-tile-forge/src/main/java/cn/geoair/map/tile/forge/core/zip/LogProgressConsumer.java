package cn.geoair.map.tile.forge.core.zip;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.percent.GiProgressListener;
import cn.geoair.base.percent.GirProgressReporter;
import cn.geoair.base.util.GutilPercent;

/**
 * 日志进度消费者：把原始进度上报转换为日志输出。
 * <p>
 * 内部使用 {@link GirProgressReporter} 按步长节流（默认每 1% 回调一次），
 * 通过 {@link GiProgressListener} 在日志中打印进度条与百分比，便于任务执行时观察进度。
 * <p>
 * 用法：直接实现 {@link ProgressConsumer} 使用，或将其作为 {@link GiProgressReporter} 的
 * 简化替代（两者都实现了 {@link cn.geoair.base.percent.GiProgressReporter}）。
 *
 * @author ：张俊
 * @date ：Created in 2026/4/23 14:10
 */
public class LogProgressConsumer implements ProgressConsumer {

    /**
     * 日志对象
     */
    private static final GiLogger log = GirLoggerFactory.getLogger();

    /**
     * 进度上报器：接收原始 (总量, 当前量) 上报，按步长节流后回调监听器
     */
    private final GirProgressReporter percentReporter = new GirProgressReporter(1, new GiProgressListener() {

        /**
         * 进度开始：记录任务总量
         */
        @Override
        public void onStart(Number total) {
            log.info("进度开始，总任务量: {}", total);
        }

        /**
         * 进度更新：打印进度条与百分比（按步长节流，每 1% 触发一次）
         */
        @Override
        public void onUpdate(Number percent) {
            if (log.isInfoEnabled()) {
                log.info("当前进度: {}%  {}", percent, GutilPercent.getProgressDisplay(percent.doubleValue()));
            }
        }
    });

    /**
     * 上报一次进度，交由内部上报器节流并输出到日志
     *
     * @param allCount    任务总量
     * @param currentCount 当前已完成量
     */
    @Override
    public void report(Long allCount, Long currentCount) {
        percentReporter.report(allCount, currentCount);
    }
}
