package cn.geoair.map.tile.forge.core.support.arcgis;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.utils.CentralDirectoryUtils;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.cache.ZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author ：张俊 &#064;date ：Created in 2025/11/17 10:16
 *     &#064;description：本地配置XML获取器抽象类，用于从本地文件系统读取ArcGIS图层配置文件
 */
public abstract class AbstractArcgisZipDirectoryGetter extends AbstractArcgisSupport
        implements ZipDirectoryGetter {
    public static GiLogger log = GirLoggerFactory.getLogger();

    public AbstractArcgisZipDirectoryGetter(GirLayerConfigContextHelper contextHelper) {
        super(contextHelper);
    }

    @Override
    public GirLayerConfigContextHelper getContextHelper() {
        return contextHelper;
    }

    public void preCacheCentralDir(
            GirLayerConfigContext layerConfigContext, List<ProgressConsumer> progressConsumers) {
        ICompressionHandler iCompressionHandler = getICompressionHandler();
        List<TileCentralDirectoryModel> batchList = new ArrayList<>();

        AtomicReference<Integer> count = new AtomicReference<>(0);
        AtomicReference<Integer> saveCount = new AtomicReference<>(0);
        try (LayerPerFileDao layerPerFileDao =
                contextHelper.getLayerPerFileDao(layerConfigContext)) {
            boolean b = layerPerFileDao.cacheEnableIs(layerConfigContext);
            if (b) {
                log.info("该数据的缓存已经构建过，此次无需构建！");
                return;
            } else {
                log.info(
                        "开始扫描压缩包{}，{}",
                        layerConfigContext.getStorageType().getValue(),
                        layerConfigContext.getObjectKey());

                RootPathInfo rootPathInfo =
                        preCheckZipAndGetRoot(layerConfigContext, iCompressionHandler);
                String rootPath = rootPathInfo.getRootPath();
                layerPerFileDao.doPreCacheStart();
                iCompressionHandler.scanAllEntries(
                        layerConfigContext.getObjectKey(),
                        (centralDirectoryEntry, allCount, currentCount) -> {
                            try {
                                if (GutilObject.isNotEmpty(progressConsumers)) {
                                    progressConsumers.forEach(
                                            progressConsumer ->
                                                    progressConsumer.accept(
                                                            allCount, currentCount));
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
                            TileCentralDirectoryModel tileCentralDirectoryEntry =
                                    tranToTileModel(centralDirectoryEntry);
                            if (tileCentralDirectoryEntry == null) {
                                return true;
                            }
                            saveCount.updateAndGet(v -> v + 1);
                            batchList.add(tileCentralDirectoryEntry);
                            if (batchList.size() >= 300) {
                                CentralDirectoryUtils.doInsert(batchList, layerPerFileDao);
                                log.info(
                                        "insert 图层{} 缓存条数  {} ,遍历总数{} ，已经插入的数量 {} ",
                                        layerConfigContext.getLayerName(),
                                        batchList.size(),
                                        count.get(),
                                        saveCount.get());
                                log.info("insert {}", batchList.size());
                                batchList.clear();
                            }
                            return true;
                        });
                if (!batchList.isEmpty()) {
                    CentralDirectoryUtils.doInsert(batchList, layerPerFileDao);
                }
                log.info(
                        "该压缩包的缓存构建完成{}，{}",
                        layerConfigContext.getStorageType().getValue(),
                        layerConfigContext.getObjectKey());
            }
            layerPerFileDao.doPreCacheEnd();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
