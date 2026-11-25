package cn.geoair.map.tile.forge.fuser.fuser;

import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

import java.awt.geom.Path2D;
import java.util.Objects;
import java.util.function.Function;

/** 组合成员的请求级别、可读级别、回退限制及经纬度覆盖。读取器接收输出矩阵 XYZ。 */
public final class TileCompositionMember {
    private final int minRequestZoom;
    private final int maxRequestZoom;
    private final int minReadZoom;
    private final int maxReadZoom;
    private final int fallbackDepth;
    private final boolean outside;
    private final Envelope envelope;
    private final PreparedGeometry coverage;
    private final TileBoundaryMask boundary;
    private final Function<TileZxyApo, TileFusionResult> reader;

    public TileCompositionMember(
            int minRequestZoom,
            int maxRequestZoom,
            int minReadZoom,
            int maxReadZoom,
            int fallbackDepth,
            Geometry coverageLonLat,
            boolean outside,
            boolean clipBoundary,
            Function<TileZxyApo, TileFusionResult> reader) {
        if (minRequestZoom < 0
                || minReadZoom < 0
                || maxRequestZoom < minRequestZoom
                || maxReadZoom < minReadZoom
                || fallbackDepth < 0) throw new IllegalArgumentException("组合成员级别范围无效");
        if (coverageLonLat != null
                && (!(coverageLonLat instanceof Polygonal)
                        || coverageLonLat.isEmpty()
                        || !coverageLonLat.isValid()))
            throw new IllegalArgumentException("覆盖范围必须为有效的面或多面");
        this.minRequestZoom = minRequestZoom;
        this.maxRequestZoom = maxRequestZoom;
        this.minReadZoom = minReadZoom;
        this.maxReadZoom = maxReadZoom;
        this.fallbackDepth = fallbackDepth;
        this.outside = outside;
        Geometry shape = coverageLonLat == null ? null : coverageLonLat.copy();
        envelope = shape == null ? null : shape.getEnvelopeInternal();
        coverage = shape == null ? null : PreparedGeometryFactory.prepare(shape);
        boundary = clipBoundary && shape != null ? new TileBoundaryMask(shape) : null;
        this.reader = Objects.requireNonNull(reader, "组合成员读取器不能为空");
    }

    boolean admits(int zoom, Envelope tile, GeometryFactory factory) {
        if (zoom < minRequestZoom || zoom > maxRequestZoom) return false;
        if (coverage == null) return true;
        if (!envelope.intersects(tile)) return outside;
        Geometry rectangle = factory.toGeometry(tile);
        return outside ? !coverage.covers(rectangle) : coverage.intersects(rectangle);
    }

    Path2D mask(Envelope tile, boolean mercator, int size) {
        return boundary == null ? null : boundary.mask(tile, mercator, size, outside);
    }

    boolean reads(int zoom, int depth) {
        return depth <= fallbackDepth && zoom >= minReadZoom && zoom <= maxReadZoom;
    }

    int fallbackDepth() {
        return fallbackDepth;
    }

    TileFusionResult read(int z, int x, int y) {
        return Objects.requireNonNull(
                reader.apply(TileZxyApo.of().setZ(z).setX(x).setY(y)), "组合成员必须返回读取结果");
    }
}
