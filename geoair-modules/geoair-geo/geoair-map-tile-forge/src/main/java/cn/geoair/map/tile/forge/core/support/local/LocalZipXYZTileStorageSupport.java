package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.AbstractZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.utils.CentralDirectoryUtils;
import cn.geoair.map.tile.forge.core.utils.ForgeExecutorUtils;
import cn.geoair.map.tile.forge.core.utils.TilePathParser;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.LocalCompressionHandler;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;
import cn.geoair.web.util.GutilMimeType;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 本地ZIP瓦片存储支持类
 * 提供从ZIP压缩包中读取XYZ坐标系瓦片数据的功能
 *
 * @author 张俊
 * @since 2025/11/17
 */

public class LocalZipXYZTileStorageSupport extends AbstractZipDirectoryGetter implements ITileStorageSupport {
    public static GiLogger log = GirLoggerFactory.getLogger();

    public LocalZipXYZTileStorageSupport(GirLayerConfigContextHelper contextHelper) {
        super(contextHelper);
    }

    /**
     * 压缩处理器实例，用于处理ZIP文件的解压缩操作
     */
    protected ICompressionHandler compressionHandler = null;

    /**
     * 获取压缩处理器实例
     * 使用懒加载单例模式，确保只有一个压缩处理器实例存在
     *
     * @return ICompressionHandler 压缩处理器实例
     */
    public ICompressionHandler getICompressionHandler() {
        if (compressionHandler == null) {
            compressionHandler = new LocalCompressionHandler();
        }
        return compressionHandler;
    }

    @Override
    public TileCentralDirectoryModel tranToTileModel(CentralDirectoryModel centralDirectoryModel) {
        TileCentralDirectoryModel tileCentralDirectoryEntry = new TileCentralDirectoryModel();
        BeanUtil.copyProperties(centralDirectoryModel, tileCentralDirectoryEntry);
        tileCentralDirectoryEntry.setId(IdUtil.getSnowflakeNextId());
        TilePathParser.XyzTileInfo xyzTileInfo = TilePathParser.parseXyzPath(centralDirectoryModel.getName());
        if (xyzTileInfo == null) {
            return null;
        }
        tileCentralDirectoryEntry.setY(xyzTileInfo.getY() + "");
        tileCentralDirectoryEntry.setX(xyzTileInfo.getX() + "");
        tileCentralDirectoryEntry.setZ(xyzTileInfo.getZ() + "");
        tileCentralDirectoryEntry.setXyzPath(xyzTileInfo.getZ() + "/" + xyzTileInfo.getX() + "/" + xyzTileInfo.getY());
        tileCentralDirectoryEntry.setFileName(xyzTileInfo.getFileName());
        return tileCentralDirectoryEntry;
    }


    /**
     * 根据图层配置和瓦片坐标获取瓦片数据
     * 首先检查本地临时目录是否存在对应瓦片文件，如果不存在则从ZIP文件中解压到本地
     *
     * @param layerConfigContext 图层配置信息对象，包含瓦片存储路径等配置信息
     * @param z                  瓦片级别(Zoom Level)
     * @param x                  瓦片列号(X Coordinate)
     * @param y                  瓦片行号(Y Coordinate)
     * @return TileRequest 瓦片请求对象，包含瓦片数据流及相关元信息
     * @throws Exception 获取瓦片数据过程中可能出现的IO异常或解压异常
     */
    @Override
    public TileRequest getTileData(GirLayerConfigContext layerConfigContext, String z, String x, String y) throws Exception {
        TileRequest tileRequest = TileRequest.emptyByContext(layerConfigContext);
        String format = layerConfigContext.getFormat();
        if (format == null) {
            format = "png";
        }
        String tempDirAbsolutePath = TileTempPathConfig.getInstance().buildLocalTempDirPath(layerConfigContext);
        StringBuilder inLocalPathBuilder = new StringBuilder();
        inLocalPathBuilder.append(tempDirAbsolutePath).append(File.separator)
                .append(z).append(File.separator)
                .append(y).append(File.separator)
                .append(x).append(".").append(format);


        String inLocalPath = inLocalPathBuilder.toString().trim();
        File localTileFile = new File(inLocalPath);
        boolean localStatu = localTileFile.exists();
        if (!localStatu) {
            // xyz的zip特别大，byZip 去zip里面找特别耗时，故全部请求全部走byPreCache，有多少看多少。
            localStatu = byPreCache(layerConfigContext, z, y, x, inLocalPath);

        }
        if (localStatu) {
            tileRequest.setBytes(FileUtil.readBytes(localTileFile));
            tileRequest.setLastModified(localTileFile.lastModified());
            tileRequest.setSize(localTileFile.length());
            tileRequest.setExists(true);
            tileRequest.setMimeType(GutilMimeType.createFromExtension(format));
        }
        return tileRequest;
    }


    private boolean byPreCache(GirLayerConfigContext layerConfigContext, String z, String y, String x, String inLocalPath) {
        try {
            TileCentralDirectoryModel zipDirectoryByFileName = getZipDirectoryByXyz(layerConfigContext, x + "", y + "", z + ""); //空指针
            if (zipDirectoryByFileName == null) {
                return false;
            }
            getICompressionHandler().readAndDecompressEntryToLocal(zipDirectoryByFileName, layerConfigContext.getObjectKey(), inLocalPath);
            return true;
        } catch (Exception e) {
            log.error("getTileDataByPreZipCache error:", e);
            return false;
        }

    }

