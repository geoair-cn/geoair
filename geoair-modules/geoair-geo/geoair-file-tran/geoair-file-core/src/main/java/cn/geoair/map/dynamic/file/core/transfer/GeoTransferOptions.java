package cn.geoair.map.dynamic.file.core.transfer;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 无状态传输引擎的执行选项。
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class GeoTransferOptions {

    /** 每批处理的要素数量。 */
    private int batchSize = 3000;

    /** 批次或记录失败后的处理方式。 */
    private ErrorPolicy errorPolicy = ErrorPolicy.FAIL_FAST;

    /** 是否在任务结束后关闭 Reader 和 Writer。 */
    private boolean autoCloseResource = true;

    /** 总执行超时，单位毫秒；小于等于 0 表示不限制。 */
    private long timeoutMillis = 30 * 60 * 1000L;
}
