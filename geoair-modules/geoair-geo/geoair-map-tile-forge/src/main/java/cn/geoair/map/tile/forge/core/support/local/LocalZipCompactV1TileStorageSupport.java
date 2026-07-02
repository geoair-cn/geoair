package cn.geoair.map.tile.forge.core.support.local;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCache;
import cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCacheV1;
import cn.geoair.map.tile.forge.core.bygwc.compact.BundleFileResource;
import cn.geoair.map.tile.forge.core.cache.TileCache;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.support.arcgis.ConfigXmlGetterZip;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.utils.TilePathParser;
import cn.geoair.map.tile.forge.core.vo.TileRequest;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.LocalCompressionHandler;
import cn.geoair.map.tile.forge.core.zip.ProgressConsumer;
import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryEntry;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryEntry;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCache.BUNDLE_EXT;
import static cn.geoair.map.tile.forge.core.bygwc.compact.ArcGISCompactCache.BUNDLX_EXT;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/13 17:59
 * &#064;description：本地ZIP压缩V1版本瓦片存储支持类，用于处理ArcGIS紧凑型缓存V1格式的瓦片数据读取
 */
@Slf4j
public class LocalZipCompactV1TileStorageSupport extends ConfigXmlGetterZip {

    protected ICompressionHandler compressionHandler = null;


    protected ICompressionHandler getICompressionHandler() {
        if (compressionHandler == null) {
            compressionHandler = new LocalCompressionHandler();
        }
        return compressionHandler;
    }


    @Override
    public TileRequest getTileData(GirLayerConfigContext layerConfigContext, String z, String x, String y) throws Exception {
        // 初始化瓦片请求对象
        TileRequest tileRequest = TileRequest.emptyByContext(layerConfigContext);
        try {
            // 创建缓存访问器，用于构建文件路径
            String tempDirAbsolutePath = TileTempPathConfig.getInstance().buildLocalTempDirPath(layerConfigContext);
            GirLayerConfigContextHelper instance = GirLayerConfigContextHelper.getInstance();
            LayerPerFileDao layerPerFileDao = instance.getLayerPerFileDao(layerConfigContext);
            if (!layerPerFileDao.cacheEnableIs(layerConfigContext)) {
                byZip(layerConfigContext, z, y, x, tempDirAbsolutePath);
            } else {
                byPreCache(layerConfigContext, z, y, x, tempDirAbsolutePath);
            }
            // 创建本地缓存访问器，从解压后的文件中读取瓦片数据
            String tilePathPrefix = layerConfigContext.getTilePathPrefix();
            String rootPath = tempDirAbsolutePath;
            if (!StrUtil.isEmpty(tilePathPrefix)) {
                rootPath = rootPath + File.separator + tilePathPrefix;
            }
            ArcGISCompactCache localCache = getArcGISCompactCache(rootPath);
            BundleFileResource bundleFileResource = localCache.getBundleFileResource(Integer.parseInt(z), Integer.parseInt(y), Integer.parseInt(x));

            // 如果找到瓦片资源，设置返回数据
            if (bundleFileResource != null) {
                long size = bundleFileResource.getSize();
                tileRequest.setExists(true);
                tileRequest.setBytes(IoUtil.readBytes(bundleFileResource.getInputStream()));
                tileRequest.setSize(size);
                tileRequest.setLastModified(bundleFileResource.getLastModified());
            }
        } catch (Exception e) {
            log.info("getTileData error:{}", e.getMessage());
        }


        return tileRequest;
    }

    ArcGISCompactCache getArcGISCompactCache(String pathToCacheRoot) {
        return new ArcGISCompactCacheV1(pathToCacheRoot);
    }

