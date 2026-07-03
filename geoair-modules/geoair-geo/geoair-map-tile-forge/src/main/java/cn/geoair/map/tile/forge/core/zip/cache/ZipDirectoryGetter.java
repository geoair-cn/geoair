package cn.geoair.map.tile.forge.core.zip.cache;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.LogProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.IoUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/11/21 16:05
 * @description： zip的中央目录的获取器
 */
public interface ZipDirectoryGetter {

    GirLayerConfigContextHelper getContextHelper();

    /**
     * 获取压缩文件处理器实例
     *
     * @return ICompressionHandler 压缩文件处理器接口实现
     */
    ICompressionHandler getICompressionHandler();


    TileCentralDirectoryModel tranToTileModel(CentralDirectoryModel centralDirectoryModel);

    /**
     * 根据XYZ坐标获取ZIP中央目录条目
     *
     * @param layerConfigContext 图层配置信息
     * @param x                  X坐标
     * @param y                  Y坐标
     * @param z                  Z坐标(层级)
     * @return 中央目录条目
     */
    default TileCentralDirectoryModel getZipDirectoryByXyz(GirLayerConfigContext layerConfigContext, String x, String y, String z) {
        GirLayerConfigContextHelper instance = getContextHelper();
        LayerPerFileDao layerPerFileDao = instance.getLayerPerFileDao(layerConfigContext);
        try {
            return layerPerFileDao.findByXyz(x, y, z);
        } catch (SQLException e) {
            return null;
        } finally {
            IoUtil.close(layerPerFileDao);
        }
    }

    default TileCentralDirectoryModel getZipDirectoryBFileName(GirLayerConfigContext layerConfigContext, String fileName) {
        GirLayerConfigContextHelper instance = getContextHelper();
        LayerPerFileDao layerPerFileDao = instance.getLayerPerFileDao(layerConfigContext);
        try {
            return layerPerFileDao.findByFileName(fileName);
        } catch (SQLException e) {
            return null;
        } finally {
            IoUtil.close(layerPerFileDao);
        }
    }

    /**
     * 初始化瓦片中央目录条目DAO
     *
     * @param layerConfigContext 图层配置信息
     */
    default void preCacheCentralDir(GirLayerConfigContext layerConfigContext) {
        preCacheCentralDir(layerConfigContext, ListUtil.of(new LogProgressConsumer()));
    }

    void preCacheCentralDir(GirLayerConfigContext layerConfigContext, List<ProgressConsumer> progressConsumers);

    /**
     * 前置检查ZIP文件，并获取到当前的zip的根
     *
     * @param layerConfigContext
     * @param iCompressionHandler
     * @return
     * @throws IOException
     */
    RootPathInfo preCheckZipAndGetRoot(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException;
}
