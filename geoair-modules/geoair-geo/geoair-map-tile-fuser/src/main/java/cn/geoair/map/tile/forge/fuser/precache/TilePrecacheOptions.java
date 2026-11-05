package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;

import lombok.Data;

/** 分段大小、单张总超时及待执行窗口；不限制业务任务数或并行级别数。 */
@Data
@GaModel(text = "分段预缓存参数")
public class TilePrecacheOptions {
    @GaModelField(text = "分段瓦片数量")
    private int segmentSize = 10000;

    @GaModelField(text = "待执行分段窗口")
    private int pendingSegments = 256;

    @GaModelField(text = "单张总超时毫秒")
    private long tileTimeoutMillis = 60000;

    @GaModelField(text = "每段筛选总超时毫秒")
    private long inspectionTimeoutMillis = 60000;

    public void validate() {
        if (segmentSize <= 0
                || pendingSegments <= 0
                || tileTimeoutMillis <= 0
                || inspectionTimeoutMillis <= 0)
            throw new IllegalArgumentException("分段大小、待执行窗口和超时必须大于零");
        Math.multiplyExact(tileTimeoutMillis, 1000000L);
        Math.multiplyExact(inspectionTimeoutMillis, 1000000L);
    }

    /** 任务开始后不受外部配置对象修改影响。 */
    public TilePrecacheOptions copy() {
        TilePrecacheOptions copy = new TilePrecacheOptions();
        copy.segmentSize = segmentSize;
        copy.pendingSegments = pendingSegments;
        copy.tileTimeoutMillis = tileTimeoutMillis;
        copy.inspectionTimeoutMillis = inspectionTimeoutMillis;
        return copy;
    }
}
