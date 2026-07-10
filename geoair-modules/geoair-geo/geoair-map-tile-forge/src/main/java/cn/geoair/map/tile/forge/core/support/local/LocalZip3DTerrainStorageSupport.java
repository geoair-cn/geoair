package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.AbstractZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.utils.TilePathParser;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.LocalCompressionHandler;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.cache.ZipDirectoryGetter;
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
import java.util.concurrent.atomic.AtomicReference;


/**
 * 本地ZIP瓦片存储支持类
 * 提供从ZIP压缩包中读取三维地形瓦片数据的功能
 *
 * @author 张俊
 * @since 2025/11/17
 */

public class LocalZip3DTerrainStorageSupport extends AbstractZipDirectoryGetter implements ZipDirectoryGetter, ITileStorageSupport {
    public static GiLogger log = GirLoggerFactory.getLogger();

    public LocalZip3DTerrainStorageSupport(GirLayerConfigContextHelper contextHelper) {
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
    public TileRequest getTileData(GirLayerConfigContext layerConfigContext, String z, String x, String y) throws Exception {
        TileRequest tileRequest = TileRequest.emptyByContext(layerConfigContext);
        String tempDirAbsolutePath = TileTempPathConfig.getInstance().buildLocalTempDirPath(layerConfigContext);
        StringBuilder inLocalPathBuilder = new StringBuilder();
        String format = layerConfigContext.getFormat();
        if (StrUtil.isEmpty(y) && StrUtil.isEmpty(x)) {
            inLocalPathBuilder.append(tempDirAbsolutePath).append(File.separator)
                    .append(z);
        } else {
            inLocalPathBuilder.append(tempDirAbsolutePath).append(File.separator)
                    .append(z).append(File.separator)
                    .append(y).append(File.separator)
                    .append(x).append(".").append(format);
        }
        String inLocalPath = inLocalPathBuilder.toString().trim();
        File localTileFile = new File(inLocalPath);
        boolean localStatu = localTileFile.exists();
        if (!localStatu) {
            localStatu = byPreCache(layerConfigContext, z, y, x, inLocalPath);
        }
        if (localStatu) {
            tileRequest.setBytes(FileUtil.readBytes(localTileFile));
            tileRequest.setLastModified(localTileFile.lastModified());
            tileRequest.setSize(localTileFile.length());
            tileRequest.setExists(true);
            tileRequest.setMimeType(GutilMimeType.fromExtension(localTileFile.getName()));
        }
        return tileRequest;
    }

    protected boolean byPreCache(GirLayerConfigContext layerConfigContext, String z, String y, String x, String inLocalPath) {
        try {
            TileCentralDirectoryModel tileCentralDirectoryEntry = null;
            if (StrUtil.isEmpty(y) && StrUtil.isEmpty(x)) {
                tileCentralDirectoryEntry = getZipDirectoryBFileName(layerConfigContext, z);
            } else {
                tileCentralDirectoryEntry = getZipDirectoryByXyz(layerConfigContext, x, y, z);
            }
            if (tileCentralDirectoryEntry == null) {
                return false;
            }
            getICompressionHandler().readAndDecompressEntryToLocal(tileCentralDirectoryEntry, layerConfigContext.getObjectKey(), inLocalPath);
            return true;
        } catch (Exception e) {
            log.error("getTileDataByPreZipCache error:", e);
            return false;
        }

    }


    @Override
    public void preCacheTiles(GirLayerConfigContext layerConfigContext, TileCache tileCache, ProgressConsumer progressConsumer) {
        this.preCacheCentralDir(layerConfigContext, ListUtil.of(progressConsumer));
    }


    @Override
    public TileCentralDirectoryModel tranToTileModel(CentralDirectoryModel centralDirectoryModel) {
        TileCentralDirectoryModel tileCentralDirectoryEntry = new TileCentralDirectoryModel();
        BeanUtil.copyProperties(centralDirectoryModel, tileCentralDirectoryEntry);
        tileCentralDirectoryEntry.setId(IdUtil.getSnowflakeNextId());
        tileCentralDirectoryEntry.setFileName(centralDirectoryModel.getName());

        TilePathParser.XyzTileInfo xyzTileInfo = TilePathParser.parseXyzPath(centralDirectoryModel.getName());
        if (xyzTileInfo == null) {
            return tileCentralDirectoryEntry;
        } else {
            tileCentralDirectoryEntry.setY(xyzTileInfo.getY() + "");
            tileCentralDirectoryEntry.setX(xyzTileInfo.getX() + "");
            tileCentralDirectoryEntry.setZ(xyzTileInfo.getZ() + "");
            tileCentralDirectoryEntry.setFileName(xyzTileInfo.getZ() + "/" + xyzTileInfo.getX() + "/" + xyzTileInfo.getY());
            return tileCentralDirectoryEntry;
        }
    }


    @Override
    public RootPathInfo preCheckZipAndGetRoot(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException {
        AtomicReference<String> tileSetPath = new AtomicReference<>("");
        iCompressionHandler.scanAllEntries(layerConfigContext.getObjectKey(), (centralDirectoryEntry, allCount, currentCount) -> {
            boolean directoryIs = centralDirectoryEntry.isDirectoryIs();
            if (directoryIs) {
                return true;
            }
            String name = centralDirectoryEntry.getName();
            if (name.toLowerCase().contains("layer.json")) {
                tileSetPath.set(name);
                return false;
            }
            return true;
        });
        String tileSetJsonPath = tileSetPath.get();

        if (StrUtil.isEmpty(tileSetJsonPath)) {
            throw new RuntimeException("三维地形中缺失layer.json关键元素");
        }
        String rootPath = tileSetJsonPath.replace("layer.json", "");
        return RootPathInfo.of().setRootFileName("layer.json").setRootPath(rootPath);
    }
}
