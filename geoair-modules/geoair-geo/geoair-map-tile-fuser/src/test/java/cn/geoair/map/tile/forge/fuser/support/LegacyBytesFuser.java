package cn.geoair.map.tile.forge.fuser.support;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExec;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;

/** 模拟旧版只覆盖字节入口的第三方子类。 */
public final class LegacyBytesFuser extends GirFuserExec {
    private final byte[] bytes;
    private final boolean fail;
    public LegacyBytesFuser(byte[] bytes, boolean fail) {
        super(new FusionTestSource(GridInitUtils.getWorldGrid3857(), (x, y) -> null),
                ImageMime.png, BoundingBox.WORLD3857, 256, 256);
        this.bytes = bytes; this.fail = fail;
    }
    @Override public byte[] toImageBytes() {
        if (fail) throw new IllegalStateException("旧版融合器读取失败");
        return bytes == null ? null : bytes.clone();
    }
}
