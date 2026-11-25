package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.map.tile.forge.core.zip.ZipRootResolver;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.AbstractZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.utils.TilePathParser;
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
 * 提供从ZIP压缩包中读取MvtTiles瓦片数据的功能
 *
 * @author 张俊
 * @since 2025/11/17
 */

public class LocalZipMvtTilesStorageSupport extends LocalZip3DTileStorageSupport {
    public static GiLogger log = GirLoggerFactory.getLogger();

    public LocalZipMvtTilesStorageSupport(GirLayerConfigContextHelper contextHelper) {
        super(contextHelper);
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
            return tileCentralDirectoryEntry;
        }
    }


    @Override
    public RootPathInfo preCheckZipAndGetRoot(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException {
        return ZipRootResolver.resolve(layerConfigContext, iCompressionHandler);
    }
}
