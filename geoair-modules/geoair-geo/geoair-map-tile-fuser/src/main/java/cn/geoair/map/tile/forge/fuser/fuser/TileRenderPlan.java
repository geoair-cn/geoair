package cn.geoair.map.tile.forge.fuser.fuser;

import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;

import lombok.Getter;

/** 与融合器共用的源分辨率、范围和实际画布计算，不读取源瓦片。 */
@Getter
public final class TileRenderPlan {
    private static final double RESOLUTION_MARGIN = 1.005;
    private final int sourceZoom;
    private final double sourceResolution;
    private final int canvasWidth;
    private final int canvasHeight;
    private final double scaleX;
    private final double scaleY;
    private final long[] sourceRectangle;

    private TileRenderPlan(
            int zoom,
            double resolution,
            int width,
            int height,
            int outputWidth,
            int outputHeight,
            long[] rectangle) {
        sourceZoom = zoom;
        sourceResolution = resolution;
        canvasWidth = width;
        canvasHeight = height;
        scaleX = (double) outputWidth / width;
        scaleY = (double) outputHeight / height;
        sourceRectangle = rectangle.clone();
    }

    public long[] getSourceRectangle() {
        return sourceRectangle.clone();
    }

    public static TileRenderPlan calculate(
            GridSubset grid, BoundingBox bounds, int width, int height) {
        if (grid == null
                || bounds == null
                || width <= 0
                || height <= 0
                || !Double.isFinite(bounds.getWidth())
                || !Double.isFinite(bounds.getHeight())
                || bounds.getWidth() <= 0
                || bounds.getHeight() <= 0) throw new IllegalArgumentException("网格、范围及输出尺寸无效");
        double[] resolutions = grid.getResolutions();
        if (resolutions == null || resolutions.length == 0)
            throw new IllegalArgumentException("源网格没有分辨率");
        double target =
                RESOLUTION_MARGIN
                        * Math.min(bounds.getWidth() / width, bounds.getHeight() / height);
        int zoom = resolutions.length - 1;
        for (int i = 0; i < resolutions.length; i++) {
            if (!Double.isFinite(resolutions[i]) || resolutions[i] <= 0)
                throw new IllegalArgumentException("源网格分辨率无效");
            if (resolutions[i] < target) {
                zoom = i;
                break;
            }
        }
        double resolution = resolutions[zoom];
        long cw = Math.round(bounds.getWidth() / resolution),
                ch = Math.round(bounds.getHeight() / resolution);
        if (cw <= 0 || ch <= 0 || cw > Integer.MAX_VALUE || ch > Integer.MAX_VALUE)
            throw new IllegalArgumentException("融合画布尺寸超出支持范围");
        long[] rectangle = grid.getCoverageIntersection(zoom, bounds);
        return new TileRenderPlan(zoom, resolution, (int) cw, (int) ch, width, height, rectangle);
    }
}
