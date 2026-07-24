package cn.geoair.map.dynamic.file.core.tran;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileReadException;
import cn.geoair.map.dynamic.file.core.exception.GeoFileTimeoutException;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.core.tran.model.TranContext;
import cn.geoair.map.dynamic.file.core.tran.model.TranProgress;
import cn.geoair.map.dynamic.file.core.tran.model.TranResult;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;

import org.geotools.api.feature.simple.SimpleFeatureType;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/** 改造后的 GeoFileTran 实现类 支持：上下文传递、进度监听、预处理/后处理、结构化结果、超时控制 */
public class GeoFileTranImpl implements GeoFileTran {

    private static GiLogger log = GirLoggerFactory.getLogger(GeoFileTranImpl.class);

    // 上下文
    private TranContext context = new TranContext();

    // 全局异常处理器
    private ExceptionConsumer exceptionConsumer;
    private Consumer<GirAdvOneRow> oneRowConsumer = girAdvOneRow -> {};

    private Consumer<SimpleFeatureType> headConsumer = simpleFeatureType -> {};

    // 进度监听器
    private TranProgressListener progressListener;

    // 进度统计
    private AtomicLong totalCount = new AtomicLong(0);

    private AtomicLong successCount = new AtomicLong(0);

    private AtomicLong failCount = new AtomicLong(0);
    // 要素总数
    long featureCount = 0L;

    // 状态控制
    private volatile TranStatus status = TranStatus.INIT;

    private long startTime;

    @Override
    public TranResult transform(GeoFileReader reader, GeoFileWriter writer) {
        return transform(reader, writer, this.context == null ? new TranContext() : this.context);
    }

    @Override
    public TranResult transform(GeoFileReader reader, GeoFileWriter writer, TranContext context) {
        reset();
        this.context = context == null ? new TranContext() : context;
        this.status = TranStatus.RUNNING;
        this.startTime = System.currentTimeMillis();
        TranResult result = new TranResult().setStatus(TranStatus.RUNNING).setStartTime(startTime);
        ExceptionConsumer internalExceptionConsumer = e -> {};

        try {
            if (reader == null || writer == null) {
                throw new IllegalArgumentException("读取器/写入器不能为空");
            }

            if (this.context.getPreProcessor() != null) {
                boolean continueFlag =
                        this.context.getPreProcessor().process(reader, writer, this.context);
                if (!continueFlag) {
                    status = TranStatus.ABORTED;
                    result.setErrorMsg("转换被终止");
                    return result;
                }
            }

            log.info("开始空间文件转换任务，上下文配置：{}", this.context);

            SimpleFeatureType featureType = reader.readHeader(internalExceptionConsumer);
            if (featureType == null) {
                throw new GeoFileReadException("读取表头失败，SimpleFeatureType 为空");
            }
            headConsumer.accept(featureType);
            writer.writeHeader(featureType, internalExceptionConsumer);
            log.info("表头初始化完成，要素类型：{}", featureType.getName());

            featureCount = reader.getFeatureCount();
            while (status == TranStatus.RUNNING) {
                checkTimeout();

                GirAdvOneRow oneRow;
                try {
                    oneRow = reader.readOneRow(internalExceptionConsumer);
                } catch (Exception e) {
                    totalCount.incrementAndGet();
                    onRecordFailure(result, e, "读取记录失败");
                    updateProgress();
                    logBatchProgress();
                    if (!this.context.isSkipErrorRecord()) {
                        break;
                    }
                    continue;
                }

                if (oneRow == null) {
                    break;
                }

                try {
                    oneRowConsumer.accept(oneRow);
                    writer.writeOneRow(oneRow, internalExceptionConsumer);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    onRecordFailure(result, e, "写入记录失败");
                    if (!this.context.isSkipErrorRecord()) {
                        totalCount.incrementAndGet();
                        updateProgress();
                        logBatchProgress();
                        break;
                    }
                }

                totalCount.incrementAndGet();
                updateProgress();
                logBatchProgress();
            }

            if (status == TranStatus.RUNNING) {
                status = TranStatus.SUCCESS;
            }
        } catch (GeoFileTimeoutException e) {
            status = TranStatus.TIMEOUT;
            result.setErrorMsg(e.getMessage());
            handleException(result, e);
        } catch (Exception e) {
            if (status != TranStatus.ABORTED && status != TranStatus.TIMEOUT) {
                status = TranStatus.FAILED;
            }
            if (result.getErrorMsg() == null) {
                result.setErrorMsg("转换任务执行异常");
            }
            handleException(result, e);
        } finally {
            if (this.context.isAutoCloseResource()) {
                closeQuietly(reader, result);
                closeQuietly(writer, result);
            }

            finalizeResult(result);
            log.info("转换任务执行完成，状态：{}，总耗时：{}ms", status, result.getElapsedTime());

            if (this.context.getPostProcessor() != null) {
                this.context.getPostProcessor().process(result, this.context);
            }

            updateProgress();
        }

        return result;
    }

