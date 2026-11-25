package cn.geoair.map.tile.forge.core.zip.cache;

import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.utils.CentralDirectoryUtils;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.ZipSourceIdentity;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/** 所有 ZIP 格式共用有界扫描和同步批次提交，成功意味着索引已完整落盘。 */
public final class ZipArchiveIndexBuilder {
    private static final ReentrantLock[] LOCKS = createLocks();

    private ZipArchiveIndexBuilder() {}

    private static ReentrantLock[] createLocks() {
        ReentrantLock[] locks = new ReentrantLock[64];
        for (int i = 0; i < locks.length; i++) locks[i] = new ReentrantLock();
        return locks;
    }

    public static void build(
            GirLayerConfigContext context,
            ZipDirectoryGetter getter,
            List<ProgressConsumer> progress) {
        try (LayerPerFileDao index = getter.getContextHelper().getLayerPerFileDao(context)) {
            // 同一实例内重复提交按索引文件串行复核，避免两个构建者互相清理和重复写入。
            ReentrantLock lock =
                    LOCKS[Math.floorMod(index.getTableName().hashCode(), LOCKS.length)];
            lock.lockInterruptibly();
            try {
                String sourceVersion =
                        ZipSourceIdentity.of(context, getter.getICompressionHandler());
                if (index.cacheEnableIs(context) && sourceVersion.equals(index.getArchiveVersion()))
                    return;
                try {
                    long batchSize = getter.getContextHelper().getLayerPerCacheBatchSize(context);
                    if (batchSize < 1 || batchSize > 100000)
                        throw new IllegalArgumentException("归档批大小须为 1 至 100000");
                    ICompressionHandler reader = getter.getICompressionHandler();
                    RootPathInfo root = getter.preCheckZipAndGetRoot(context, reader);
                    index.doPreCacheStart();
                    List<TileCentralDirectoryModel> batch = new ArrayList<>((int) batchSize);
                    AtomicLong total = new AtomicLong();
                    AtomicLong visited = new AtomicLong();
                    reader.scanAllEntries(
                            context.getObjectKey(),
                            (entry, all, current) -> {
                                if (Thread.currentThread().isInterrupted())
                                    throw new IllegalStateException("归档索引扫描已中断");
                                total.set(all);
                                visited.incrementAndGet();
                                if (!entry.isDirectoryIs()) {
                                    String name = entry.getName();
                                    String prefix = root.getRootPath();
                                    if (prefix != null && !prefix.isEmpty()) {
                                        if (!name.startsWith(prefix)) return true;
                                        entry.setName(name.substring(prefix.length()));
                                    }
                                    if (name.equals(root.getRootFilePath())
                                            && root.getRootFileStandardName() != null)
                                        entry.setName(root.getRootFileStandardName());
                                    TileCentralDirectoryModel model = getter.tranToTileModel(entry);
                                    if (model != null) {
                                        model.setFileName(entry.getName());
                                        batch.add(model);
                                    }
                                }
                                if (batch.size() >= batchSize) {
                                    flush(index, batch);
                                    report(progress, total.get(), visited.get());
                                }
                                return true;
                            });
                    if (visited.get() != total.get())
                        throw new IllegalStateException("归档扫描不完整，索引未发布");
                    flush(index, batch);
                    if (!sourceVersion.equals(ZipSourceIdentity.of(context, reader)))
                        throw new IllegalStateException("源归档在建立索引时发生变化，索引未发布");
                    index.setArchiveVersion(sourceVersion);
                    index.doPreCacheEnd();
                    report(progress, total.get(), visited.get());
                } catch (Exception e) {
                    try {
                        index.doPreCacheFailed();
                    } catch (Exception statusFailure) {
                        e.addSuppressed(statusFailure);
                    }
                    throw e;
                }
            } finally {
                lock.unlock();
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new IllegalStateException("归档索引构建失败", e);
        }
    }

    private static void flush(LayerPerFileDao index, List<TileCentralDirectoryModel> batch) {
        if (batch.isEmpty()) return;
        CentralDirectoryUtils.doInsert(batch, index);
        batch.clear();
    }

    private static void report(List<ProgressConsumer> listeners, long total, long visited) {
        if (listeners != null)
            for (ProgressConsumer listener : listeners) listener.report(total, visited);
    }
}
