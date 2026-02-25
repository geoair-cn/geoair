package cn.geoair.map.dynamic.tools.simple.executor;


import cn.geoair.gtc.base.log.GiLogger;
import cn.geoair.gtc.base.log.GirLogger;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 代理的线程池
 */
public abstract class GirExecutorService implements ScheduledExecutorService {
    private static final GiLogger log = GirLogger.getLoger(GirExecutorService.class);

    private final ExecutorService delegate;
    private final GirTaskInterceptor interceptor;


    public GirExecutorService(ExecutorService delegate, GirTaskInterceptor interceptor) {
        this.delegate = delegate;
        this.interceptor = interceptor != null ? interceptor : new DefaultTaskInterceptor();
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
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(wrapTasks(tasks));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.invokeAll(wrapTasks(tasks), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(wrapTasks(tasks));
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(wrapTasks(tasks), timeout, unit);
    }

    @Override
    public void shutdown() {
        log.debug("关闭线程池: {}", this);
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        log.debug("立即关闭线程池: {}", this);
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
        log.debug("等待线程池终止: {}, 超时时间: {} {}", this, timeout, unit);
        return delegate.awaitTermination(timeout, unit);
    }

    private <T> Callable<T> wrap(Callable<T> task) {
        return () -> {
            String taskId = interceptor.beforeTask(task);
            try {
                return task.call();
            } catch (Exception ex) {
                interceptor.onTaskException(taskId, task, ex);
                throw ex;
            } finally {
                interceptor.afterTask(taskId, task);
            }
        };
    }

    private Runnable wrap(Runnable task) {
        return () -> {
            String taskId = interceptor.beforeTask(task);
            try {
                task.run();
            } catch (Exception ex) {
                interceptor.onTaskException(taskId, task, ex);
                throw ex;
            } finally {
                interceptor.afterTask(taskId, task);
            }
        };
    }

    private <T> Collection<? extends Callable<T>> wrapTasks(Collection<? extends Callable<T>> tasks) {
        return tasks.stream().map(this::wrap).collect(Collectors.toList());
    }


    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        if (delegate instanceof ScheduledExecutorService) {
            return ((ScheduledExecutorService) delegate).schedule(wrap(command), delay, unit);
        } else {
            throw new UnsupportedOperationException("代理线程池不支持定时任务");
        }
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        if (delegate instanceof ScheduledExecutorService) {
            return ((ScheduledExecutorService) delegate).schedule(wrap(callable), delay, unit);
        } else {
            throw new UnsupportedOperationException("代理线程池不支持定时任务");
        }
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (delegate instanceof ScheduledExecutorService) {
            return ((ScheduledExecutorService) delegate).scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
        } else {
            throw new UnsupportedOperationException("代理线程池不支持定时任务");
        }
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (delegate instanceof ScheduledExecutorService) {
            return ((ScheduledExecutorService) delegate).scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
        } else {
            throw new UnsupportedOperationException("代理线程池不支持定时任务");
        }
    }


}
