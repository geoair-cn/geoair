package cn.geoair.map.tile.forge.core.zip.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/** ZIP 本地文件头（Local File Header） 存储单个文件在ZIP中的头部元数据，用于定位文件实际数据的起始位置 */
@Data
@AllArgsConstructor
public class LocalFileHeader {
    /** 文件名长度（字节） */
    private long fileNameLength;

    /** 扩展字段长度（字节） */
    private long extraFieldLength;

    /** 文件实际数据的起始偏移量（字节） */
    private long dataOffset;
}
