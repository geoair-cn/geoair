package cn.geoair.map.dynamic.file.core.transfer;

import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * 无状态传输引擎的结果快照。
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class GeoTransferResult {

    /** 最终执行状态。 */
    private TranStatus status = TranStatus.INIT;

    /** 读取到的要素总数。 */
    private long totalCount;

    /** 成功写入的要素数。 */
    private long successCount;

    /** 失败或被跳过的要素数。 */
    private long failCount;

    /** 任务开始时间。 */
    private long startTime;

    /** 任务结束时间。 */
    private long endTime;

    /** 任务耗时。 */
    private long elapsedTime;

    /** 失败原因摘要。 */
    private String errorMessage;

    /** 捕获到的异常集合。 */
    private List<Throwable> exceptions = new ArrayList<>();
}
