package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.EocdInfo;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** 分块读取/解析与批量入库并行，队列满时生产端等待，避免全量目录堆积在内存中。 */
final class ZipDirectoryPipeline {
    private ZipDirectoryPipeline() { }

    static void scan(AbstractZipCompressionHandler handler, EocdInfo eocd, String source,
                     TerminatingConsumer<CentralDirectoryModel> consumer) throws IOException {
        BlockingQueue<CentralDirectoryModel> queue = new ArrayBlockingQueue<>(512);
        AtomicBoolean stopped = new AtomicBoolean();
        FutureTask<Void> producer = new FutureTask<>(() -> {
            handler.scanDirectoryEntries(eocd, source, (entry, total, index) -> {
                if (stopped.get()) return false;
                try {
                    queue.put(entry);
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CancellationException("ZIP中央目录生产已取消");
                }
            });
            return null;
        });
        Thread worker = new Thread(producer, "zip-directory-producer");
        worker.setDaemon(true);
        worker.start();
        try {
            long index = 0;
            while (true) {
                if (producer.isDone()) {
                    // 先检查生产异常，不能将截断或读取失败当作正常完成。
                    producer.get();
                    if (queue.isEmpty()) return;
                }
                CentralDirectoryModel entry = queue.poll(100, TimeUnit.MILLISECONDS);
                if (entry != null && !consumer.accept(entry, eocd.getTotalEntries(), index++)) return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("ZIP中央目录消费已中断");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new IOException("ZIP中央目录生产失败", cause);
        } finally {
            stopped.set(true);
            producer.cancel(true);
            // 清理期间临时取走中断状态，使 join 能等待生产线程退出，随后恢复。
            boolean interrupted = Thread.interrupted();
            try {
                worker.join(5000);
                if (worker.isAlive()) {
                    handler.log.warn("ZIP目录生产线程尚未退出，等待底层范围读取超时：{}", source);
                }
            } catch (InterruptedException e) {
                interrupted = true;
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
                queue.clear();
            }
        }
    }
}
