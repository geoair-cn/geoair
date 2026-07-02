package cn.geoair.map.tile.forge.core.support.arcgis;

import cn.geoair.base.util.GutilObject;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.AbstractTileStorageSupport;
import cn.geoair.map.tile.forge.core.utils.ArcgisTileUtils;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryEntry;
import cn.geoair.map.tile.forge.core.zip.cache.ZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryEntry;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/17 10:16
 * &#064;description：本地配置XML获取器抽象类，用于从本地文件系统读取ArcGIS图层配置文件
 */
@Slf4j
public abstract class ConfigXmlGetterZip implements ArcgisConfigXmlGetter {

    /**
     * 获取压缩文件处理器实例
     *
     * @return ICompressionHandler 压缩文件处理器接口实现
     */
    protected abstract ICompressionHandler getICompressionHandler();

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
