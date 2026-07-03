package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.base.exception.GirException;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
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

    @Override
    public RootPathInfo preCheckZipAndGetRoot(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException {
        // 存储所有找到的tileset.json路径
        List<String> allTileSetPaths = new ArrayList<>();
        GirMapTileType mapTileType = layerConfigContext.getMapTileType();
        String rootFileNameSuffix = "scp";
        // 扫描ZIP中所有条目，收集所有tileset.json路径
        String finalRootFileNameSuffix = rootFileNameSuffix;
        iCompressionHandler.scanAllEntries(layerConfigContext.getObjectKey(), (centralDirectoryEntry, allCount, currentCount) -> {
            // 跳过目录
            if (centralDirectoryEntry.isDirectoryIs()) {
                return true;
            }
            String entryName = centralDirectoryEntry.getName();
            // 匹配tileset.json（不区分大小写）
            String suffix = FileUtil.getSuffix(entryName);
            if (suffix.equals(finalRootFileNameSuffix)) {
                allTileSetPaths.add(entryName);
                log.info("发现{}路径: {}", finalRootFileNameSuffix, entryName);
//                return false;  这里不停止的原因是 每个层级都有tileset.json，所以要拿到所有的tileset.json，在判断最外面的根
            }
            // 继续扫描所有条目（不提前终止）
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
        return RootPathInfo.of().setRootFileName(name).setRootPath(rootPath);
    }


}
