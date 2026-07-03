package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.arcgis.AbstractArcgisZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.utils.ArcgisTileUtils;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.LocalCompressionHandler;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;

import java.io.IOException;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/17 15:20
 * &#064;description：本地松散ZIP瓦片存储支持类，用于处理ArcGIS瓦片数据的读取和解压缩
 */
public class LocalZipLooseTileStorageSupport extends AbstractArcgisZipDirectoryGetter {
    public LocalZipLooseTileStorageSupport(GirLayerConfigContextHelper contextHelper ) {
        super(contextHelper);

    }

    /**
     * 压缩处理器实例，用于处理ZIP文件的解压缩操作
     */
    protected ICompressionHandler compressionHandler = null;

    /**
     * 获取压缩处理器实例
     * 使用单例模式，确保只有一个压缩处理器实例存在
     *
     * @return ICompressionHandler 压缩处理器实例
     */
    @Override
    public ICompressionHandler getICompressionHandler() {
        if (compressionHandler == null) {
            compressionHandler = new LocalCompressionHandler();
        }
        return compressionHandler;
    }



    /**
     * 根据图层配置和瓦片坐标获取瓦片数据
     *
     * @param layerConfigContext 图层配置信息对象
     * @param z              瓦片级别
     * @param x              瓦片列号
     * @param y              瓦片行号
     * @return TileRequest 瓦片请求对象，包含瓦片数据
     * @throws Exception 获取瓦片数据过程中可能出现的异常
     */
    @Override
    public TileRequest getTileData(GirLayerConfigContext layerConfigContext, String z, String x, String y) throws Exception {

        return null;
    }


    @Override
    public TileCentralDirectoryModel tranToTileModel(CentralDirectoryModel centralDirectoryModel) {
        return null;
    }

    @Override
    public RootPathInfo preCheckZipAndGetRoot(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException {
        return RootPathInfo.of();
    }

    /**
     * 从压缩包中获取配置XML文件内容
     *
     * @param layerConfigContext 图层配置信息对象
     * @return String 配置XML文件内容
     * @throws Exception 文件读取异常
     */
    @Override
    public String getConfigXml(GirLayerConfigContext layerConfigContext) throws Exception {
        return ArcgisTileUtils.getConfigXmlByZip(layerConfigContext, getICompressionHandler());
    }


    @Override
    public String getConfigCdi(GirLayerConfigContext layerConfigContext) throws Exception {
        return ArcgisTileUtils.getConfigCdiByZip(layerConfigContext, getICompressionHandler());
    }

}
