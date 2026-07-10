package cn.geoair.map.tile.forge.core.support.s3;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.support.local.LocalZipXYZTileStorageSupport;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.S3CompressionHandler;
 

/**
 * @author ：张俊
 * @date ：Created in 2025/11/17 15:20
 * @description：S3 ZIP瓦片存储支持类，用于处理存储在S3中的XYZ瓦片数据的读取和解压缩
 */
 
public class S3ZipXYZTileStorageSupport extends LocalZipXYZTileStorageSupport {
    public static GiLogger log = GirLoggerFactory.getLogger();
    public S3ZipXYZTileStorageSupport(GirLayerConfigContextHelper contextHelper) {
        super(contextHelper);
    }

    /**
     * 获取压缩处理器实例
     * 使用单例模式，确保只有一个压缩处理器实例存在
     *
     * @return ICompressionHandler 压缩处理器实例
     */
    @Override
    public ICompressionHandler getICompressionHandler() {
        if (compressionHandler == null) {
            compressionHandler = new S3CompressionHandler();
        }
        return compressionHandler;
    }


}
