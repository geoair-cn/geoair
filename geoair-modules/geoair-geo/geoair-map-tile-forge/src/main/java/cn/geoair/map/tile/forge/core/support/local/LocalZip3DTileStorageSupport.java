package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.base.exception.GirException;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.support.AbstractZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.vo.TileRequest;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.LocalCompressionHandler;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryModel;
import cn.geoair.map.tile.forge.core.zip.cache.ZipDirectoryGetter;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryModel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * 本地ZIP瓦片存储支持类
 * 提供从ZIP压缩包中读取3DTiles瓦片数据的功能
 *
 * @author 张俊
 * @since 2025/11/17
 */
@Slf4j
public class LocalZip3DTileStorageSupport extends AbstractZipDirectoryGetter implements ZipDirectoryGetter, ITileStorageSupport {

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
        String inLocalPathBuilder = tempDirAbsolutePath + File.separator + fileName;
        String inLocalPath = inLocalPathBuilder.trim();
        File localTileFile = new File(inLocalPath);
        boolean localStatu = localTileFile.exists();
        if (!localStatu) {
            localStatu = byPreCache(layerConfigContext, fileName, inLocalPath);
        }
        if (localStatu) {
            tileRequest.setBytes(FileUtil.readBytes(localTileFile));
            tileRequest.setLastModified(localTileFile.lastModified());
            tileRequest.setSize(localTileFile.length());
            tileRequest.setExists(true);
            Optional<MediaType> mediaType = MediaTypeFactory.getMediaType(localTileFile.getName());
            MediaType mediaType1 = mediaType.orElse(MediaType.APPLICATION_OCTET_STREAM);
            tileRequest.mimeTypeByType(mediaType1);
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
    public String preCheckZip(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException {
        // 存储所有找到的tileset.json路径
        List<String> allTileSetPaths = new ArrayList<>();
        GirMapTileType mapTileType = layerConfigContext.getMapTileType();
        String rootFileName = "tileset.json";
        if (mapTileType == GirMapTileType.S3M) {
            rootFileName = "tilesets3mb.scp";
        } else {
            rootFileName = "tileset.json";
        }
        // 扫描ZIP中所有条目，收集所有tileset.json路径
        String finalRootFileName = rootFileName;
        iCompressionHandler.scanAllEntries(layerConfigContext.getObjectKey(), (centralDirectoryEntry, allCount, currentCount) -> {
            // 跳过目录
            if (centralDirectoryEntry.isDirectoryIs()) {
                return true;
            }
            String entryName = centralDirectoryEntry.getName();
            // 匹配tileset.json（不区分大小写）
            String lowerCase = entryName.toLowerCase();
            if (lowerCase.contains(finalRootFileName)) {
                allTileSetPaths.add(entryName);
                log.info("发现{}路径: {}", finalRootFileName, entryName);
//                return false;  这里不停止的原因是 每个层级都有tileset.json，所以要拿到所有的tileset.json，在判断最外面的根
            }
            // 继续扫描所有条目（不提前终止）
            return true;
        });

        // 校验是否找到tileset.json
        if (allTileSetPaths.isEmpty()) {
            throw new GirException("三维数据中缺失{}关键元素", rootFileName);
        }

        // 筛选最外层的tileset.json（路径层级最少）
        String outerMostTileSetPath = findOuterMostPath(allTileSetPaths);
        log.info("选中最外层的tileset.json路径: {}", outerMostTileSetPath);

        // 提取根路径（移除tileset.json文件名）
        return outerMostTileSetPath.replace(rootFileName, "");
    }

    /**
     * 从多个路径中找到层级最少（最外层）的路径
     *
     * @param paths 所有tileset.json的路径列表
     * @return 最外层路径
     */
    private String findOuterMostPath(List<String> paths) {
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
