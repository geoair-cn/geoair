package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import lombok.Data;

/** 统计吞吐与写入让行参数，在执行器创建时形成快照。 */
@Data
public class MbtilesStatisticsOptions {
    private int batchSize = 512;
    private int readOnlyBatchSize = 10000;
    private long sliceMillis = 25;
    private long yieldMillis = 25;
    private int writeBusyTimeoutMillis = 100;
    public MbtilesStatisticsOptions copy() {
        if(batchSize<1 || batchSize>100000 || readOnlyBatchSize<1 || readOnlyBatchSize>100000
                || sliceMillis<1 || yieldMillis<0 || writeBusyTimeoutMillis<1)
            throw new IllegalArgumentException("统计批大小须为 1 至 100000，时间片和锁等待须为正数，让行时间不能为负数");
        Math.multiplyExact(sliceMillis,1000000L);
        MbtilesStatisticsOptions copy=new MbtilesStatisticsOptions();
        copy.batchSize=batchSize;copy.readOnlyBatchSize=readOnlyBatchSize;
        copy.sliceMillis=sliceMillis;copy.yieldMillis=yieldMillis;copy.writeBusyTimeoutMillis=writeBusyTimeoutMillis;
        return copy;
    }
}
