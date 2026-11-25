package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.encoding.TileImageCodec;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;

import java.io.IOException;

/** 显式适配融合器所需图片格式；不修改源字节或旧Getter的默认行为。 */
public final class FormattedLayerTileGetter implements LayerTileGetter {
    private final LayerTileGetter source;
    private final ImageMime format;

    public FormattedLayerTileGetter(LayerTileGetter source, ImageMime format) {
        if (source == null || format == null) throw new IllegalArgumentException("源读取器和响应格式不能为空");
        this.source = source;
        this.format = format;
    }

    @Override
    public Resource getTileResource(int z, int x, int y) {
        TileOperationContext.checkCurrent();
        Resource resource = source.getTileResource(z, x, y);
        if (resource == null) return null;
        try {
            return new ByteArrayResource(
                    TileImageCodec.normalize(resource.getByteData(), format.getInternalName()));
        } catch (IOException failure) {
            throw new IllegalStateException("瓦片格式适配失败", failure);
        }
    }

    @Override
    public ImageMime getSrcFormat() {
        return format;
    }

    @Override
    public GridSubset getSrcGridSubset() {
        return source.getSrcGridSubset();
    }

    public LayerTileGetter getSource() {
        return source;
    }
}
