package cn.geoair.map.dynamic.file.core.tran;

import cn.geoair.gtc.base.log.GiLogger;
import cn.geoair.gtc.base.log.GirLogger;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.tran.model.*;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 改造后的 GeoFileTran 实现类
 * 支持：上下文传递、进度监听、预处理/后处理、结构化结果、超时控制
 */

public class GeoFileTranImpl implements GeoFileTran {
    private static GiLogger log = GirLogger.getLoger(GeoFileTranImpl.class);
    // 上下文
    private TranContext context = new TranContext();

    // 全局异常处理器
    private ExceptionConsumer exceptionConsumer;

    // 进度监听器
    private TranProgressListener progressListener;

    // 进度统计
    private AtomicLong totalCount = new AtomicLong(0);
    private AtomicLong successCount = new AtomicLong(0);
    private AtomicLong failCount = new AtomicLong(0);

    // 状态控制
    private volatile TranStatus status = TranStatus.INIT;
    private long startTime;

    @Override
    public TranResult transform(GeoFileReader reader, GeoFileWriter writer) {
        return transform(reader, writer, new TranContext());
    }

    @Override
    public TranResult transform(GeoFileReader reader, GeoFileWriter writer, TranContext context) {
        // 初始化
        reset();
        this.context = context;
        this.status = TranStatus.RUNNING;
        this.startTime = System.currentTimeMillis();
        TranResult result = TranResult.success();

        try {
            // 1. 前置校验
            if (reader == null || writer == null) {
                throw new IllegalArgumentException("读取器/写入器不能为空");
            }

            // 2. 执行预处理
            if (context.getPreProcessor() != null) {
                boolean continueFlag = context.getPreProcessor().process(reader, writer, context);
                if (!continueFlag) {
                    status = TranStatus.ABORTED;
                    return TranResult.aborted();
                }
            }

            log.info("开始空间文件转换任务，上下文配置：{}", context);

            // 3. 读取表头并传递给写入器
            SimpleFeatureType featureType = reader.readHeader(this::handleException);
            if (featureType == null) {
                throw new RuntimeException("读取表头失败，SimpleFeatureType 为空");
            }
            writer.writeHeader(featureType, this::handleException);
            log.info("表头初始化完成，要素类型：{}", featureType.getName());

            // 4. 逐行转换（带超时控制）
            GirAdvOneRow oneRow;
            while ((oneRow = reader.readOneRow(this::handleException)) != null) {
                // 超时检查
                checkTimeout();

                try {
                    // 写入数据
                    writer.writeOneRow(oneRow, this::handleException);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    handleException(e);
                    log.error("第 {} 条记录转换失败", totalCount.get() + 1, e);

                    // 是否跳过错误记录
                    if (!context.isSkipErrorRecord()) {
                        status = TranStatus.FAILED;
                        result = TranResult.fail("单条记录转换失败且不允许跳过", e);
                        break;
                    }
                }

                // 统计更新
                totalCount.incrementAndGet();

                // 进度回调
                updateProgress();

                // 批量日志
                if (totalCount.get() % context.getBatchLogThreshold() == 0) {
                    log.info("转换进度：已处理 {} 条，成功 {} 条，失败 {} 条，成功率 {:.2f}%",
                            totalCount.get(), successCount.get(), failCount.get(),
                            (double) successCount.get() / totalCount.get() * 100);
                }
            }

            // 5. 转换完成，更新结果
            if (status == TranStatus.RUNNING) {
                status = TranStatus.SUCCESS;
                result.setTotalCount(totalCount.get())
                        .setSuccessCount(successCount.get())
                        .setFailCount(failCount.get())
                        .calculateSuccessRate()
                        .setStartTime(startTime)
                        .setEndTime(System.currentTimeMillis())
                        .setElapsedTime(result.getEndTime() - result.getStartTime());
            }

            log.info("转换任务执行完成，状态：{}，总耗时：{}ms", status, result.getElapsedTime());

        } catch (Exception e) {
            status = TranStatus.FAILED;
            result = TranResult.fail("转换任务执行异常", e);
            handleException(e);
        } finally {
            // 6. 执行后处理
            if (context.getPostProcessor() != null) {
                context.getPostProcessor().process(result, context);
            }

            // 7. 自动关闭资源
            if (context.isAutoCloseResource()) {
                closeQuietly(reader);
                closeQuietly(writer);
            }

            // 8. 最终进度回调
            updateProgress();
        }

        return result;
    }

    /**
     * 超时检查
     */
    private void checkTimeout() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - startTime > context.getTimeout()) {
            status = TranStatus.TIMEOUT;
            throw new RuntimeException("转换任务超时（超时时间：" + context.getTimeout() + "ms）");
        }
    }

    /**
     * 更新进度并回调监听器
     */
    private void updateProgress() {
        if (progressListener == null) {
            return;
        }

        TranProgress progress = new TranProgress()
                .setTotalCount(totalCount.get())
                .setSuccessCount(successCount.get())
                .setFailCount(failCount.get())
                .calculateSuccessRate()
                .setElapsedTime(System.currentTimeMillis() - startTime)
                .setStatus(status)
                .setMessage("转换中...");

        try {
            progressListener.onProgressUpdate(progress);
        } catch (Exception e) {
            log.error("进度监听器回调异常", e);
        }
    }

    /**
     * 统一异常处理
     */
    private void handleException(Exception e) {
        if (exceptionConsumer != null) {
            exceptionConsumer.accept(e);
        } else {
            log.error("转换异常", e);
        }
    }

    /**
     * 静默关闭资源
     */
    private void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
            log.info("资源 {} 关闭成功", closeable.getClass().getSimpleName());
        } catch (IOException e) {
            log.error("资源 {} 关闭失败", closeable.getClass().getSimpleName(), e);
        }
    }

    // ========== 接口方法实现 ==========
    @Override
    public GeoFileTran setExceptionConsumer(ExceptionConsumer exceptionConsumer) {
        this.exceptionConsumer = exceptionConsumer;
        return this;
    }

    @Override
    public GeoFileTran setProgressListener(TranProgressListener progressListener) {
        this.progressListener = progressListener;
        return this;
    }

    @Override
    public TranContext getContext() {
        return this.context;
    }

    @Override
    public void reset() {
        this.totalCount.set(0);
        this.successCount.set(0);
        this.failCount.set(0);
        this.status = TranStatus.INIT;
        this.startTime = 0;
    }

    @Override
    public void close() throws IOException {
        reset();
        this.context = new TranContext();
        this.exceptionConsumer = null;
        this.progressListener = null;
        log.info("GeoFileTran 处理器已关闭并重置");
    }
}