    protected void byZip(GirLayerConfigContext layerConfigContext, String z, String y, String x, String tempDirAbsolutePath) throws IOException {
        ArcGISCompactCache arcGISCompactCache = getArcGISCompactCache(layerConfigContext.getTilePathPrefix());
        String filePath = arcGISCompactCache.buildBundleFilePath(Integer.parseInt(z), Integer.parseInt(y), Integer.parseInt(x));
        boolean b = zipBundleFileToLocal(layerConfigContext, filePath, tempDirAbsolutePath, BUNDLX_EXT);
        if (!b) {
            // 上一个不存在，第二个肯定也是不存在的
            return;
        }
        zipBundleFileToLocal(layerConfigContext, filePath, tempDirAbsolutePath, BUNDLE_EXT);
    }

    protected void byPreCache(GirLayerConfigContext layerConfigContext, String z, String y, String x, String tempDirAbsolutePath) throws IOException {
        ArcGISCompactCache arcGISCompactCache = getArcGISCompactCache(File.separator);
        String filePath = arcGISCompactCache.buildBundleFilePath(Integer.parseInt(z), Integer.parseInt(y), Integer.parseInt(x));
        boolean b = preCacheBundleFileToLocal(layerConfigContext, filePath, tempDirAbsolutePath, BUNDLX_EXT);
        if (!b) {
            return;
        }
//        preCacheBundleFileToLocal(layerConfigContext, filePath, tempDirAbsolutePath, BUNDLE_EXT);
    }

    protected boolean zipBundleFileToLocal(GirLayerConfigContext layerConfigContext, String filePath, String tempDirAbsolutePath, String fileExt) throws IOException {
        boolean b = preCacheBundleFileToLocal(layerConfigContext, filePath, tempDirAbsolutePath, fileExt);
        if (b) {  // 虽然走到这个方法的时候，肯定cacheEnableIs是false，但是不排除已经存在部分缓存了，这里先去缓存里面试探一下。
            return true;
        }
        String pathToBundleFile = filePath.replaceFirst("^\\\\+", "") + fileExt;
        // 检查并解压 .bundle 文件到临时目录
        File tempBundleFile = FileUtil.file(tempDirAbsolutePath + File.separator + pathToBundleFile);
        if (!FileUtil.exist(tempBundleFile)) {
            // ZIP文件内路径统一使用正斜杠
            String normalizedPathToBundleFile = pathToBundleFile.replace('\\', '/');
            try {
                getICompressionHandler().readFileFromZipToLocal(layerConfigContext.getObjectKey(), normalizedPathToBundleFile, tempBundleFile.getAbsolutePath());
            } catch (Exception e) {
                log.error(e.getMessage());
                return false;
            }
            return true;
        } else {
            return true;
        }
    }

    protected boolean preCacheBundleFileToLocal(GirLayerConfigContext layerConfigContext, String filePath, String tempDirAbsolutePath, String fileExt) {
        GirLayerConfigContextHelper instance = GirLayerConfigContextHelper.getInstance();
        String pathToBundleFile = null;
        if (filePath.startsWith(File.separator)) {
            String replaceFirst = StrUtil.replaceFirst(filePath, File.separator, "");
            pathToBundleFile = replaceFirst + fileExt;
        }else{
            pathToBundleFile = filePath+ fileExt;
        }
        // 检查并解压 .bundle 文件到临时目录

        File tempBundleFile = FileUtil.file(tempDirAbsolutePath + File.separator + (layerConfigContext.getTilePathPrefix() != null ? layerConfigContext.getTilePathPrefix() + File.separator : "") + pathToBundleFile);

        if (FileUtil.exist(tempBundleFile)) {
            return true;
        }
        try (LayerPerFileDao layerPerFileDao = instance.getLayerPerFileDao(layerConfigContext)) {
            boolean b = layerPerFileDao.cacheEnableIs(layerConfigContext);
            if (b) {
                String replace = pathToBundleFile.replace("\\", "/");
                TileCentralDirectoryEntry zipDirectoryByFileName = layerPerFileDao.findByFileName(replace);
                if (zipDirectoryByFileName == null) {
                    return false;
                }
                getICompressionHandler().readAndDecompressEntryToLocal(zipDirectoryByFileName, layerConfigContext.getObjectKey(), tempBundleFile.getAbsolutePath());
            } else {
                return false;
            }
        } catch (Exception e) {

            log.error("getTileDataByPreZipCache error:", e);
            return false;
        }
        return true;
    }

