package cn.geoair.map.tile.forge.core.zip.model;

import cn.geoair.map.tile.forge.core.enums.GirCompressionType;
import cn.geoair.map.tile.forge.core.zip.decompression.DecompressionHandler;
import jakarta.persistence.Transient;
import lombok.Data;



/**
 * ZIP 中央目录中的单个文件条目
 */
@Data
public class CentralDirectoryModel {
    // 本地文件头的起始偏移量
    private long localHeaderOffset;
    // 压缩数据的起始偏移量（可通过本地文件头计算）
    private Long dataOffset;
    // 压缩方法（0=未压缩，8=DEFLATE）
    private long compressionMethod;
    // 压缩后的大小
    private long compressedSize;
    // 解压后的大小
    private long uncompressedSize;

    // 文件名
    private String name;

    // 条目总大小
    private int entrySize;

    // 是否文件夹
    private boolean directoryIs;

    public CentralDirectoryModel() {

    }

    public CentralDirectoryModel(long localHeaderOffset, Long dataOffset, long compressionMethod,
                                 long compressedSize, long uncompressedSize, String name, int entrySize) {
        this.localHeaderOffset = localHeaderOffset;
        this.dataOffset = dataOffset;
        this.compressionMethod = compressionMethod;
        this.compressedSize = compressedSize;
        this.uncompressedSize = uncompressedSize;
        this.name = name;
        this.entrySize = entrySize;
    }

    @Transient
    public DecompressionHandler getDecompressionHandler() {
        int methodCode = (int) this.getCompressionMethod();
        GirCompressionType type = GirCompressionType.getByMethodCode(methodCode);
        DecompressionHandler handler = type.getHandler();
        if (handler == null) {
            throw  new RuntimeException("Unknown compression method: " + methodCode);
        }
        return handler;
    }

}
