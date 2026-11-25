package cn.geoair.map.tile.forge.fuser.fuser;

import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.Grid;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationException;
import cn.geoair.map.tile.forge.fuser.execution.TileOperationStatus;
import cn.geoair.map.tile.forge.fuser.provider.SameGridTileReader;
import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import cn.geoair.map.tile.forge.fuser.utils.TileResourceLimits;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

/** 对已转换到同一输出矩阵的成员依次叠加，整张没有可见像素时才向父级回退。 */
public final class TileComposer {
    private static final double MERCATOR_RADIUS = 6378137;
    private static final GeometryFactory GEOMETRY = new GeometryFactory();

    private TileComposer() {}

    public static TileCompositionResult compose(
            GridSubset grid,
            int z,
            int x,
            int y,
            int size,
            List<TileCompositionMember> members,
            boolean nearestParent)
            throws IOException {
        int srid = SameGridTileReader.normalizeSrid(grid.getSRS().getNumber());
        if (srid != 3857 && srid != 4326) throw new IllegalArgumentException("组合支持墨卡托和经纬度矩阵");
        TileResourceLimits.validateImageDimensions(size, size);
        int tmsY = SameGridTileReader.tmsY(grid, srid, z, x, y);
        BoundingBox request = grid.boundsFromIndex(new long[] {x, tmsY, z});
        boolean mercator = srid == 3857;
        Envelope tile =
                mercator
                        ? new Envelope(
                                lon(request.getMinX()),
                                lon(request.getMaxX()),
                                lat(request.getMinY()),
                                lat(request.getMaxY()))
                        : new Envelope(
                                request.getMinX(),
                                request.getMaxX(),
                                request.getMinY(),
                                request.getMaxY());

        List<TileCompositionMember> eligible = new ArrayList<>();
        List<Path2D> clips = new ArrayList<>();
        int maxDepth = 0;
        for (TileCompositionMember member : members) {
            TileOperationContext.checkCurrent();
            if (!member.admits(z, tile, GEOMETRY)) continue;
            Path2D clip = member.mask(tile, mercator, size);
            if (clip != null && clip.getCurrentPoint() == null) continue;
            eligible.add(member);
            clips.add(clip);
            maxDepth = Math.max(maxDepth, member.fallbackDepth());
        }

        Exception failure = null;
        for (int depth = 0; depth <= Math.min(maxDepth, z - grid.getZoomStart()); depth++) {
            int readZoom = z - depth;
            Grid matrix = grid.getGridSet().getGrid(readZoom);
            BoundingBox first = grid.boundsFromIndex(new long[] {0, 0, readZoom});
            // 按实际矩阵定位父片，避免 4490 的第 0 到 1 级被当作固定四叉树。
            long px = (long) Math.floor((centerX(request) - first.getMinX()) / first.getWidth());
            long py = (long) Math.floor((centerY(request) - first.getMinY()) / first.getHeight());
            long[] index = {px, py, readZoom};
            if (!grid.covers(index)) continue;
            BoundingBox parent = grid.boundsFromIndex(index);
            if (!contains(parent, request)) continue;
            BufferedImage canvas = null;
            try {
                for (int i = 0; i < eligible.size(); i++) {
                    TileOperationContext.checkCurrent();
                    TileCompositionMember member = eligible.get(i);
                    if (!member.reads(readZoom, depth)) continue;
                    try {
                        TileFusionResult result =
                                member.read(
                                        readZoom,
                                        Math.toIntExact(px),
                                        Math.toIntExact(matrix.getNumTilesHigh() - py - 1));
                        if (result.getState() == TileFusionState.NO_COVERAGE) continue;
                        if (result.getState() == TileFusionState.FAILED)
                            throw new IOException("组合成员读取失败", result.getFailure());
                        if (result.getState() == TileFusionState.PARTIAL && failure == null)
                            failure = new IOException("组合成员部分瓦片读取失败");
                        if (canvas == null)
                            canvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                        draw(
                                canvas,
                                result.getBytes(),
                                clips.get(i),
                                request,
                                parent,
                                nearestParent && depth > 0);
                    } catch (RuntimeException | IOException problem) {
                        if (problem instanceof TileOperationException
                                && ((TileOperationException) problem).getStatus()
                                        == TileOperationStatus.CANCELLED)
                            throw (TileOperationException) problem;
                        TileOperationContext.checkCurrent();
                        if (failure == null) failure = problem;
                    }
                }
                if (canvas != null && visible(canvas)) {
                    return new TileCompositionResult(
                            failure == null ? TileFusionState.COMPLETE : TileFusionState.PARTIAL,
                            TileImageUtils.writeImage(canvas, "png"),
                            readZoom,
                            failure);
                }
            } finally {
                if (canvas != null) canvas.flush();
            }
        }
        return new TileCompositionResult(
                failure == null ? TileFusionState.NO_COVERAGE : TileFusionState.FAILED,
                null,
                -1,
                failure);
    }

    private static void draw(
            BufferedImage canvas,
            byte[] bytes,
            Path2D clip,
            BoundingBox request,
            BoundingBox parent,
            boolean nearest)
            throws IOException {
        BufferedImage image = TileImageUtils.readImage(bytes);
        if (image == null) throw new IOException("组合成员不是有效图片");
        Graphics2D graphics = canvas.createGraphics();
        try {
            if (clip != null) graphics.clip(clip);
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    nearest
                            ? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
                            : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            double scaleX = canvas.getWidth() / request.getWidth();
            double scaleY = canvas.getHeight() / request.getHeight();
            graphics.drawImage(
                    image,
                    new AffineTransform(
                            scaleX * parent.getWidth() / image.getWidth(),
                            0,
                            0,
                            scaleY * parent.getHeight() / image.getHeight(),
                            scaleX * (parent.getMinX() - request.getMinX()),
                            scaleY * (request.getMaxY() - parent.getMaxY())),
                    null);
        } finally {
            graphics.dispose();
            image.flush();
        }
    }

    private static boolean visible(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++)
                if ((image.getRGB(x, y) >>> 24) != 0) return true;
        return false;
    }

    private static boolean contains(BoundingBox parent, BoundingBox tile) {
        double tolerance = 1e-9 * Math.max(parent.getWidth(), parent.getHeight());
        return parent.getMinX() <= tile.getMinX() + tolerance
                && parent.getMinY() <= tile.getMinY() + tolerance
                && parent.getMaxX() >= tile.getMaxX() - tolerance
                && parent.getMaxY() >= tile.getMaxY() - tolerance;
    }

    private static double centerX(BoundingBox bounds) {
        return bounds.getMinX() + bounds.getWidth() / 2;
    }

    private static double centerY(BoundingBox bounds) {
        return bounds.getMinY() + bounds.getHeight() / 2;
    }

    private static double lon(double x) {
        return Math.toDegrees(x / MERCATOR_RADIUS);
    }

    private static double lat(double y) {
        return Math.toDegrees(Math.atan(Math.sinh(y / MERCATOR_RADIUS)));
    }
}
