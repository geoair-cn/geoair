package cn.geoair.map.tile.forge.core.zip.cache;

import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import lombok.Data;

/**
 * @author ：张俊
 * @date ：Created in 2025/11/21 15:22
 * @description： TODO
 */
@Data
public class TileCentralDirectoryModel extends CentralDirectoryModel {
    public TileCentralDirectoryModel(
            long localHeaderOffset,
            Long dataOffset,
            long compressionMethod,
            long compressedSize,
            long uncompressedSize,
            String name,
            int entrySize) {
        super(
                localHeaderOffset,
                dataOffset,
                compressionMethod,
                compressedSize,
                uncompressedSize,
                name,
                entrySize);
    }

    public TileCentralDirectoryModel() {}

    private Long id;

    private String xyzPath; // 松散型路径（如 "3/1/2"）
    private String x; // 列号
    private String y; // 行号
    private String z; // 层级

    private String fileName; // 松散型：文件名（如 "2.png"）；紧凑型：bundle文件名（如 "0.bundle"）
    private String storageType; // 存储类型标记："LOOSE" / "COMPACT"

    /**
     * 获取列号x的Long类型值
     *
     * @return 列号
     */
    public Long getXAsLong() {
        return x != null ? Long.parseLong(x) : null;
    }

    /**
     * 获取行号y的Long类型值
     *
     * @return 行号
     */
    public Long getYAsLong() {
        return y != null ? Long.parseLong(y) : null;
    }

    /**
     * 获取层级z的Long类型值
     *
     * @return 层级
     */
    public Long getZAsLong() {
        return z != null ? Long.parseLong(z) : null;
    }

    /**
     * 获取列号x的Integer类型值
     *
     * @return 列号
     */
    public Integer getXAsInt() {
        return x != null ? Integer.parseInt(x) : null;
    }

    /**
     * 获取行号y的Integer类型值
     *
     * @return 行号
     */
    public Integer getYAsInt() {
        return y != null ? Integer.parseInt(y) : null;
    }

    /**
     * 获取层级z的Integer类型值
     *
     * @return 层级
     */
    public Integer getZAsInt() {
        return z != null ? Integer.parseInt(z) : null;
    }
}
