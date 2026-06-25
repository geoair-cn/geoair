package cn.geoair.map.tile.forge.fuser.converter;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesInfo;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.hutool.core.io.unit.DataSizeUtil;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/25 11:51
 * @description： 批量插入消费模型
 */
public class MbtilesInfoBatchPutConsumer implements Consumer<MbtilesInfo> {

    private static GiLogger log = GirLoggerFactory.getLogger(TileToMbtilesConverter.class);
    @Getter
    ConvertStats stats = new ConvertStats();
    boolean needReverseY;
    boolean overwrite;
    int batchSize;
    List<MbtilesInfo> batchArgs;
    DruidDataSource dataSource;
    Integer zoom;
    long layerStartTime;

    public MbtilesInfoBatchPutConsumer(boolean needReverseY, boolean overwrite, int batchSize, DruidDataSource dataSource, Integer zoom) {
        this.needReverseY = needReverseY;
        this.overwrite = overwrite;
        this.batchSize = batchSize;
        this.dataSource = dataSource;
        this.zoom = zoom;
        batchArgs = new ArrayList<>(batchSize);
        layerStartTime = System.currentTimeMillis();
    }


    @Override
    public void accept(MbtilesInfo tile) {
        stats.total++;
        try {
            // 读取瓦片数据

            if (tile.getDataSize() == 0) {
                stats.failed++;
                return;
            }
            // 计算存储 Y（根据需要翻转）
            int storeY = FuserCacheUtils.getStoreY(zoom, tile.getY(), needReverseY);

            // 添加到批量
            batchArgs.add(tile.setY(storeY));
            stats.totalSize += tile.getDataSize();

            if (batchArgs.size() >= batchSize) {
                int[] results = MbtilesUtils.putTileBatch(dataSource, overwrite, batchArgs);
                stats.success += results[0];
                stats.skipped += results[1];
                stats.failed += results[2];
                log.info("导入成功{}条，zoom：{}，总成功数量：{}", batchArgs.size(), zoom, stats.success);
                batchArgs.clear();
            }
        } catch (Exception e) {

            stats.failed++;
        }
    }

    public void doImportEnd() {
        // 执行剩余的批量插入
        if (!batchArgs.isEmpty()) {
            int[] results = MbtilesUtils.putTileBatch(dataSource, overwrite, batchArgs);
            stats.success += results[0];
            stats.skipped += results[1];
            stats.failed += results[2];
            log.info("执行剩余的批量插入成功{}条，zoom：{}，总成功数量：{}", batchArgs.size(), zoom, stats.success);
            batchArgs.clear();
        }
        log.info("层级 z={} 完成: 总数={}, 耗时={}ms,批次总大小={}",
                zoom, stats.total, System.currentTimeMillis() - layerStartTime, DataSizeUtil.format(stats.totalSize));
    }

}

