package cn.geoair.map.tile.forge.core.support.s3;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.support.local.LocalZipCompactV2TileStorageSupport;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.S3CompressionHandler;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/13 17:58
 * &#064;description：S3存储的紧凑型V2瓦片支持类，继承自本地ZIP紧凑型V2瓦片支持类，用于处理S3上的压缩瓦片数据
 */
public class S3ZipCompactV2TileStorageSupport extends LocalZipCompactV2TileStorageSupport {
    public S3ZipCompactV2TileStorageSupport(GirLayerConfigContextHelper contextHelper) {
        super(contextHelper);
    }

    public ICompressionHandler getICompressionHandler() {
        if (compressionHandler == null) {
            compressionHandler = new S3CompressionHandler();
        }
        return compressionHandler;
    }

}
