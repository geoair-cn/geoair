package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.GirTileConverterOpt;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import java.util.BitSet;
import java.util.Objects;

/** 每段仅保存位图；整块在内/在外直接判定，边界块递归细分。 */
public class TileRegionMask {
    private final PreparedGeometry region;
    private final int zoom;
    private final GirTileConverterOpt grid;
    private final TileYAxis yAxis;
    private final Runnable checkStop;

    public TileRegionMask(Geometry geometry, int zoom, GirTileConverterOpt grid, TileYAxis yAxis) {
        this(
                geometry,
                zoom,
                grid,
                yAxis,
                () -> {
                    if (Thread.currentThread().isInterrupted())
                        throw new IllegalStateException("任务执行中断");
                });
    }

    public TileRegionMask(
            Geometry geometry,
            int zoom,
            GirTileConverterOpt grid,
            TileYAxis yAxis,
            Runnable checkStop) {
        if (geometry == null
                || geometry.isEmpty()
                || !(geometry instanceof Polygon || geometry instanceof MultiPolygon)
                || !geometry.isValid()) throw new IllegalArgumentException("预缓存范围必须是有效的面或多面");
        this.region = PreparedGeometryFactory.prepare(geometry.copy());
        this.zoom = zoom;
        this.grid = Objects.requireNonNull(grid);
        this.yAxis = Objects.requireNonNull(yAxis);
        this.checkStop = checkStop;
    }

    public BitSet classify(RangeApo range, long rows, long from, long to) {
        if (rows != (long) range.getMaxY() - range.getMinY() + 1
                || rows <= 0
                || from < 0
                || to < from
                || to - from > Integer.MAX_VALUE
                || to > rows * ((long) range.getMaxX() - range.getMinX() + 1))
            throw new IllegalArgumentException("无效的瓦片分段范围");
        BitSet result = new BitSet((int) (to - from));
        for (long index = from; index < to; ) {
            checkStop.run();
            int count = (int) Math.min(to - index, rows - index % rows);
            classifyColumn(
                    (int) (range.getMinX() + index / rows),
                    (int) (range.getMinY() + index % rows),
                    count,
                    (int) (index - from),
                    result);
            index += count;
        }
        return result;
    }

    private void classifyColumn(int x, int y, int count, int offset, BitSet result) {
        TileOperationContext.checkCurrent();
        checkStop.run();
        Envelope envelope = tile(x, y).getEnvelopeInternal();
        if (count > 1) envelope.expandToInclude(tile(x, y + count - 1).getEnvelopeInternal());
        Geometry block = region.getGeometry().getFactory().toGeometry(envelope);
        // 只对穿过边界的块二分，孔洞也属于范围外；相交一张瓦片时保留原有触边语义。
        if (!region.intersects(block)) return;
        if (count == 1 || region.covers(block)) {
            result.set(offset, offset + count);
            return;
        }
        int half = count / 2;
        classifyColumn(x, y, half, offset, result);
        classifyColumn(x, y + half, count - half, offset + half, result);
    }

    private Geometry tile(int x, int y) {
        BoxReferencedEnvelope box = grid.xyzToTileBox(zoom, x, y, yAxis, 4326);
        return GirAdvTools.getFormatOpt().wktToJtsGeometry(box.getWktString(4326));
    }
}
