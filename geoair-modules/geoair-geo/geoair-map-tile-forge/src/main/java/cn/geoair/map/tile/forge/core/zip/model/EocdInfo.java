package cn.geoair.map.tile.forge.core.zip.model;

import lombok.Data;

/**
 * ZIP文件中央目录结束记录（EOCD）封装
 */
@Data
public class EocdInfo {
    // 当前磁盘号
    private long diskNumber;
    // 中央目录开始的磁盘号
    private long startDisk;
    // 当前磁盘上的中央目录条目数
    private long diskEntries;
    // 中央目录条目总数
    private long totalEntries;
    // 中央目录总大小（字节）
    private long centralDirSize;
    // 中央目录起始偏移量（字节）
    private long centralDirOffset;
    // ZIP文件注释长度
    private long commentLength;
    // ZIP的文件大小
    private long fileSize;

    // 构造方法（对应解析逻辑）
    public EocdInfo(long diskNumber, long startDisk, long diskEntries, long totalEntries,
                    long centralDirSize, long centralDirOffset, long commentLength) {
        this.diskNumber = diskNumber;
        this.startDisk = startDisk;
        this.diskEntries = diskEntries;
        this.totalEntries = totalEntries;
        this.centralDirSize = centralDirSize;
        this.centralDirOffset = centralDirOffset;
        this.commentLength = commentLength;
    }
}
