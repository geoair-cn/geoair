package cn.geoair.map.dynamic.tools.simple.executor;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/6/16 15:11 @description： 默认任务拦截器实现
 */
public class DefaultTaskInterceptor implements GirTaskInterceptor {

    private static final GiLogger log = GirLoggerFactory.getLogger(DefaultTaskInterceptor.class);

    @Override
    public String beforeTask(Object task) {
        String taskId = java.util.UUID.randomUUID().toString();
        log.info("任务[{}]开始执行: {}", taskId, task.getClass().getName());
        return taskId;
    }

    @Override
    public void afterTask(String taskId, Object task) {
        log.info("任务[{}]执行完成", taskId);
    }

    @Override
    public void onTaskException(String taskId, Object task, Exception ex) {
        log.error("任务[{}]执行异常: {}", taskId, ex.getMessage(), ex);
    }
}
