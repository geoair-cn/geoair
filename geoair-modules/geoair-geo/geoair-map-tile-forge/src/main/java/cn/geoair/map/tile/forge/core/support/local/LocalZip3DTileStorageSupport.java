package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.base.exception.GirException;
import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.support.AbstractZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;
import cn.geoair.web.util.GutilMimeType;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;

import cn.hutool.core.util.StrUtil;

import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.LocalCompressionHandler;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.cache.ZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


/**
 * 本地ZIP瓦片存储支持类
 * 提供从ZIP压缩包中读取3DTiles瓦片数据的功能
 *
 * @author 张俊
 * @since 2025/11/17
 */

public class LocalZip3DTileStorageSupport extends AbstractZipDirectoryGetter implements ZipDirectoryGetter, ITileStorageSupport {
    public static GiLogger log = GirLoggerFactory.getLogger();
    public LocalZip3DTileStorageSupport(GirLayerConfigContextHelper contextHelper) {
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
    public TileRequest getTileData(GirLayerConfigContext layerConfigContext, String fileName, String x, String y) throws Exception {

        // 3Dtile就Z有用，其他的我全部都不要了。z就是一个web请求路径

        TileRequest tileRequest = TileRequest.emptyByContext(layerConfigContext);
        String tempDirAbsolutePath = TileTempPathConfig.getInstance().buildLocalTempDirPath(layerConfigContext);
        Path rootPath = Paths.get(tempDirAbsolutePath).toAbsolutePath().normalize();
        Path localPath = rootPath.resolve(fileName).normalize();
        if (!localPath.startsWith(rootPath)) {
            throw new IOException("瓦片路径越出临时目录");
        }
        File localTileFile = localPath.toFile();
        String inLocalPath = localTileFile.getAbsolutePath();
        boolean localStatu = localTileFile.exists();
        if (!localStatu) {
            localStatu = byPreCache(layerConfigContext, fileName, inLocalPath);
        }
        if (localStatu) {
            tileRequest.setBytes(FileUtil.readBytes(localTileFile));
            tileRequest.setLastModified(localTileFile.lastModified());
            tileRequest.setSize(localTileFile.length());
            tileRequest.setExists(true);
            tileRequest.setMimeType(GutilMimeType.fromExtension(FileUtil.getSuffix(localTileFile.getName())));
        }
        return tileRequest;
    }

    protected boolean byPreCache(GirLayerConfigContext layerConfigContext, String z, String inLocalPath) {
        try {
            TileCentralDirectoryModel zipDirectoryByFileName = getZipDirectoryBFileName(layerConfigContext, z);
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
        this.preCacheCentralDir(layerConfigContext, ListUtil.of(progressConsumer));
    }


    @Override
    public TileCentralDirectoryModel tranToTileModel(CentralDirectoryModel centralDirectoryModel) {
        TileCentralDirectoryModel tileCentralDirectoryEntry = new TileCentralDirectoryModel();
        BeanUtil.copyProperties(centralDirectoryModel, tileCentralDirectoryEntry);
        tileCentralDirectoryEntry.setId(IdUtil.getSnowflakeNextId());
        tileCentralDirectoryEntry.setFileName(centralDirectoryModel.getName());
        return tileCentralDirectoryEntry;
    }


    @Override
    public RootPathInfo preCheckZipAndGetRoot(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException {
        GirMapTileType mapTileType = layerConfigContext.getMapTileType();
        List<String> allTileSetPaths = new ArrayList<>();
        String finalRootFileNameSuffix = "json";
        if (mapTileType.equals(GirMapTileType.S3M)) {
            finalRootFileNameSuffix = "scp";
        } else {
            finalRootFileNameSuffix = "json";
        }

        String finalRootFileNameSuffix1 = finalRootFileNameSuffix;
        iCompressionHandler.scanAllEntries(layerConfigContext.getObjectKey(), (centralDirectoryEntry, allCount, currentCount) -> {
            // 跳过目录
            if (centralDirectoryEntry.isDirectoryIs()) {
                return true;
            }
            String entryName = centralDirectoryEntry.getName();
            String suffix = FileUtil.getSuffix(entryName);
            if (suffix.equals(finalRootFileNameSuffix1)) {
                allTileSetPaths.add(entryName);
            }
            return true;
        });

        // 校验是否找到tileset.json
        if (allTileSetPaths.isEmpty()) {
            throw new GirException("三维数据中缺失{}关键元素", finalRootFileNameSuffix);
        }
        String outerMostTileSetPath = findOuterMostPath(allTileSetPaths);
        log.info("zip中找到{}，判断为合法的三维数据：{}", outerMostTileSetPath);
        log.info("选中最外层的路径: {}", outerMostTileSetPath);
        String name = FileUtil.getName(outerMostTileSetPath);
        String rootPath = outerMostTileSetPath.replace(name, "");
        return RootPathInfo.of().setRootFileName(name).setRootPath(rootPath).setRootFilePath(outerMostTileSetPath)
                .setRootFileStandardName(mapTileType.equals(GirMapTileType.S3M) ? "tilesetS3MB.scp" : "tileset.json");
    }

    /**
     * 从多个路径中找到层级最少（最外层）的路径
     *
     * @param paths 所有tileset.json的路径列表
     * @return 最外层路径
     */
    protected String findOuterMostPath(List<String> paths) {
        // 初始化最外层路径为第一个元素
        String outerMost = paths.get(0);
        int minLevel = getPathLevel(outerMost);

        // 遍历所有路径，找到层级最少的
        for (String path : paths) {
            int currentLevel = getPathLevel(path);
            // 如果当前路径层级更少，更新最外层路径
            if (currentLevel < minLevel) {
                minLevel = currentLevel;
                outerMost = path;
            }
            // 若层级相同，优先选择路径更短的（避免冗余目录）
            else if (currentLevel == minLevel && path.length() < outerMost.length()) {
                outerMost = path;
            }
        }
        return outerMost;
    }

    /**
     * 计算路径的层级（以/为分隔符）
     * 示例：
     * - tileset.json → 0层
     * - dir/tileset.json → 1层
     * - dir/sub/tileset.json → 2层
     */
    private int getPathLevel(String path) {
        if (StrUtil.isEmpty(path)) {
            return 0;
        }
        // 统一路径分隔符（兼容Windows\和Linux/）
        path = path.replace("\\", "/");
        // 统计路径中的/数量（层级 = /的数量）
        int level = 0;
        for (char c : path.toCharArray()) {
            if (c == '/') {
                level++;
            }
        }
        // 特殊处理：如果路径以/结尾（如dir/），层级减1（避免多算）
        if (path.endsWith("/")) {
            level--;
        }
        return level;
    }


}
