package cn.geoair.map.dynamic.tools.grid.dto;

import lombok.Data;
import org.locationtech.jts.geom.Envelope;

/**
 * @author ：张俊
 * @date ：Created in 2026/5/9 09:55
 * @description： 瓦片层级元数据类
 */
@Data
public class TileLevelMetadata {

    /** 缩放级别（0~maxZoom） */
    private final int zoom;

    /** 每行的瓦片数量（水平方向/X轴） */
    private final int numTilesWide;

    /** 每列的瓦片数量（垂直方向/Y轴） */
    private final int numTilesHigh;

    /** 每个瓦片的地理尺寸（米），计算公式：globalWidthMeters / numTilesWide */
    private final double tileSizeM;

    /** 地面分辨率（米/像素），每个像素代表的地面实际距离，计算公式：tileSizeM / tilePixelSize */
    private final double groundResolution;

    /** 分辨率（米/像素） （度/像素） */
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

    /** 当前缩放级别的全局范围（EPSG:3857 平面坐标，单位：米） */
    private final Envelope extent;

    /** 网格集名称（如 EPSG:4326 或 EPSG:3857） */
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

    /** 获取格式化的比例尺字符串 */
    public String getScaleString() {
        if (scale >= 1000000) {
            return String.format("1:%.1f万", scale / 10000);
        }
        return String.format("1:%.0f", scale);
    }
}
