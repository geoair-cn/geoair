package cn.geoair.base.concurrent;

/**
 * @author ：张俊
 * @date ：Created in 2025/6/16 15:11
 * @description： 任务拦截器接口，定义任务执行前后的处理逻辑
 */

public interface GirTaskInterceptor {
    /**
     * 任务执行前的处理
     *
     * @param task 待执行的任务
     * @return 任务唯一标识，用于后续追踪
     */
    String beforeTask(Object task);

    /**
     * 任务执行后的处理（无论是否发生异常）
     *
     * @param taskId 任务唯一标识
     * @param task   已执行的任务
     */
    void afterTask(String taskId, Object task);

    /**
     * 任务发生异常时的处理
     *
     * @param taskId 任务唯一标识
     * @param task   执行失败的任务
     * @param ex     抛出的异常
     */
    void onTaskException(String taskId, Object task, Exception ex);
}
