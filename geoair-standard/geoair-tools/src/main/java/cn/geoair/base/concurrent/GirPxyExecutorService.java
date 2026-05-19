package cn.geoair.base.concurrent;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/** 代理的线程池 */
public class GirPxyExecutorService implements ExecutorService {
    private static final GiLogger log = GirLogger.getLoger(GirPxyExecutorService.class);

    protected final ExecutorService delegate;
    protected final List<GirTaskInterceptor> interceptors;

    public GirPxyExecutorService(ExecutorService delegate, List<GirTaskInterceptor> interceptors) {
        this.delegate = delegate;
        this.interceptors = interceptors != null ? interceptors : getDefault();
    }

    public ExecutorService getDelegate() {
        return delegate;
    }

    public static List<GirTaskInterceptor> getDefault() {
        ArrayList<GirTaskInterceptor> list = new ArrayList<>();
        list.add(new DefaultLogTaskInterceptor());
        return list;
    }

    /**
     * 静态工厂方法，创建支持任务拦截的线程池包装器
     *
     * @param delegate 原始线程池
     * @param interceptor 任务拦截器，可为null（使用默认实现）
     * @return 包装后的线程池
     */
    public static GirPxyExecutorService of(
            ExecutorService delegate, GirTaskInterceptor interceptor) {
        ArrayList<GirTaskInterceptor> interceptors = new ArrayList<>();
        interceptors.add(interceptor);
        return new GirPxyExecutorService(delegate, interceptors);
    }

    /**
     * 静态工厂方法，创建支持任务拦截的线程池包装器
     *
     * @param delegate 原始线程池
     * @param interceptors 任务拦截器，可为null（使用默认实现）
     * @return 包装后的线程池
     */
    public static GirPxyExecutorService of(
            ExecutorService delegate, List<GirTaskInterceptor> interceptors) {
        return new GirPxyExecutorService(delegate, interceptors);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(wrap(command));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(wrap(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(wrap(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return delegate.invokeAll(wrapTasks(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(
            Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(wrapTasks(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrapTasks(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrapTasks(tasks), timeout, unit);
    }

    @Override
    public void shutdown() {
        log.debug("关闭GirExecutorService线程池: {}", this);
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        log.debug("立即关闭GirExecutorService线程池: {}", this);
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        log.debug("等待GirExecutorService线程池终止: {}, 超时时间: {} {}", this, timeout, unit);
        return delegate.awaitTermination(timeout, unit);
    }

    /** 包装 Callable 任务，支持链式执行所有拦截器 */
    protected <T> Callable<T> wrap(Callable<T> task) {
        return () -> {
            // 1. 执行所有拦截器的 beforeTask，收集 taskId（取第一个拦截器返回的ID）
            String taskId = null;
            for (GirTaskInterceptor interceptor : interceptors) {
                String currentId = interceptor.beforeTask(task);
                if (taskId == null) {
                    taskId = currentId;
                }
            }

            try {
                // 2. 执行真实任务
                return task.call();
            } catch (Exception ex) {
                // 3. 异常时执行所有拦截器的 onTaskException
                for (GirTaskInterceptor interceptor : interceptors) {
                    try {
                        interceptor.onTaskException(taskId, task, ex);
                    } catch (Exception e) {
                        log.error("拦截器执行异常处理时出错", e);
                    }
                }
                throw ex;
            } finally {
                // 4. 最终一定执行所有拦截器的 afterTask
                for (GirTaskInterceptor interceptor : interceptors) {
                    try {
                        interceptor.afterTask(taskId, task);
                    } catch (Exception e) {
                        log.error("拦截器执行结束回调时出错", e);
                    }
                }
            }
        };
    }

    /** 包装 Runnable 任务，支持链式执行所有拦截器 */
    protected Runnable wrap(Runnable task) {
        return () -> {
            // 1. 执行所有拦截器的 beforeTask
            String taskId = null;
            for (GirTaskInterceptor interceptor : interceptors) {
                String currentId = interceptor.beforeTask(task);
                if (taskId == null) {
                    taskId = currentId;
                }
            }

            try {
                // 2. 执行真实任务
                task.run();
            } catch (Exception ex) {
                // 3. 异常时执行所有拦截器
                for (GirTaskInterceptor interceptor : interceptors) {
                    try {
                        interceptor.onTaskException(taskId, task, ex);
                    } catch (Exception e) {
                        log.error("拦截器执行异常处理时出错", e);
                    }
                }
                throw ex;
            } finally {
                // 4. 最终执行所有拦截器的结束方法
                for (GirTaskInterceptor interceptor : interceptors) {
                    try {
                        interceptor.afterTask(taskId, task);
                    } catch (Exception e) {
                        log.error("拦截器执行结束回调时出错", e);
                    }
                }
            }
        };
    }

    private <T> Collection<? extends Callable<T>> wrapTasks(
            Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(this::wrap).collect(Collectors.toList());
    }
}
