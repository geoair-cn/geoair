package cn.geoair.map.dynamic.file.core.tran.model;

import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 转换进度信息
 */
@Data
@Accessors(chain = true)
public class TranProgress {

    private long allCount = 0; // 所有的总数
    // 核心统计
    private long totalCount = 0; // 当前批次处理条数

    private long successCount = 0; // 当前批次成功条数

    private long failCount = 0; // 当前批次失败条数

    // 计算字段
    private double successRate = 0.0; // 当前批次成功率（%）

    private long elapsedTime = 0; // 当前批次已耗时（ms）

    // 状态
    private TranStatus status = TranStatus.RUNNING; // 转换状态

    private String message; // 进度描述信息

    // 计算成功率
    public TranProgress calculateSuccessRate() {
        if (totalCount == 0) {
            this.successRate = 0.0;
        } else {
            this.successRate = (double) successCount / totalCount * 100;
        }
        return this;
    }
}
