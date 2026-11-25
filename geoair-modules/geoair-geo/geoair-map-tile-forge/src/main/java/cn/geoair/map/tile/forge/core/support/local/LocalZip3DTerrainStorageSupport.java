package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.AbstractZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.utils.TilePathParser;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.LocalCompressionHandler;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.ZipPaths;
import cn.geoair.map.tile.forge.core.zip.ZipRootResolver;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.cache.ZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

import java.io.IOException;

/**
 * 本地ZIP瓦片存储支持类 提供从ZIP压缩包中读取三维地形瓦片数据的功能
 *
 * @author 张俊
 * @since 2025/11/17
 */
public class LocalZip3DTerrainStorageSupport extends AbstractZipDirectoryGetter
        implements ZipDirectoryGetter, ITileStorageSupport {
    public static GiLogger log = GirLoggerFactory.getLogger();

    public LocalZip3DTerrainStorageSupport(GirLayerConfigContextHelper contextHelper) {
        super(contextHelper);
    }

    /** 压缩处理器实例，用于处理ZIP文件的解压缩操作 */
    protected ICompressionHandler compressionHandler = null;

    /**
     * 获取压缩处理器实例 使用懒加载单例模式，确保只有一个压缩处理器实例存在
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
    public TileRequest getTileData(
            GirLayerConfigContext layerConfigContext, String z, String x, String y)
            throws Exception {
        String relative =
                StrUtil.isEmpty(x) && StrUtil.isEmpty(y)
                        ? z
                        : ZipPaths.number(z)
                                + "/"
                                + ZipPaths.number(x)
                                + "/"
                                + ZipPaths.number(y)
                                + "."
                                + ZipPaths.format(
                                        layerConfigContext.getFormat() == null
                                                ? "terrain"
                                                : layerConfigContext.getFormat());
        return zipResources.readTile(layerConfigContext, relative);
    }

    protected boolean byPreCache(
            GirLayerConfigContext layerConfigContext,
            String z,
            String y,
            String x,
            String inLocalPath) {
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
            getICompressionHandler()
                    .readAndDecompressEntryToLocal(
                            tileCentralDirectoryEntry,
                            layerConfigContext.getObjectKey(),
                            inLocalPath);
            return true;
        } catch (Exception e) {
            log.error("getTileDataByPreZipCache error:", e);
            return false;
        }
    }

    @Override
    public void preCacheTiles(
            GirLayerConfigContext layerConfigContext,
            TileCache tileCache,
            ProgressConsumer progressConsumer) {
        this.preCacheCentralDir(layerConfigContext, ListUtil.of(progressConsumer));
    }

    @Override
    public TileCentralDirectoryModel tranToTileModel(CentralDirectoryModel centralDirectoryModel) {
        TileCentralDirectoryModel tileCentralDirectoryEntry = new TileCentralDirectoryModel();
        BeanUtil.copyProperties(centralDirectoryModel, tileCentralDirectoryEntry);
        tileCentralDirectoryEntry.setId(IdUtil.getSnowflakeNextId());
        tileCentralDirectoryEntry.setFileName(centralDirectoryModel.getName());

        TilePathParser.XyzTileInfo xyzTileInfo =
                TilePathParser.parseXyzPath(centralDirectoryModel.getName());
        if (xyzTileInfo == null) {
            return tileCentralDirectoryEntry;
        } else {
            tileCentralDirectoryEntry.setY(xyzTileInfo.getY() + "");
            tileCentralDirectoryEntry.setX(xyzTileInfo.getX() + "");
            tileCentralDirectoryEntry.setZ(xyzTileInfo.getZ() + "");
            tileCentralDirectoryEntry.setFileName(
                    xyzTileInfo.getZ() + "/" + xyzTileInfo.getX() + "/" + xyzTileInfo.getY());
            return tileCentralDirectoryEntry;
        }
    }

    @Override
    public RootPathInfo preCheckZipAndGetRoot(
            GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler)
            throws IOException {
        return ZipRootResolver.resolve(layerConfigContext, iCompressionHandler);
    }
}
