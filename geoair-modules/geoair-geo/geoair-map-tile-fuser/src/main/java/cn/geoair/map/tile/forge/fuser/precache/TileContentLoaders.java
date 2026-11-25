package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.io.Resource;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationScope;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExec;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.RequiredTileGetter;

import java.util.Objects;

/** 原始网格与融合输出的标准加载器；由分段执行器统一写入目标缓存。 */
public final class TileContentLoaders {
    private TileContentLoaders() {}

    /** 行号按 LayerTileGetter 契约传入（当前内置 Getter 使用 TMS）；不得重复翻转。 */
    public static TileContentLoader original(LayerTileGetter getter) {
        RequiredTileGetter required = new RequiredTileGetter(getter);
        return (z, x, y, context) -> {
            try (TileOperationScope ignored = context.bind()) {
                Resource resource = required.getTileResource(z, x, y);
                byte[] data = resource.getByteData();
                context.check();
                return data;
            }
        };
    }

    /** 任意一张依赖源片缺失，都不写入不完整的融合结果。 */
    public static TileContentLoader fused(
            LayerTileGetter getter,
            ImageMime outputFormat,
            TileBoundsResolver bounds,
            int width,
            int height) {
        RequiredTileGetter required = new RequiredTileGetter(getter);
        Objects.requireNonNull(bounds);
        Objects.requireNonNull(outputFormat);
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("输出尺寸必须大于零");
        return (z, x, y, context) -> {
            try (TileOperationScope ignored = context.bind()) {
                context.check();
                return new GirFuserExec(
                                required, outputFormat, bounds.resolve(z, x, y), width, height)
                        .toImageBytes();
            }
        };
    }
}
