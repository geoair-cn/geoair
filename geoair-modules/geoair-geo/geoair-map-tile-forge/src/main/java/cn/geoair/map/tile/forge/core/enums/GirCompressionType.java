package cn.geoair.map.tile.forge.core.enums;

import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.map.tile.forge.core.zip.decompression.*;
import lombok.Getter;

/** 压缩文件类型枚举（包含对应的ZIP压缩方法代码） */
@Getter
public enum GirCompressionType implements GiVisualValuable<String> {
    /** 未压缩（ZIP规范中method=0） */
    UNCOMPRESSED("未压缩", 0, new UncompressedHandler()),
    /** ZIP标准压缩（DEFLATE算法，method=8） */
    ZIP("ZIP压缩（DEFLATE）", 8, new ZipHandler()),
    /** GZIP压缩（method=1） */
    GZIP("GZIP压缩", 1, new GzipHandler()),
    /** TAR.GZ压缩（BZIP2算法，method=12） */
    TAR_GZ("BZIP2压缩（BZIP2）", 12, new Bzip2Handler());

    // 文本描述
    private final String text;
    // 对应的ZIP压缩方法代码
    private final int methodCode;
    // 关联的解压处理器
    private final DecompressionHandler handler;

    GirCompressionType(String text, int methodCode, DecompressionHandler handler) {
        this.text = text;
        this.methodCode = methodCode;
        this.handler = handler;
    }

    /** 根据压缩方法代码获取对应的枚举 */
    public static GirCompressionType getByMethodCode(int methodCode) {
        for (GirCompressionType type : values()) {
            if (type.methodCode == methodCode) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的压缩方法代码: " + methodCode);
    }
}
