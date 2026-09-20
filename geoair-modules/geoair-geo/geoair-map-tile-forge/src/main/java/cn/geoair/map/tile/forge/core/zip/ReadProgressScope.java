package cn.geoair.map.tile.forge.core.zip;

/**
 * 读取进度作用域。
 * <p>
 * readRange 在子类里实现，调用点又散落在中央目录扫描、条目解压里，把 ProgressConsumer 一路透传下去
 * 会污染一大片方法签名。这里换一种做法：用一个线程私有的槽位承载「当前这次读取任务」的进度消费者，
 * 由 {@link ICompressionHandler} 上带 ProgressConsumer 的重载方法开启和关闭，
 * 底层真正耗时的大块读取通过 {@link #report(long, long)} 往上报。
 * <p>
 * 槽位是按线程走的，多个请求线程互不影响。没有开启作用域时 {@link #report(long, long)} 什么都不做，
 * 所以对现有调用链没有任何影响。
 *
 * @author ：张俊
 * &#064;date ：Created in 2026/9/20
 */
public final class ReadProgressScope {

    /**
     * 当前线程的进度消费者，null 表示不上报
     */
    private static final ThreadLocal<ProgressConsumer> HOLDER = new ThreadLocal<>();

    private ReadProgressScope() {
    }

    /**
     * 开启进度作用域
     *
     * @param consumer 进度消费者，可为 null
     * @return 上一层作用域的消费者，用于退出时还原
     */
    public static ProgressConsumer begin(ProgressConsumer consumer) {
        ProgressConsumer previous = HOLDER.get();
        HOLDER.set(consumer);
        return previous;
    }

    /**
     * 关闭进度作用域，还原到上一层
     *
     * @param previous {@link #begin(ProgressConsumer)} 的返回值
     */
    public static void end(ProgressConsumer previous) {
        if (previous == null) {
            HOLDER.remove();
        } else {
            HOLDER.set(previous);
        }
    }

    /**
     * 上报一次进度，未开启作用域时不做任何事
     *
     * @param total   本次读取的总字节数
     * @param current 已读取的字节数
     */
    public static void report(long total, long current) {
        ProgressConsumer consumer = HOLDER.get();
        if (consumer == null) {
            return;
        }
        try {
            consumer.report(total, current);
        } catch (Exception e) {
            // 进度上报是旁路逻辑，出问题不能影响数据读取
        }
    }
}