    /** 超时检查 */
    private void checkTimeout() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - startTime > context.getTimeout()) {
            throw new GeoFileTimeoutException("转换任务超时（超时时间：" + context.getTimeout() + "ms）");
        }
    }

    /** 更新进度并回调监听器 */
    private void updateProgress() {
        if (progressListener == null) {
            return;
        }

        TranProgress progress =
                new TranProgress()
                        .setTotalFeatureCount(featureCount)
                        .setBatchTotalCount(totalCount.get())
                        .setBatchSuccessCount(successCount.get())
                        .setBatchFailCount(failCount.get())
                        .calculateSuccessRate()
                        .setElapsedTime(System.currentTimeMillis() - startTime)
                        .setStatus(status)
                        .setMessage(resolveProgressMessage());

        try {
            progressListener.onProgressUpdate(progress);
        } catch (Exception e) {
            log.error("进度监听器回调异常", e);
        }
    }

    /** 统一异常处理 */
    private void handleException(TranResult result, Exception e) {
        if (e == null) {
            return;
        }
        if (result != null && !containsException(result, e)) {
            result.getExceptions().add(e);
        }
        if (exceptionConsumer != null) {
            try {
                exceptionConsumer.accept(e);
            } catch (Exception consumerException) {
                log.error("全局异常处理器执行失败", consumerException);
            }
        } else {
            log.error("转换异常", e);
        }
    }

    /** 静默关闭资源 */
    private void closeQuietly(Closeable closeable, TranResult result) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
            log.info("资源 {} 关闭成功", closeable.getClass().getSimpleName());
        } catch (Exception e) {
            if (status == TranStatus.RUNNING || status == TranStatus.SUCCESS) {
                status = TranStatus.FAILED;
                if (result.getErrorMsg() == null || "转换成功".equals(result.getErrorMsg())) {
                    result.setErrorMsg("资源关闭失败");
                }
            }
            handleException(result, e instanceof Exception ? (Exception) e : new IOException(e));
            log.error("资源 {} 关闭失败", closeable.getClass().getSimpleName(), e);
        }
    }

    private void onRecordFailure(TranResult result, Exception e, String errorMsg) {
        failCount.incrementAndGet();
        if (result.getErrorMsg() == null) {
            result.setErrorMsg(errorMsg);
        }
        handleException(result, e);
        if (!context.isSkipErrorRecord()) {
            status = TranStatus.FAILED;
        }
    }

    private void logBatchProgress() {
        if (totalCount.get() == 0 || context.getBatchLogThreshold() <= 0) {
            return;
        }
        if (totalCount.get() % context.getBatchLogThreshold() == 0) {
            log.info(
                    "转换进度：已处理 {} 条，成功 {} 条，失败 {} 条，成功率 {}%",
                    totalCount.get(),
                    successCount.get(),
                    failCount.get(),
                    totalCount.get() == 0
                            ? 0.0
                            : (double) successCount.get() / totalCount.get() * 100);
        }
    }

    private void finalizeResult(TranResult result) {
        long endTime = System.currentTimeMillis();
        result.setStatus(status)
                .setTotalCount(totalCount.get())
                .setSuccessCount(successCount.get())
                .setFailCount(failCount.get())
                .setStartTime(startTime)
                .setEndTime(endTime)
                .setElapsedTime(endTime - startTime)
                .calculateSuccessRate();
        if (status == TranStatus.SUCCESS) {
            result.setErrorMsg(resolveResultMessage());
        } else if (result.getErrorMsg() == null) {
            result.setErrorMsg(resolveResultMessage());
        }
    }

    private boolean containsException(TranResult result, Exception target) {
        for (Throwable throwable : result.getExceptions()) {
            if (throwable == target) {
                return true;
            }
        }
        return false;
    }

    private String resolveProgressMessage() {
        switch (status) {
            case SUCCESS:
                return failCount.get() > 0 ? "转换完成，存在失败记录" : "转换成功";
            case FAILED:
                return "转换失败";
            case ABORTED:
                return "转换被终止";
            case TIMEOUT:
                return "转换超时";
            case RUNNING:
                return "转换中...";
            default:
                return "等待开始";
        }
    }

    private String resolveResultMessage() {
        switch (status) {
            case SUCCESS:
                return failCount.get() > 0 ? "转换完成，存在失败记录" : "转换成功";
            case FAILED:
                return "转换失败";
            case ABORTED:
                return "转换被终止";
            case TIMEOUT:
                return "转换超时";
            case RUNNING:
                return "转换中";
            default:
                return "转换未开始";
        }
    }

    // ========== 接口方法实现 ==========
    @Override
    public GeoFileTran setExceptionConsumer(ExceptionConsumer exceptionConsumer) {
        this.exceptionConsumer = exceptionConsumer;
        return this;
    }

    @Override
    public GeoFileTran setGirAdvOneRowConsumer(Consumer<GirAdvOneRow> oneRowConsumer) {
        this.oneRowConsumer = oneRowConsumer;
        return this;
    }

    @Override
    public GeoFileTran setHeadConsumer(Consumer<SimpleFeatureType> headConsumer) {
        this.headConsumer = headConsumer;
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
        this.featureCount = 0L;
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
