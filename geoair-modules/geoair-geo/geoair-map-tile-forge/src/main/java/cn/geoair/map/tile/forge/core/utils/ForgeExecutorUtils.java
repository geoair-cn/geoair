package cn.geoair.map.tile.forge.core.utils;

import cn.hutool.extra.spring.SpringUtil;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/2 11:08
 * @description： TODO
 */
public class ForgeExecutorUtils {
    private static ExecutorService executor = null;

    public static ExecutorService getExecutor() {
        if (executor == null) {
            try {
                executor = SpringUtil.getBean(ExecutorService.class);
            } catch (Exception e) {
            }
        }
        if (executor == null) {
            int CORE_POOL_SIZE = 20;
            int MAX_POOL_SIZE = 200;
            long KEEP_ALIVE_TIME = 60L;
            BlockingQueue<Runnable> WORK_QUEUE = new LinkedBlockingQueue<>(10000);
            executor = new ThreadPoolExecutor(
                    CORE_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                    WORK_QUEUE, new ThreadFactory() {
                private final AtomicLong count = new AtomicLong(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "tile-precache-" + count.incrementAndGet());
                }
            }, new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：由调用线程执行
            );
        }

        return executor;
    }
}
