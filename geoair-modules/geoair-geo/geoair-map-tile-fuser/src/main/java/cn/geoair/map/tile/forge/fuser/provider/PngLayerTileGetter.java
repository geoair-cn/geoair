package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.image.TileImageFormats;
import java.io.IOException;

/** 用于独立结果发布的 PNG 适配器，不回写原始数据。 */
public final class PngLayerTileGetter implements LayerTileGetter {
    private final LayerTileGetter source;
    public PngLayerTileGetter(LayerTileGetter source) {
        if (source == null) throw new IllegalArgumentException("源读取器不能为空");
        this.source = source;
    }
    @Override public Resource getTileResource(int z, int x, int y) {
        Resource resource = source.getTileResource(z, x, y);
        if (resource == null) return null;
        try { return new ByteArrayResource(TileImageFormats.toPng(resource.getByteData())); }
        catch (IOException failure) { throw new IllegalStateException("瓦片图片格式转换失败", failure); }
    }
    @Override public ImageMime getSrcFormat() { return ImageMime.png; }
    @Override public GridSubset getSrcGridSubset() { return source.getSrcGridSubset(); }
}
