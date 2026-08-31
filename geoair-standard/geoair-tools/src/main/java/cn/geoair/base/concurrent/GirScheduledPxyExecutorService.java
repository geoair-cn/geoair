package cn.geoair.base.concurrent;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/** 代理的定时任务线程池 */
public class GirScheduledPxyExecutorService extends GirPxyExecutorService
        implements ScheduledExecutorService {
    private static final GiLogger log =
            GirLoggerFactory.getLogger(GirScheduledPxyExecutorService.class);

    public GirScheduledPxyExecutorService(
            ExecutorService delegate, List<GirTaskInterceptor> interceptors) {
        super(delegate, interceptors);
    }

    /**
     * 静态工厂方法，创建支持任务拦截的线程池包装器
     *
     * @param delegate 原始线程池
     * @param interceptor 任务拦截器，可为null（使用默认实现）
     * @return 包装后的线程池
     */
    public static GirScheduledPxyExecutorService of(
            ExecutorService delegate, GirTaskInterceptor interceptor) {
        ArrayList<GirTaskInterceptor> interceptors = new ArrayList<>();
        interceptors.add(interceptor);
        return new GirScheduledPxyExecutorService(delegate, interceptors);
    }

    /**
     * 静态工厂方法，创建支持任务拦截的线程池包装器
     *
     * @param delegate 原始线程池
     * @param interceptors 任务拦截器，可为null（使用默认实现）
     * @return 包装后的线程池
     */
    public static GirScheduledPxyExecutorService of(
            ExecutorService delegate, List<GirTaskInterceptor> interceptors) {
        return new GirScheduledPxyExecutorService(delegate, interceptors);
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
    public ScheduledFuture<?> scheduleAtFixedRate(
            Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (delegate instanceof ScheduledExecutorService) {
            return ((ScheduledExecutorService) delegate)
                    .scheduleAtFixedRate(wrap(command), initialDelay, period, unit);
        } else {
            throw new UnsupportedOperationException("代理线程池不支持定时任务");
        }
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable command, long initialDelay, long delay, TimeUnit unit) {
        if (delegate instanceof ScheduledExecutorService) {
            return ((ScheduledExecutorService) delegate)
                    .scheduleWithFixedDelay(wrap(command), initialDelay, delay, unit);
        } else {
            throw new UnsupportedOperationException("代理线程池不支持定时任务");
        }
    }
}
