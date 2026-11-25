package cn.geoair.map.dynamic.file.core.tran.model;

import cn.geoair.map.dynamic.file.core.enums.TranStatus;

import lombok.Data;
import lombok.experimental.Accessors;

/** 转换进度信息 */
@Data
@Accessors(chain = true)
public class TranProgress {

    private long totalFeatureCount = 0; // 数据源总要素数（所有数据总量）

    // 当前批次统计
    private long batchTotalCount = 0; // 当前批次处理总条数

    private long batchSuccessCount = 0; // 当前批次成功条数

    private long batchFailCount = 0; // 当前批次失败条数

    // 计算字段
    private double successRate = 0.0; // 当前批次成功率（%）

    private long elapsedTime = 0; // 当前批次已耗时（ms）

    // 状态
    private TranStatus status = TranStatus.RUNNING; // 转换状态

    private String message; // 进度描述信息

    // 计算成功率
    public TranProgress calculateSuccessRate() {
        if (batchTotalCount == 0) {
            this.successRate = 0.0;
        } else {
            this.successRate = (double) batchSuccessCount / batchTotalCount * 100;
        }
        return this;
    }
}
