package cn.geoair.map.tile.forge.core.support.s3;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.support.local.LocalZip3DTileStorageSupport;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.S3CompressionHandler;
 


/**
 * 本地ZIP瓦片存储支持类
 * 提供从ZIP压缩包中读取3DTiles瓦片数据的功能
 *
 * @author 张俊
 * @since 2025/11/17
 */
 
public class S3Zip3DTileStorageSupport extends LocalZip3DTileStorageSupport {
    public static GiLogger log = GirLoggerFactory.getLogger();
    public S3Zip3DTileStorageSupport(GirLayerConfigContextHelper contextHelper) {
        super(contextHelper);
    }

    /**
     * 获取压缩处理器实例
     * 使用懒加载单例模式，确保只有一个压缩处理器实例存在
     *
     * @return ICompressionHandler 压缩处理器实例
     */
    public ICompressionHandler getICompressionHandler() {
        if (compressionHandler == null) {
            compressionHandler = new S3CompressionHandler();
        }
        return compressionHandler;
    }


}
