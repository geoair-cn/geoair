package cn.geoair.map.tile.forge.fuser.execution;

import java.util.concurrent.TimeUnit;

/** 单张处理使用单调时钟总截止时间，重试不能重新获得完整超时预算。 */
public class TileOperationContext {
    private static final ThreadLocal<TileOperationContext> CURRENT = new ThreadLocal<>();
    private final TileCancellationToken cancellation;
    private final long started = System.nanoTime();
    private final long timeoutNanos;

    public TileOperationContext(TileCancellationToken cancellation, long timeoutMillis) {
        if (cancellation == null || timeoutMillis <= 0)
            throw new IllegalArgumentException("取消令牌及正超时时间不能为空");
        this.cancellation = cancellation;
        this.timeoutNanos = Math.multiplyExact(timeoutMillis, 1_000_000L);
    }

    public TileCancellationToken getCancellation() {
        return cancellation;
    }

    public static TileOperationContext current() {
        return CURRENT.get();
    }

    public TileOperationScope bind() {
        TileOperationContext previous = CURRENT.get();
        CURRENT.set(this);
        return new TileOperationScope(previous);
    }

    static void restore(TileOperationContext previous) {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    public void check() {
        if (cancellation.isCancelled() || Thread.currentThread().isInterrupted())
            throw new TileOperationException(TileOperationStatus.CANCELLED, "瓦片处理已取消");
        if (System.nanoTime() - started >= timeoutNanos)
            throw new TileOperationException(TileOperationStatus.TIMED_OUT, "瓦片处理超过总截止时间");
    }

    public int remainingMillis() {
        check();
        long remaining = timeoutNanos - (System.nanoTime() - started);
        long millis = remaining / 1000000L + (remaining % 1000000L > 0 ? 1 : 0);
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, millis));
    }

    public static void checkCurrent() {
        TileOperationContext context = current();
        if (context != null) context.check();
    }

    public void sleep(long millis) {
        long until =
                System.nanoTime()
                        + TimeUnit.MILLISECONDS.toNanos(Math.min(millis, remainingMillis()));
        while (System.nanoTime() < until) {
            check();
            try {
                Thread.sleep(
                        Math.min(
                                50,
                                Math.max(
                                        1,
                                        TimeUnit.NANOSECONDS.toMillis(until - System.nanoTime()))));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                check();
            }
        }
        check();
    }
}
