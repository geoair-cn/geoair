package cn.geoair.base.percent;

import cn.geoair.base.util.GutilPercent;

/**
 * {@link GirProgressReporter} 快速构造器工具类。
 * <p>
 * 提供与旧版 {@code cn.geoair.base.util.GutilPercent} 中
 * {@code getPercentConsumerInt} / {@code getPercentConsumerDouble}
 * 工厂方法同名的静态方法，便于老代码快速替换。
 * <p>
 * 旧代码替换示例：
 * <pre>
 * // 旧写法（旧接口 GiPercentUpdateConsumer 与旧工厂已移除）
 * GiPercentUpdateConsumer consumer = new GiPercentUpdateConsumer() {
 *     public void start(Number allCount) { ... }
 *     public void update(Number updatePercent) { ... }
 * };
 * GiPercentConsumer c = GutilPercent.getPercentConsumerDouble(consumer);
 *
 * // 新写法：接口换成 GiProgressListener（onStart/onUpdate），工厂换成本类，其余不变
 * GiProgressListener listener = new GiProgressListener() {
 *     public void onStart(Number total) { ... }
 *     public void onUpdate(Number percent) { ... }
 * };
 * GirProgressReporter reporter = GutilProgressReporter.getPercentConsumerDouble(listener);
 * </pre>
 *
 * @author ：张俊
 * @date ：Created in 2026/8/17
 */
public class GutilProgressReporter {

    /**
     * 私有构造器 - 工具类不可实例化
     */
    private GutilProgressReporter() {
    }

    /**
     * 创建默认 int 步长（{@link GutilPercent#DEFAULT_STEP}）的进度上报器
     *
     * @param listener 进度更新监听器，不能为 null
     * @return 进度上报器
     */
    public static GirProgressReporter getPercentConsumerInt(GiProgressListener listener) {
        return new GirProgressReporter(listener);
    }

    /**
     * 创建自定义 int 步长的进度上报器
     *
     * @param listener 进度更新监听器，不能为 null
     * @param step     步长（百分比），<= 0 表示每次变化都回调
     * @return 进度上报器
     */
    public static GirProgressReporter getPercentConsumerInt(GiProgressListener listener, int step) {
        return new GirProgressReporter(step, listener);
    }

    /**
     * 创建默认 double 步长（{@link GutilPercent#DEFAULT_STEP_DOUBLE}）的进度上报器
     *
     * @param listener 进度更新监听器，不能为 null
     * @return 进度上报器
     */
    public static GirProgressReporter getPercentConsumerDouble(GiProgressListener listener) {
        return new GirProgressReporter(listener);
    }

    /**
     * 创建自定义 double 步长的进度上报器
     *
     * @param listener 进度更新监听器，不能为 null
     * @param step     步长（百分比），<= 0 表示每次变化都回调
     * @return 进度上报器
     */
    public static GirProgressReporter getPercentConsumerDouble(GiProgressListener listener, double step) {
        return new GirProgressReporter(step, listener);
    }
}