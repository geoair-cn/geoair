package cn.geoair.map.tile.forge.fuser.support;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import java.util.function.BiFunction;
import java.util.concurrent.atomic.AtomicInteger;

/** 行列可编程的源，记录真实调用以验证直出不改变级别和行号。 */
public final class FusionTestSource implements LayerTileGetter {
    private final GridSubset grid;
    private final BiFunction<Integer, Integer, Resource> reader;
    public final AtomicInteger calls = new AtomicInteger();
    public volatile int lastZ, lastX, lastY;
    public FusionTestSource(GridSubset grid, BiFunction<Integer, Integer, Resource> reader) {
        this.grid = grid; this.reader = reader;
    }
    @Override public Resource getTileResource(int z, int x, int y) {
        calls.incrementAndGet(); lastZ = z; lastX = x; lastY = y;
        return reader.apply(x, y);
    }
    @Override public ImageMime getSrcFormat() { return ImageMime.png; }
    @Override public GridSubset getSrcGridSubset() { return grid; }
}
