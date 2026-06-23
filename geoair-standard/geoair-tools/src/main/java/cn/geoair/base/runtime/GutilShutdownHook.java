package cn.geoair.base.runtime;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JVM 关闭钩子管理器
 * <p>
 * 统一管理应用程序的关闭钩子，确保资源在 JVM 关闭时正确释放
 * </p>
 *
 * @author 张俊
 * @date Created in 2026/06/23
 */

public class GutilShutdownHook {

    private static final GiLogger log = GirLoggerFactory.getLogger(GutilShutdownHook.class);
    /**
     * 单例实例
     */
    private static final GutilShutdownHook INSTANCE = new GutilShutdownHook();

    /**
     * 关闭任务列表
     */
    private final List<Runnable> shutdownTasks = new ArrayList<>();

    /**
     * 是否已注册
     */
    private final AtomicBoolean registered = new AtomicBoolean(false);

    /**
     * 是否正在关闭
     */
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    /**
     * 私有构造函数（单例模式）
     */
    private GutilShutdownHook() {
    }

    /**
     * 获取单例实例
     *
     * @return ShutdownHookManager 实例
     */
    public static GutilShutdownHook getInstance() {
        return INSTANCE;
    }

    /**
     * 注册关闭任务
     *
     * @param task 关闭任务
     */
    public void registerTask(Runnable task) {
        if (task == null) {
            return;
        }

        synchronized (shutdownTasks) {
            shutdownTasks.add(task);
            log.debug("已注册关闭任务: {}", task.getClass().getSimpleName());
        }

        // 确保钩子已注册
        registerHook();
    }

    /**
     * 注册关闭任务（带名称）
     *
     * @param taskName 任务名称
     * @param task     关闭任务
     */
    public void registerTask(String taskName, Runnable task) {
        if (task == null) {
            return;
        }

        synchronized (shutdownTasks) {
            shutdownTasks.add(() -> {
                try {
                    log.debug("执行关闭任务: {}", taskName);
                    task.run();
                } catch (Exception e) {
                    log.error("执行关闭任务失败: {}", taskName, e);
                }
            });
            log.debug("已注册关闭任务: {}", taskName);
        }

        registerHook();
    }

    /**
     * 注册 JVM 关闭钩子
     */
    private void registerHook() {
        if (!registered.compareAndSet(false, true)) {
            return;
        }

        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("JVM 关闭钩子触发，开始执行 {} 个关闭任务...", shutdownTasks.size());
                shuttingDown.set(true);

                // 逆序执行（先注册的后执行）
                List<Runnable> tasks;
                synchronized (shutdownTasks) {
                    tasks = new ArrayList<>(shutdownTasks);
                }

                // 逆序执行
                for (int i = tasks.size() - 1; i >= 0; i--) {
                    try {
                        tasks.get(i).run();
                    } catch (Exception e) {
                        log.error("执行关闭任务失败", e);
                    }
                }

                log.info("所有关闭任务执行完成");
            }, "ShutdownHookManager-Hook"));

            log.info("ShutdownHookManager 已注册");
        } catch (Exception e) {
            log.error("注册 Shutdown Hook 失败", e);
        }
    }

    /**
     * 手动触发关闭
     * <p>
     * 用于在应用程序正常关闭时释放资源
     * </p>
     */
    public void shutdown() {
        if (shuttingDown.compareAndSet(false, true)) {
            log.info("手动触发关闭，开始执行 {} 个关闭任务...", shutdownTasks.size());

            List<Runnable> tasks;
            synchronized (shutdownTasks) {
                tasks = new ArrayList<>(shutdownTasks);
            }

            // 逆序执行
            for (int i = tasks.size() - 1; i >= 0; i--) {
                try {
                    tasks.get(i).run();
                } catch (Exception e) {
                    log.error("执行关闭任务失败", e);
                }
            }

            log.info("手动关闭完成");
        }
    }

    /**
     * 检查是否正在关闭
     *
     * @return true 表示正在关闭
     */
    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    /**
     * 获取已注册的任务数量
     *
     * @return 任务数量
     */
    public int getTaskCount() {
        synchronized (shutdownTasks) {
            return shutdownTasks.size();
        }
    }

    /**
     * 清空所有任务
     */
    public void clearTasks() {
        synchronized (shutdownTasks) {
            shutdownTasks.clear();
            log.debug("已清空所有关闭任务");
        }
    }
}
