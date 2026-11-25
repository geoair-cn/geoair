package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;

import java.util.Objects;

/** 严格预缓存模式：源瓦片缺失立即中止本张融合，避免把透明空图永久缓存。 */
public final class RequiredTileGetter implements LayerTileGetter {
    private final LayerTileGetter delegate;

    public RequiredTileGetter(LayerTileGetter delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public Resource getTileResource(int z, int x, int y) {
        TileOperationContext.checkCurrent();
        Resource resource = delegate.getTileResource(z, x, y);
        TileOperationContext.checkCurrent();
        if (resource == null || resource.getSize() == 0)
            throw new TileOperationException(
                    TileOperationStatus.MISSING, "源瓦片缺失：" + z + "/" + x + "/" + y);
        return resource;
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
