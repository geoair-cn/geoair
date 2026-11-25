package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.fuser.TileFusionResult;
import java.util.concurrent.atomic.AtomicLong;

/** 每次融合独享一个记录器；并行读取时也能区分缺片、无覆盖和读取失败。 */
public final class CheckedLayerTileGetter implements LayerTileGetter {
    private final LayerTileGetter delegate;
    private final MissingTileMeaning missingMeaning;
    private final TileReadFailurePolicy failurePolicy;
    private final AtomicLong available = new AtomicLong();
    private final AtomicLong noCoverage = new AtomicLong();
    private final AtomicLong missing = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public CheckedLayerTileGetter(
            LayerTileGetter delegate,
            MissingTileMeaning missingMeaning,
            TileReadFailurePolicy failurePolicy) {
        if (delegate == null || missingMeaning == null || failurePolicy == null)
            throw new IllegalArgumentException("读取器及读取策略不能为空");
        this.delegate = delegate;
        this.missingMeaning = missingMeaning;
        this.failurePolicy = failurePolicy;
    }

    @Override
    public Resource getTileResource(int z, int x, int y) {
        TileOperationContext.checkCurrent();
        try {
            Resource resource = delegate.getTileResource(z, x, y);
            if (resource == null) {
                if (missingMeaning == MissingTileMeaning.NO_COVERAGE) noCoverage.incrementAndGet();
                else missing.incrementAndGet();
            } else available.incrementAndGet();
            return resource;
        } catch (TileOperationException stopped) {
            // 超时和主动取消不能伪装为普通缺片继续执行。
            if (stopped.getStatus() == TileOperationStatus.CANCELLED
                    || stopped.getStatus() == TileOperationStatus.TIMED_OUT) throw stopped;
            failed.incrementAndGet();
            if (failurePolicy == TileReadFailurePolicy.FAIL_FAST) throw stopped;
            return null;
        } catch (RuntimeException failure) {
            failed.incrementAndGet();
            if (failurePolicy == TileReadFailurePolicy.FAIL_FAST
                    || Thread.currentThread().isInterrupted()) throw failure;
            return null;
        }
    }

    /** 已读到数据但无法解码时，把该片从可用计数移到失败计数。 */
    public void recordDecodeFailure() {
        available.decrementAndGet();
        failed.incrementAndGet();
    }

    public TileFusionResult result(byte[] bytes, Exception failure) {
        long valid = available.get();
        // 全部源片不可用时，融合器生成的透明画布不代表成功。
        return new TileFusionResult(
                valid == 0 ? null : bytes,
                valid,
                noCoverage.get(),
                missing.get(),
                failed.get(),
                failure);
    }

    @Override
    public ImageMime getSrcFormat() {
        return delegate.getSrcFormat();
    }

    @Override
    public GridSubset getSrcGridSubset() {
        return delegate.getSrcGridSubset();
    }
}
