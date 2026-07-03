package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.base.exception.GirException;
import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.model.RootPathInfo;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * 本地ZIP瓦片存储支持类
 * 提供从ZIP压缩包中读取s3m瓦片数据的功能
 *
 * @author 张俊
 * @since 2025/11/17
 */
@Slf4j
public class LocalZipS3MStorageSupport extends LocalZip3DTileStorageSupport {
    public LocalZipS3MStorageSupport(GirLayerConfigContextHelper contextHelper) {
        super(contextHelper);
    }

    @Override
    public RootPathInfo preCheckZipAndGetRoot(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException {
        List<String> allTileSetPaths = new ArrayList<>();
        String finalRootFileNameSuffix = "scp";
        iCompressionHandler.scanAllEntries(layerConfigContext.getObjectKey(), (centralDirectoryEntry, allCount, currentCount) -> {
            // 跳过目录
            if (centralDirectoryEntry.isDirectoryIs()) {
                return true;
            }
            String entryName = centralDirectoryEntry.getName();
            String suffix = FileUtil.getSuffix(entryName);
            if (suffix.equals(finalRootFileNameSuffix)) {
                allTileSetPaths.add(entryName);
                log.info("发现{}路径: {}", finalRootFileNameSuffix, entryName);
            }
            return true;
        });

        // 校验是否找到tileset.json
        if (allTileSetPaths.isEmpty()) {
            throw new GirException("三维数据中缺失{}关键元素", finalRootFileNameSuffix);
        }
        String outerMostTileSetPath = findOuterMostPath(allTileSetPaths);
        log.info("选中最外层的tileset.json路径: {}", outerMostTileSetPath);
        String name = FileUtil.getName(outerMostTileSetPath);
        String rootPath = outerMostTileSetPath.replace(name, "");
        return RootPathInfo.of().setRootFileName(name).setRootPath(rootPath).setRootFilePath(outerMostTileSetPath).setRootFileExtension("scp");
    }


}
