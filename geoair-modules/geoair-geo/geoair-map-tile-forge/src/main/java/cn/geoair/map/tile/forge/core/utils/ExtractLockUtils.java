package cn.geoair.map.tile.forge.core.utils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 本地抽取互斥锁
 * <p>
 * 瓦片抽取到本地临时目录时，存在「判断文件不存在 -> 抽取并写文件」的 check-then-act 窗口。
 * XYZ 请求是高并发的，同一个 bundle / 瓦片一旦在这个窗口里被多个请求同时命中，
 * 就会出现重复下载，更糟的是后来的请求会读到前一个请求还没写完的半截文件。
 * <p>
 * 这里按目标文件路径做分段互斥：同一路径只可能落到同一把锁上，因此同一时刻只有一个线程在抽取，
 * 其余线程会等它落盘后直接读现成文件。分段是为了把锁数量固定住，不随文件数量无限增长。
 *
 * @author 张俊
 */
public final class ExtractLockUtils {

    /**
     * 分段数量，取 2 的幂便于用位运算取模
     */
    private static final int STRIPES = 64;

    /**
     * 分段锁数组，构造后不再变更
     */
    private static final ReentrantLock[] STRIPE_LOCKS = new ReentrantLock[STRIPES];

    /**
     * 获取锁的默认等待上限，超过就放弃并报错，宁可这一个瓦片失败，也不能把线程池拖死
     */
    private static final long DEFAULT_TIMEOUT_MILLIS = 60L * 1000L;

    static {
        for (int i = 0; i < STRIPES; i++) {
            STRIPE_LOCKS[i] = new ReentrantLock();
        }
    }

    private ExtractLockUtils() {
    }

    /**
     * 带锁执行一段逻辑
     *
     * @param key       互斥键，同一个键会串行执行，建议用目标文件的绝对路径
     * @param extractor 真正要执行的抽取逻辑
     * @param <T>       返回值类型
     * @return 抽取逻辑的返回值
     * @throws Exception 抽取逻辑抛出的异常，或等待锁超时
     */
    public static <T> T withLock(String key, LockedSupplier<T> extractor) throws Exception {
        return withLock(key, DEFAULT_TIMEOUT_MILLIS, extractor);
    }

    /**
     * 带锁执行一段逻辑，可指定等待上限
     *
     * @param key              互斥键
     * @param timeoutMillis    等待锁的毫秒数
     * @param extractor        真正要执行的抽取逻辑
     * @param <T>              返回值类型
     * @return 抽取逻辑的返回值
     * @throws Exception 抽取逻辑抛出的异常，或等待锁超时
     */
    public static <T> T withLock(String key, long timeoutMillis, LockedSupplier<T> extractor) throws Exception {
        if (key == null || key.isEmpty()) {
            return extractor.get();
        }
        ReentrantLock lock = stripeFor(key);
        boolean acquired;
        try {
            acquired = lock.tryLock(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("等待文件抽取锁时被中断：" + key, e);
        }
        if (!acquired) {
            throw new RuntimeException("等待文件抽取锁超时（" + timeoutMillis + "ms）：" + key);
        }
        try {
            return extractor.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 按互斥键取对应分段的锁
     */
    static ReentrantLock stripeFor(String key) {
        int hash = key.hashCode();
        // 与 MAX_VALUE 与运算再取模，避免 Integer.MIN_VALUE 取绝对值后仍为负数导致下标越界
        int index = (hash & Integer.MAX_VALUE) % STRIPES;
        return STRIPE_LOCKS[index];
    }

    /**
     * 允许抛出受检异常的任务，方便在锁内直接写抽取逻辑
     */
    @FunctionalInterface
    public interface LockedSupplier<T> {
        T get() throws Exception;
    }
}
