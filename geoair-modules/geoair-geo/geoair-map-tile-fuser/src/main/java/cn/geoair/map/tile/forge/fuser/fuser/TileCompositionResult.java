package cn.geoair.map.tile.forge.fuser.fuser;

import lombok.Getter;

/** 瓦片组输出和实际读取级别。确认无覆盖与临时故障分别报告。 */
@Getter
public final class TileCompositionResult {
    private final TileFusionState state;
    private final byte[] png;
    private final int readZoom;
    private final Exception failure;

    TileCompositionResult(TileFusionState state, byte[] png, int readZoom, Exception failure) {
        this.state = state;
        this.png = png;
        this.readZoom = readZoom;
        this.failure = failure;
    }

    public byte[] getPng() { return png == null ? null : png.clone(); }

    public boolean isCacheable() { return state == TileFusionState.COMPLETE || state == TileFusionState.NO_COVERAGE; }
}
