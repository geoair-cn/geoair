package cn.geoair.map.dynamic.tools.grid.dto;

import lombok.Data;
import org.locationtech.jts.geom.Envelope;

/**
 * 单个瓦片层级的网格与显示参数。
 *
 * <p>数值单位取决于 {@link #gridSetName}：EPSG:3857 通常使用米，EPSG:4326 使用度。 因而 {@link #tileSizeM}、{@link
 * #groundResolution} 和 {@link #resolution} 的名称为 历史 API，不能一律理解为米制值。
 *
 * @author 张逢吉
 */
@Data
public class TileLevelMetadata {

    /** 缩放级别（0~maxZoom） */
    private final int zoom;

    /** 每行的瓦片数量（水平方向/X轴） */
    private final int numTilesWide;

    /** 每列的瓦片数量（垂直方向/Y轴） */
    private final int numTilesHigh;

    /** 每个瓦片在当前网格坐标单位下的横向跨度。 */
    private final double tileSizeM;

    /** 每像素在当前网格坐标单位下的跨度。 */
    private final double groundResolution;

    /** 网格分辨率；3857 通常为米/像素，4326 通常为度/像素。 */
    private final double resolution;

    /** 比例尺（如 1:scale），表示地图上的1单位长度对应的实际地面长度 */
    private final double scale;

    /** 当前缩放级别的总瓦片数量，计算公式：numTilesWide * numTilesHigh */
    private final long totalTiles;

    /** 瓦片像素尺寸（通常为 256 或 512） */
    private final int tilePixelSize;

    /** 屏幕 DPI（每英寸像素数，通常为 90.714 或 96） */
    private final double dpi;

    /** 每像素代表的毫米数，计算公式：25.4 / dpi */
    private final double mmPerPixel;

    /** 当前网格坐标参考系中的全局范围。 */
    private final Envelope extent;

    /** 网格集标识（例如 {@code EPSG:4326} 或 {@code EPSG:3857}）。 */
    private final String gridSetName;

    public TileLevelMetadata(
            int zoom,
            int numTilesWide,
            int numTilesHigh,
            double tileSizeM,
            double groundResolution,
            double resolution,
            double scale,
            long totalTiles,
            int tilePixelSize,
            double dpi,
            double mmPerPixel,
            Envelope extent,
            String gridSetName) {
        this.zoom = zoom;
        this.numTilesWide = numTilesWide;
        this.numTilesHigh = numTilesHigh;
        this.tileSizeM = tileSizeM;
        this.groundResolution = groundResolution;
        this.resolution = resolution;
        this.scale = scale;
        this.totalTiles = totalTiles;
        this.tilePixelSize = tilePixelSize;
        this.dpi = dpi;
        this.mmPerPixel = mmPerPixel;
        this.extent = extent;
        this.gridSetName = gridSetName;
    }

    /**
     * 获取格式化的比例尺字符串。
     *
     * @return {@code 1:xxx} 格式的比例尺文本
     */
    public String getScaleString() {
        if (scale >= 1000000) {
            return String.format("1:%.1f万", scale / 10000);
        }
        return String.format("1:%.0f", scale);
    }
}
