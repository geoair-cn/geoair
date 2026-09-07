package cn.geoair.map.dynamic.file.core.transfer;

import cn.geoair.base.data.page.support.GirPageParam;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.enums.TranStatus;
import cn.geoair.map.dynamic.file.core.read.GeoFileReader;
import cn.geoair.map.dynamic.file.core.tran.model.TranProgress;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Core V2 的无状态空间数据传输引擎。
 * <p>
 * 引擎只负责读取、批处理、错误策略、进度和资源关闭；数据库方言、文件格式和建表细节
 * 均由 Reader / Writer 实现承担。每次调用使用独立请求对象，因此同一个引擎实例可安全复用。
 *
 * @author 张逢吉
 */
public final class GeoTransferEngine {

    /** 创建一个无状态传输引擎。 */
    public static GeoTransferEngine create() {
        return new GeoTransferEngine();
    }

    private GeoTransferEngine() {
    }

    /** 执行一次空间数据传输。 */
    public GeoTransferResult execute(GeoTransferRequest request) {
        validate(request);
        GeoTransferResult result = new GeoTransferResult().setStatus(TranStatus.RUNNING);
        long start = System.currentTimeMillis();
        result.setStartTime(start);
        GeoTransferOptions options = request.getOptions();
        GeoFileReader reader = request.getReader();
        GeoFileWriter writer = request.getWriter();
        try {
            writer.setWriteConfig(request.getWriteConfig());
            writer.writeHeader(reader.readHeader(null), null);
            long count = reader.getFeatureCount();
            if (count > 0) {
                transferByPage(reader, writer, count, options, request.getProgressListener(), result, start);
            } else {
                transferByCursor(reader, writer, options, request.getProgressListener(), result, start);
            }
            result.setStatus(TranStatus.SUCCESS);
        } catch (Exception e) {
            result.getExceptions().add(e);
            result.setErrorMessage(e.getMessage());
            result.setStatus(TranStatus.FAILED);
        } finally {
            if (options.isAutoCloseResource()) {
                closeQuietly(reader, result);
                closeQuietly(writer, result);
            }
            long end = System.currentTimeMillis();
            result.setEndTime(end).setElapsedTime(end - start);
        }
        return result;
    }

    private void transferByPage(
            GeoFileReader reader, GeoFileWriter writer, long expectedCount, GeoTransferOptions options,
            GeoTransferProgressListener listener, GeoTransferResult result, long start) {
        int pageNum = 1;
        while (result.getTotalCount() < expectedCount) {
            checkTimeout(options, start);
            GirPageParam page = new GirPageParam();
            page.putParam(options.getBatchSize(), pageNum++, 0L, false);
            GirPager<GirAdvOneRow> pager = reader.readRowPage(page, null);
            List<GirAdvOneRow> rows = pager == null ? null : (List<GirAdvOneRow>) pager.getList();
            if (rows == null || rows.isEmpty()) {
                break;
            }
            writeBatch(writer, rows, options, result);
            result.setTotalCount(result.getTotalCount() + rows.size());
            notifyProgress(listener, result, expectedCount, start);
        }
    }

    private void transferByCursor(
            GeoFileReader reader, GeoFileWriter writer, GeoTransferOptions options,
            GeoTransferProgressListener listener, GeoTransferResult result, long start) {
        Iterator<GirAdvOneRow> iterator = reader.readRowIterator(null);
        List<GirAdvOneRow> batch = new ArrayList<>(options.getBatchSize());
        while (iterator != null && iterator.hasNext()) {
            checkTimeout(options, start);
            batch.add(iterator.next());
            if (batch.size() >= options.getBatchSize()) {
                flushCursorBatch(writer, batch, options, listener, result, start);
            }
        }
        flushCursorBatch(writer, batch, options, listener, result, start);
    }

    private void flushCursorBatch(
            GeoFileWriter writer, List<GirAdvOneRow> batch, GeoTransferOptions options,
            GeoTransferProgressListener listener, GeoTransferResult result, long start) {
        if (batch.isEmpty()) {
            return;
        }
        writeBatch(writer, batch, options, result);
        result.setTotalCount(result.getTotalCount() + batch.size());
        notifyProgress(listener, result, -1, start);
        batch.clear();
    }

    private void writeBatch(
            GeoFileWriter writer, List<GirAdvOneRow> rows, GeoTransferOptions options, GeoTransferResult result) {
        try {
            writer.writeRows(rows, null);
            result.setSuccessCount(result.getSuccessCount() + rows.size());
        } catch (Exception batchError) {
            if (options.getErrorPolicy() == ErrorPolicy.FAIL_FAST) {
                throw new IllegalStateException("批量写入失败", batchError);
            }
            if (options.getErrorPolicy() == ErrorPolicy.SKIP_BATCH) {
                result.setFailCount(result.getFailCount() + rows.size());
                result.getExceptions().add(batchError);
                return;
            }
            for (GirAdvOneRow row : rows) {
                try {
                    writer.writeOneRow(row, null);
                    result.setSuccessCount(result.getSuccessCount() + 1);
                } catch (Exception rowError) {
                    result.setFailCount(result.getFailCount() + 1);
                    result.getExceptions().add(rowError);
                }
            }
        }
    }

    private void notifyProgress(
            GeoTransferProgressListener listener, GeoTransferResult result, long expectedCount, long start) {
        if (listener == null) {
            return;
        }
        TranProgress progress = new TranProgress()
                .setTotalFeatureCount(expectedCount)
                .setBatchTotalCount(result.getTotalCount())
                .setBatchSuccessCount(result.getSuccessCount())
                .setBatchFailCount(result.getFailCount())
                .setElapsedTime(System.currentTimeMillis() - start)
                .setStatus(TranStatus.RUNNING)
                .calculateSuccessRate();
        listener.onProgress(progress);
    }

    private void checkTimeout(GeoTransferOptions options, long start) {
        if (options.getTimeoutMillis() > 0 && System.currentTimeMillis() - start > options.getTimeoutMillis()) {
            throw new IllegalStateException("空间数据传输超时：" + options.getTimeoutMillis() + "ms");
        }
    }

    private void closeQuietly(java.io.Closeable closeable, GeoTransferResult result) {
        try {
            closeable.close();
        } catch (Exception e) {
            result.getExceptions().add(e);
            if (result.getStatus() == TranStatus.SUCCESS) {
                result.setStatus(TranStatus.FAILED).setErrorMessage("资源关闭失败：" + e.getMessage());
            }
        }
    }

    private void validate(GeoTransferRequest request) {
        if (request == null || request.getReader() == null || request.getWriter() == null) {
            throw new IllegalArgumentException("GeoTransferRequest、reader 和 writer 不能为空");
        }
        if (request.getOptions() == null || request.getOptions().getBatchSize() <= 0) {
            throw new IllegalArgumentException("batchSize 必须大于 0");
        }
    }
}