    @Override
    public void preCacheTiles(GirLayerConfigContext layerConfigContext, TileCache tileCache, ProgressConsumer progressConsumer) {
        log.info("preCacheTiles start...{}", getClass().getName());
        this.preCacheCentralDir(layerConfigContext, ListUtil.of(progressConsumer));

        LayerPerFileDao layerPerFileDao = contextHelper.getLayerPerFileDao(layerConfigContext);
        // 参数校验
//        if (layerConfigContext == null || tileCache == null) {
//            throw new IllegalArgumentException("layerConfigDto和cacheProvider不能为空");
//        }
        if (tileCache == null) {
            log.info("tileCache is null， 跳过缓存执行操作");
            return;
        }

        ICompressionHandler iCompressionHandler = getICompressionHandler();
        String objectKey = layerConfigContext.getObjectKey();
        try {
            layerPerFileDao.findAll(tileCentralDirectoryEntry -> {
                Integer z = tileCentralDirectoryEntry.getZAsInt(), x = tileCentralDirectoryEntry.getXAsInt(), y = tileCentralDirectoryEntry.getYAsInt();
                TileRequest tileRequest = TileRequest.emptyByContext(layerConfigContext);
                String cacheKey = tileCache.buildTileCacheKey(layerConfigContext.getDataId(), z + "", y + "", x + "");
                ForgeExecutorUtils.getExecutor().submit(() -> {
                    try {
                        byte[] bytes = iCompressionHandler.readAndDecompressEntry(tileCentralDirectoryEntry, objectKey);
                        tileRequest.setBytes(bytes);
                        tileRequest.setLastModified(System.currentTimeMillis());
                        tileRequest.setSize(bytes.length);
                        tileRequest.setExists(true);
                        tileRequest.setMimeType(ImageMime.png);
                        tileCache.putTile(cacheKey, tileRequest, "png");
                    } catch (Exception e) {
                        log.error("preCacheTiles error:{}", e.getMessage());
                    }
                });
            });

        } catch (Exception e) {


        }

    }


    @Override
    public void preCacheCentralDir(GirLayerConfigContext layerConfigContext, List<ProgressConsumer> progressConsumers) {
        log.info("initTileCentralDirectoryEntryDao start...{}", getClass().getName());
        ICompressionHandler iCompressionHandler = getICompressionHandler();
        List<TileCentralDirectoryModel> batchList = new ArrayList<>();
        AtomicReference<Integer> count = new AtomicReference<>(0);
        AtomicReference<Integer> saveCount = new AtomicReference<>(0);

        Long layerPerCacheBatchSize = contextHelper.getLayerPerCacheBatchSize(layerConfigContext);
        try (LayerPerFileDao layerPerFileDao = contextHelper.getLayerPerFileDao(layerConfigContext)) {
            boolean b = layerPerFileDao.cacheEnableIs(layerConfigContext);
            if (b) {
                log.info("  start...{},enable..{}", layerPerFileDao.getClass().getName(), b);
                return;
            } else {
                log.info("开始扫描压缩包{}", layerConfigContext.getObjectKey());
                preCheckZipAndGetRoot(layerConfigContext, iCompressionHandler);
                iCompressionHandler.scanAllEntries(layerConfigContext.getObjectKey(), (centralDirectoryEntry, allCount, currentCount) -> {
                    try {
                        if (GutilObject.isNotEmpty(progressConsumers)) {
                            progressConsumers.forEach(progressConsumer -> progressConsumer.accept(allCount, currentCount));
                        }
                    } catch (Exception e) {

                    }
                    count.updateAndGet(v -> v + 1);
                    if (centralDirectoryEntry.isDirectoryIs()) {
                        return true;
                    }
                    TileCentralDirectoryModel centralDirectoryModel = tranToTileModel(centralDirectoryEntry);
                    if (centralDirectoryModel == null) {
                        return true;
                    }
                    saveCount.updateAndGet(v -> v + 1);
                    centralDirectoryModel.setStorageType(layerConfigContext.getStorageType().getValue());
                    batchList.add(centralDirectoryModel);
                    if (batchList.size() >= layerPerCacheBatchSize) {
                        try {
                            CentralDirectoryUtils.doInsert(batchList, layerPerFileDao);
                            log.info("insert 图层{} 缓存条数  {} ,遍历总数{} ，已经插入的数量 {} ", layerConfigContext.getLayerName(), batchList.size(), count.get(), saveCount.get());
                            batchList.clear();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return true;
                });
                if (!batchList.isEmpty()) {
                    CentralDirectoryUtils.doInsert(batchList, layerPerFileDao);
                }
            }
            layerPerFileDao.doPreCacheEnd();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public RootPathInfo preCheckZipAndGetRoot(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException {
        AtomicReference<String> checkPath = new AtomicReference<>("");
        iCompressionHandler.scanAllEntries(layerConfigContext.getObjectKey(), (centralDirectoryEntry, allCount, currentCount) -> {
            boolean directoryIs = centralDirectoryEntry.isDirectoryIs();
            if (directoryIs) {
                return true;
            }
            String name = centralDirectoryEntry.getName();
            TilePathParser.XyzTileInfo xyzTileInfo = TilePathParser.parseXyzPath(centralDirectoryEntry.getName());
            if (xyzTileInfo == null) {
                return true;
            }
            long x = xyzTileInfo.getX();
            long y = xyzTileInfo.getY();
            long z = xyzTileInfo.getZ();
            if (x != 0L && y != 0L && z != 0L) {
                checkPath.set(name);
                return false;
            }

            return true;
        });
        String tileSetJsonPath = checkPath.get();

        if (StrUtil.isEmpty(tileSetJsonPath)) {
            throw new RuntimeException("xyz瓦片总缺失XYZ组合");
        }

        return RootPathInfo.of();
    }

}
