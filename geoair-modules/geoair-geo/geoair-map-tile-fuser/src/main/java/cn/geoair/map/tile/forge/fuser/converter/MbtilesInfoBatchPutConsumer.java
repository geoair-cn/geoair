package cn.geoair.map.tile.forge.fuser.converter;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesInfo;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;
import cn.geoair.map.tile.forge.fuser.utils.FuserCacheUtils;
import cn.hutool.core.io.unit.DataSizeUtil;
import com.alibaba.druid.pool.DruidDataSource;
import lombok.Getter;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/25 11:51
 * @description： 批量插入消费模型
 */
public class MbtilesInfoBatchPutConsumer implements Consumer<MbtilesInfo>, Closeable {

    private static GiLogger log = GirLoggerFactory.getLogger(MbtilesInfoBatchPutConsumer.class);

    @Getter
    private final ConvertStats stats = new ConvertStats();

    private final boolean needReverseY;
    private final boolean overwrite;
    private final int batchSize;
    private final DruidDataSource dataSource;
    private final Integer zoom;
    private final List<MbtilesInfo> batchArgs;
    private final ReentrantLock lock = new ReentrantLock();
    private long layerStartTime;
    private String tileCountToLog;
    private volatile boolean closed = false;

    public MbtilesInfoBatchPutConsumer(boolean needReverseY, boolean overwrite,
                                       int batchSize, DruidDataSource dataSource, Integer zoom, long tileCount) {
        this.needReverseY = needReverseY;
        this.overwrite = overwrite;
        this.batchSize = batchSize;
        this.dataSource = dataSource;
        this.zoom = zoom;
        this.batchArgs = new ArrayList<>(batchSize);
        this.layerStartTime = System.currentTimeMillis();
        this.tileCountToLog = tileCount == 0 ? "" : tileCount + "";
    }

    @Override
    public void accept(MbtilesInfo tile) {
        if (tile == null) {
            synchronized (stats) {
                stats.failed++;
            }
            return;
        }
        if (closed) {
            log.warn("消费者已关闭，拒绝新任务: z={}, x={}, y={}",
                    tile.getZoomLevel(), tile.getTileColumn(), tile.getTileRow());
            return;
        }

        // 统计总数（使用 synchronized 保证原子性）
        synchronized (stats) {
            stats.total++;
        }

        try {
            // 校验数据
            if (tile.getTileData() == null || tile.getTileData().length == 0) {
                synchronized (stats) {
                    stats.failed++;
                }
                log.debug("瓦片数据为空，跳过: z={}, x={}, y={}",
                        tile.getZoomLevel(), tile.getTileColumn(), tile.getTileRow());
                return;
            }

            // 计算存储 Y（根据需要翻转）
            int storeY = FuserCacheUtils.getStoreY(tile.getZoomLevel(), tile.getY(), needReverseY);
            tile.setTileRow(storeY);

            // 获取锁，保证批量操作的线程安全
            lock.lock();
            try {
                // 添加到批量
                batchArgs.add(tile);
                synchronized (stats) {
                    stats.totalSize += tile.getTileData().length;
                }

                // 达到批量大小时提交
                if (batchArgs.size() >= batchSize) {
                    doBatchSubmit();
                }
            } finally {
                lock.unlock();
            }

        } catch (Exception e) {
            synchronized (stats) {
                stats.failed++;
            }
            log.error("处理瓦片失败: z={}, x={}, y={}",
                    tile.getZoomLevel(), tile.getTileColumn(), tile.getTileRow(), e);
        }
    }

    /**
     * 执行批量提交（必须在锁内调用）
     */
    private void doBatchSubmit() {
        if (batchArgs.isEmpty()) {
            return;
        }

        try {
            // 复制当前批次数据
            List<MbtilesInfo> batchToSubmit = new ArrayList<>(batchArgs);
            batchArgs.clear();

            // 执行批量插入
            int[] results = MbtilesUtils.putTileBatch(dataSource, overwrite, batchToSubmit);

            synchronized (stats) {
                stats.success += results[0];
                stats.skipped += results[1];
                stats.failed += results[2];
            }

            log.info("批量提交成功:总数量={}, 当前提交数量={}, zoom={}, 成功={}, 跳过={}, 失败={}",
                    tileCountToLog, batchToSubmit.size(), zoom, results[0], results[1], results[2]);

        } catch (Exception e) {
            log.error("批量提交失败: zoom={}", zoom, e);
            synchronized (stats) {
                stats.failed += batchArgs.size();
            }
            batchArgs.clear();
        }
    }

    /**
     * 关闭消费者，提交剩余数据
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        lock.lock();
        try {
            closed = true;
            // 提交剩余数据
            if (!batchArgs.isEmpty()) {
                doBatchSubmit();
            }
        } finally {
            lock.unlock();
        }

        long costTime = System.currentTimeMillis() - layerStartTime;
        log.info("层级 z={} 完成: 总数={}, 成功={}, 跳过={}, 失败={}, 耗时={}s, 总大小={}",
                zoom, stats.total, stats.success, stats.skipped, stats.failed,
                costTime / 1000, DataSizeUtil.format(stats.totalSize));
    }

    /**
     * 获取当前批次大小（用于监控）
     */
    public int getCurrentBatchSize() {
        lock.lock();
        try {
            return batchArgs.size();
        } finally {
            lock.unlock();
        }
    }
}
