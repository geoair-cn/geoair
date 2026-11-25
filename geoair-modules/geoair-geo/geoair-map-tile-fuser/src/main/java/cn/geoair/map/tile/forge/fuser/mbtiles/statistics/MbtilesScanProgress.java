package cn.geoair.map.tile.forge.fuser.mbtiles.statistics;

import lombok.Getter;
import java.util.List;

/** 侧车记录的是某次扫描快照；STALE / INTERRUPTED 不能作为当前精确结果。 */
@Getter
public final class MbtilesScanProgress {
    private List<CacheZoomStatistics> coordinateOverview;
    public void setCoordinateOverview(List<CacheZoomStatistics> value) { coordinateOverview = value; }
    private final String state;
    public MbtilesScanState getScanState() { return MbtilesScanState.valueOf(state); }
    private final long scannedCount;
    private final long lastRowId;
    private final Integer currentZoom;
    private final String fingerprint;
    private final List<CacheZoomStatistics> levels;
    public MbtilesScanProgress(String state,long scannedCount,long lastRowId,Integer currentZoom,
                               String fingerprint,List<CacheZoomStatistics> levels) {
        this.state=state;this.scannedCount=scannedCount;this.lastRowId=lastRowId;
        this.currentZoom=currentZoom;this.fingerprint=fingerprint;this.levels=levels;
    }
}
