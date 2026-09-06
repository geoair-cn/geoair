package cn.geoair.map.dynamic.file.core.transfer;

/**
 * 批量空间数据传输时的错误处理策略。
 *
 * @author 张逢吉
 */
public enum ErrorPolicy {

    /** 任意读取或写入错误立即终止任务。 */
    FAIL_FAST,

    /** 当前批次失败时跳过整批并继续后续批次。 */
    SKIP_BATCH,

    /** 当前批次失败后降级为逐条写入，跳过失败记录并继续。 */
    SKIP_RECORD
}
