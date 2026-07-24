package cn.geoair.map.tile.forge.fuser.mbtiles;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.tools.simple.GirImageUtil;
import java.awt.image.BufferedImage;
import java.io.IOException;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * MBTiles 瓦片信息对象
 *
 * <p>对应 tiles 表结构：zoom_level, tile_column, tile_row, tile_data
 *
 * @author 张俊
 * @date Created in 2026/6/25 09:30
 */
@Data
@Accessors(chain = true)
public class MbtilesInfo {
    private static GiLogger log = GirLoggerFactory.getLogger();

    public static MbtilesInfo of() {
        return new MbtilesInfo();
    }

    /** 层级 (Zoom Level) */
    private Integer zoomLevel;

    /** 列号 (Tile Column / X) */
    private Integer tileColumn;

    /** 行号 (Tile Row / Y) 注意：MBTiles 标准中 y 轴方向为 TMS 格式（从南到北） */
    private Integer tileRow;

    /** 瓦片数据（二进制） 格式通常为 PNG、JPG、WebP 或 PBF */
    private byte[] tileData;

    public MbtilesInfo setX(Integer tileColumn) {
        this.tileColumn = tileColumn;
        return this;
    }

    public MbtilesInfo setY(Integer tileRow) {
        this.tileRow = tileRow;
        return this;
    }

    public Integer getX() {
        return tileColumn;
    }

    public Integer getY() {
        return tileRow;
    }

    /** 构造函数 */
    public MbtilesInfo() {}

    /**
     * 全参构造函数
     *
     * @param zoomLevel 层级
     * @param tileColumn 列号
     * @param tileRow 行号
     * @param tileData 瓦片数据
     */
    public MbtilesInfo(Integer zoomLevel, Integer tileColumn, Integer tileRow, byte[] tileData) {
        this.zoomLevel = zoomLevel;
        this.tileColumn = tileColumn;
        this.tileRow = tileRow;
        this.tileData = tileData;
    }

    /**
     * 检查瓦片数据是否为空
     *
     * @return 是否为空
     */
    public boolean isEmpty() {
        return tileData == null || tileData.length == 0;
    }

    /**
     * 获取瓦片数据大小
     *
     * @return 数据大小（字节）
     */
    public int getDataSize() {
        return tileData == null ? 0 : tileData.length;
    }

    /**
     * 获取瓦片坐标字符串
     *
     * @return 坐标字符串，格式：z_x_y
     */
    public String getCoordinateKey() {
        return String.format("%d_%d_%d", zoomLevel, tileColumn, tileRow);
    }

    @Override
    public String toString() {
        return String.format(
                "MbtilesInfo{z=%d, x=%d, y=%d, size=%d}",
                zoomLevel, tileColumn, tileRow, getDataSize());
    }

    /**
     * 将瓦片数据转换为 BufferedImage
     *
     * <p>支持 PNG、JPG、WebP 等图片格式，不支持 PBF 矢量瓦片
     *
     * @return BufferedImage 对象，如果数据为空或解析失败返回 null
     */
    public BufferedImage toImage() {
        if (tileData == null || tileData.length == 0) {
            log.warn("瓦片数据为空，无法转换为图片: z={}, x={}, y={}", zoomLevel, tileColumn, tileRow);
            return null;
        }
        try {
            BufferedImage bufferedImage = GirImageUtil.bytesToImage(tileData);
            if (bufferedImage == null) {
                log.warn(
                        "无法解析瓦片数据为图片: z={}, x={}, y={}, 数据大小={}",
                        zoomLevel,
                        tileColumn,
                        tileRow,
                        tileData.length);
            }
            return bufferedImage;
        } catch (IOException e) {
            log.error("转换瓦片数据为图片失败: z={}, x={}, y={}", zoomLevel, tileColumn, tileRow, e);
            return null;
        }
    }
}
