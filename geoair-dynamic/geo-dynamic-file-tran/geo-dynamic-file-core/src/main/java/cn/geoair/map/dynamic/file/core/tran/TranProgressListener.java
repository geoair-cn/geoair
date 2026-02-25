package cn.geoair.map.dynamic.file.core.tran;


import cn.geoair.map.dynamic.file.core.tran.model.TranProgress;

/**
 * 转换进度监听器
 * 用于实时监控转换进度（如前端展示、日志输出、告警触发）
 */
@FunctionalInterface
public interface TranProgressListener {

    /**
     * 进度更新回调
     * @param progress 进度信息（当前条数、成功率、状态等）
     */
    void onProgressUpdate(TranProgress progress);
}
