package cn.geoair.map.tile.forge.core.zip.cache;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.zip.LogProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.IoUtil;

import java.sql.SQLException;
import java.util.List;

/**
 * @author ：zhangjun
 * @date ：Created in 2025/11/21 16:05
 * @description： zip的中央目录的获取器
 */
public interface ZipDirectoryGetter {


    /**
     * 根据XYZ坐标获取ZIP中央目录条目
     *
     * @param layerConfigContext 图层配置信息
     * @param x                  X坐标
     * @param y                  Y坐标
     * @param z                  Z坐标(层级)
     * @return 中央目录条目
     */
    default TileCentralDirectoryEntry getZipDirectoryByXyz(GirLayerConfigContext layerConfigContext, String x, String y, String z) {
        GirLayerConfigContextHelper instance = GirLayerConfigContextHelper.getInstance();
        LayerPerFileDao layerPerFileDao = instance.getLayerPerFileDao(layerConfigContext);
        try {
            return layerPerFileDao.findByXyz(x, y, z);
        } catch (SQLException e) {
            return null;
        } finally {
            IoUtil.close(layerPerFileDao);
        }
    }

    default TileCentralDirectoryEntry getZipDirectoryBFileName(GirLayerConfigContext layerConfigContext, String fileName) {
        GirLayerConfigContextHelper instance = GirLayerConfigContextHelper.getInstance();
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
    default void initTileCentralDirectoryEntryDao(GirLayerConfigContext layerConfigContext) {
        initTileCentralDirectoryEntryDao(layerConfigContext, ListUtil.of(new LogProgressConsumer()));
    }

    void initTileCentralDirectoryEntryDao(GirLayerConfigContext layerConfigContext, List<ProgressConsumer> progressConsumers);

}
