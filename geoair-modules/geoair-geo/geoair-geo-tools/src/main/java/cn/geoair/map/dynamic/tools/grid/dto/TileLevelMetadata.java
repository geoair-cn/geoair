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
    private final int zoom; // 缩放级别
    private final double tileCount; // 每行/列的瓦片数量
    private final double tileSizeM; // 每个瓦片的地理尺寸（米 ）
    private final double groundResolution; // 地面分辨率（米/像素 ）
    private final double resolution; //  分辨率
    private final double scale; // 比例尺
    private final long totalTiles; // 总瓦片数量
    private final int tilePixelSize; // 瓦片像素尺寸
    private final double dpi; // 屏幕DPI
    private final double mmPerPixel; // 每像素代表的毫米
    private final Envelope extent; // 全局范围
    private final String gridSetName; // 坐标系

    public TileLevelMetadata(
            int zoom,
            double tileCount,
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
        this.tileCount = tileCount;
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
