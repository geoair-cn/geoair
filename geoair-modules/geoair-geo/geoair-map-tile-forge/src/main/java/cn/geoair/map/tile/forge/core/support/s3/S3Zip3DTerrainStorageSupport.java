package cn.geoair.map.tile.forge.core.support.s3;


import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.support.local.LocalZip3DTerrainStorageSupport;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.S3CompressionHandler;
import lombok.extern.slf4j.Slf4j;


/**
 * S3ZIP瓦片存储支持类
 * 提供从ZIP压缩包中读取三维地形瓦片数据的功能
 *
 * @author 张俊
 * @since 2025/11/17
 */
@Slf4j
public class S3Zip3DTerrainStorageSupport extends LocalZip3DTerrainStorageSupport {
    public S3Zip3DTerrainStorageSupport(GirLayerConfigContextHelper contextHelper) {
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
