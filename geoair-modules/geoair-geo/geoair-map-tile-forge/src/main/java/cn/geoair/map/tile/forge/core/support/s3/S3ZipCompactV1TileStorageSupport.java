package cn.geoair.map.tile.forge.core.support.s3;

import cn.geoair.map.tile.forge.core.support.local.LocalZipCompactV1TileStorageSupport;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.S3CompressionHandler;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/13 17:58
 * &#064;description：S3存储的紧凑型V1瓦片支持类，用于处理基于S3的压缩瓦片存储和读取
 */
public class  S3ZipCompactV1TileStorageSupport extends LocalZipCompactV1TileStorageSupport {

    /**
     * 获取压缩处理器实例
     * 如果压缩处理器未初始化，则创建一个新的S3压缩处理器实例
     * @return ICompressionHandler 压缩处理器实例
     */
    protected ICompressionHandler getICompressionHandler() {
        if (compressionHandler == null) {
            compressionHandler = new S3CompressionHandler();
        }
        return compressionHandler;
    }

}
