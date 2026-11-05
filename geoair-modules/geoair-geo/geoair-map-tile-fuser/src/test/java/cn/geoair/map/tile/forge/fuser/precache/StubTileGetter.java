package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;

/** 本地融合测试的内存源，不访问网络。 */
final class StubTileGetter implements LayerTileGetter {
    private final byte[] data;

    StubTileGetter(byte[] data) {
        this.data = data;
    }

    @Override
    public Resource getTileResource(int z, int x, int y) {
        return data == null ? null : new ByteArrayResource(data);
    }

    @Override
    public ImageMime getSrcFormat() {
        return ImageMime.png;
    }

    @Override
    public GridSubset getSrcGridSubset() {
        return GridInitUtils.getWorldGrid3857();
    }
}
