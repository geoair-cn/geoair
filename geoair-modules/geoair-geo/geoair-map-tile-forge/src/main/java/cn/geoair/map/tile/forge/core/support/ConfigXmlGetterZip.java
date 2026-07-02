package cn.geoair.map.tile.forge.core.support;

import cn.geoair.base.util.GutilObject;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
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
public abstract class ConfigXmlGetterZip extends AbstractTileStorageSupport implements ZipDirectoryGetter {

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


    public void initTileCentralDirectoryEntryDao(GirLayerConfigContext layerConfigContext, List<ProgressConsumer> progressConsumers) {
        ICompressionHandler iCompressionHandler = getICompressionHandler();
        List<TileCentralDirectoryEntry> batchList = new ArrayList<>();
        GirLayerConfigContextHelper instance = GirLayerConfigContextHelper.getInstance();
        AtomicReference<Integer> count = new AtomicReference<>(0);
        AtomicReference<Integer> saveCount = new AtomicReference<>(0);
        try (LayerPerFileDao layerPerFileDao = instance.getLayerPerFileDao(layerConfigContext)) {
            boolean b = layerPerFileDao.cacheEnableIs(layerConfigContext);
            if (b) {
                log.info("该数据的缓存已经构建过，此次无需构建！");
                return;
            } else {
                log.info("开始扫描压缩包{}，{}", layerConfigContext.getStorageType().getValue(), layerConfigContext.getObjectKey());

                String rootPath = preCheckZip(layerConfigContext, iCompressionHandler);

                layerPerFileDao.doPreCacheStart();
                iCompressionHandler.scanAllEntries(layerConfigContext.getObjectKey(), (centralDirectoryEntry, allCount, currentCount) -> {
                    try {
                        if (GutilObject.isNotEmpty(progressConsumers)) {
                            progressConsumers.forEach(progressConsumer -> progressConsumer.accept(allCount, currentCount));
                        }
                    } catch (Exception e) {
                    }
                    count.updateAndGet(v -> v + 1);
                    boolean directoryIs = centralDirectoryEntry.isDirectoryIs();
                    if (directoryIs) {
                        return true;
                    }
                    String name = centralDirectoryEntry.getName();
                    if (!name.contains(rootPath)) { // 不在根下面的节点就给他不处理
                        return true;
                    }
                    String replace = name.replace(rootPath, "");
                    centralDirectoryEntry.setName(replace);
                    TileCentralDirectoryEntry tileCentralDirectoryEntry = getTileCentralDirectoryEntry(centralDirectoryEntry);
                    if (tileCentralDirectoryEntry == null) {
                        return true;
                    }
                    saveCount.updateAndGet(v -> v + 1);
                    batchList.add(tileCentralDirectoryEntry);
                    if (batchList.size() >= 300) {
                        doInsert(batchList, layerPerFileDao);
                        log.info("insert 图层{} 缓存条数  {} ,遍历总数{} ，已经插入的数量 {} ", layerConfigContext.getLayerName(), batchList.size(), count.get(), saveCount.get());
                        log.info("insert {}", batchList.size());
                        batchList.clear();
                    }
                    return true;
                });
                if (!batchList.isEmpty()) {
                    doInsert(batchList, layerPerFileDao);
                }
                log.info("该压缩包的缓存构建完成{}，{}", layerConfigContext.getStorageType().getValue(), layerConfigContext.getObjectKey());
            }
            layerPerFileDao.doPreCacheEnd();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract TileCentralDirectoryEntry getTileCentralDirectoryEntry(CentralDirectoryEntry centralDirectoryEntry);

    protected void doInsert(List<TileCentralDirectoryEntry> batchList, LayerPerFileDao layerPerFileDao) {

        List<TileCentralDirectoryEntry> insertList = new ArrayList<>(batchList);
        getExecutor().submit(() -> {
            try {
                layerPerFileDao.batchInsert(insertList);
                insertList.clear();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

    }

    /**
     * 前置检查ZIP文件，并获取到当前的zip的根
     *
     * @param layerConfigContext
     * @param iCompressionHandler
     * @return
     * @throws IOException
     */
    protected abstract String preCheckZip(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException;

}