    @Override
    public TileCentralDirectoryEntry getTileCentralDirectoryEntry(CentralDirectoryEntry centralDirectoryEntry) {
        if (centralDirectoryEntry.isDirectoryIs()) {
            return null;
        }
        TileCentralDirectoryEntry tileCentralDirectoryEntry = new TileCentralDirectoryEntry();
        BeanUtil.copyProperties(centralDirectoryEntry, tileCentralDirectoryEntry);
        String name = centralDirectoryEntry.getName();
        String subBundlePath = TilePathParser.getSubBundlePath(name);
        if (subBundlePath == null) {
            return null;
        }
        tileCentralDirectoryEntry.setFileName(subBundlePath);
        tileCentralDirectoryEntry.setStorageType("COMPACT");
        return tileCentralDirectoryEntry;
    }

    @Override
    public void preCacheTiles(GirLayerConfigContext layerConfigContext, TileCache tileCache, ProgressConsumer progressConsumer) {
        this.initTileCentralDirectoryEntryDao(layerConfigContext, ListUtil.of(progressConsumer));
        GirLayerConfigContextHelper instance = GirLayerConfigContextHelper.getInstance();
        LayerPerFileDao layerPerFileDao = instance.getLayerPerFileDao(layerConfigContext);
        // 参数校验
        if (layerConfigContext == null ) {
            throw new IllegalArgumentException("layerConfigDto 不能为空");
        }

        ICompressionHandler iCompressionHandler = getICompressionHandler();
        String objectKey = layerConfigContext.getObjectKey();
        try {
            layerPerFileDao.findAll(tileCentralDirectoryEntry -> {
//                Integer z = tileCentralDirectoryEntry.getZAsInt(), x = tileCentralDirectoryEntry.getXAsInt(), y = tileCentralDirectoryEntry.getYAsInt();
//                TileRequest tileRequest = getTileRequest(layerConfigDto );
//                String cacheKey = tileCache.buildTileCacheKey(layerConfigDto.getLayerName(), z, y, x);
//                getExecutor().submit(() -> {
//                    try {
//                        byte[] bytes = iCompressionHandler.readAndDecompressEntry(tileCentralDirectoryEntry, objectKey);
//                        tileRequest.setBytes(bytes);
//                        tileRequest.setLastModified(System.currentTimeMillis());
//                        tileRequest.setSize(bytes.length);
//                        tileRequest.setExists(true);
//                        tileRequest.mimeTypeByType(MediaType.IMAGE_PNG);
//                        tileCache.putTile(cacheKey, tileRequest);
//                    } catch (Exception e) {
//                        log.error("preCacheTiles error:{}", e.getMessage());
//                    }
//                });
            });

        } catch (Exception e) {


        }

    }

    @Override
    protected   String preCheckZip(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws IOException {
        AtomicReference<String> tileSetPath = new AtomicReference<>("");
        iCompressionHandler.scanAllEntries(layerConfigContext.getObjectKey(), (centralDirectoryEntry, allCount, currentCount) -> {
            boolean directoryIs = centralDirectoryEntry.isDirectoryIs();
            if (directoryIs) {
                return true;
            }
            String name = centralDirectoryEntry.getName();
            String confFileName = "conf.xml";
            if (name.toLowerCase().contains(confFileName)) {
                tileSetPath.set(name);
                return false;
            }
            return true;
        });
        String tileSetJsonPath = tileSetPath.get();

        if(StrUtil.isEmpty(tileSetJsonPath)){
            throw  new RuntimeException("arcGis紧凑型缺失conf.xml文件，校验失败！");
        }
        String rootPath = tileSetJsonPath.replace("conf.xml", "")
                .replace("Conf.xml", "");
        return rootPath;
    }

}
