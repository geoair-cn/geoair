package cn.geoair.map.dynamic.file.core.tran.model;

import cn.geoair.map.dynamic.file.core.enums.TranStatus;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 结构化转换结果 */
@Data
@Accessors(chain = true)
public class TranResult {

    // 最终状态
    private TranStatus status; // 转换状态

    // 核心统计
    private long totalCount = 0; // 总处理条数

    private long successCount = 0; // 成功条数

    private long failCount = 0; // 失败条数

    private double successRate = 0.0; // 成功率（%）

    // 时间信息
    private long startTime; // 开始时间（时间戳）

    private long endTime; // 结束时间（时间戳）

    private long elapsedTime; // 总耗时（ms）

    // 异常信息
    private List<Throwable> exceptions = new ArrayList<>(); // 异常列表

    private String errorMsg; // 核心错误信息

    // 扩展数据
    private Map<String, Object> extData = new HashMap<>();

    // 快捷创建成功结果
    public static TranResult success() {
        return new TranResult().setStatus(TranStatus.SUCCESS).setErrorMsg("转换成功");
    }

    // 快捷创建失败结果
    public static TranResult fail(String errorMsg, Throwable e) {
        TranResult result = new TranResult().setStatus(TranStatus.FAILED).setErrorMsg(errorMsg);
        result.getExceptions().add(e);
        return result;
    }

    public TranResult calculateSuccessRate() {
        if (totalCount == 0) {
            this.successRate = 0.0;
        } else {
            this.successRate = (double) successCount / totalCount * 100;
        }
        return this;
    }

    // 快捷创建终止结果
    public static TranResult aborted() {
        return new TranResult().setStatus(TranStatus.ABORTED).setErrorMsg("转换被终止");
    }
}
