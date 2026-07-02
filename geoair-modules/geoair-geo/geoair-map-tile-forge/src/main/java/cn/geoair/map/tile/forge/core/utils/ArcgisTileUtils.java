package cn.geoair.map.tile.forge.core.utils;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import cn.geoair.map.tile.forge.core.zip.ICompressionHandler;
import cn.geoair.map.tile.forge.core.zip.cache.LayerPerFileDao;
import cn.geoair.map.tile.forge.core.zip.cache.TileCentralDirectoryEntry;
import cn.geoair.map.tile.forge.core.zip.model.CentralDirectoryEntry;
import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/2 10:26
 * @description： arcgis瓦片的一些特殊处理
 */
public class ArcgisTileUtils {

    // 配置文件名称常量
    private static final String CONF_XML = "Conf.xml";
    private static final String CONF_CDI = "conf.cdi";


    public static String getConfigXmlByZip(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws Exception {
        return getConfigFromZip(layerConfigContext, iCompressionHandler, CONF_XML);
    }

    public static String getConfigCdiByZip(GirLayerConfigContext layerConfigContext, ICompressionHandler iCompressionHandler) throws Exception {
        return getConfigFromZip(layerConfigContext, iCompressionHandler, CONF_CDI);
    }

    public static String getConfigXmlByS3(GirLayerConfigContext layerConfigContext) throws Exception {
        return getConfigFromS3(layerConfigContext, CONF_XML);
    }

    public static String getConfigCdiByS3(GirLayerConfigContext layerConfigContext) throws Exception {
        return getConfigFromS3(layerConfigContext, CONF_CDI);
    }

    public static String getConfigXmlByLocal(GirLayerConfigContext layerConfigContext) throws Exception {
        return getConfigFromLocal(layerConfigContext, CONF_XML);
    }

    public static String getConfigCdiByLocal(GirLayerConfigContext layerConfigContext) throws Exception {
        return getConfigFromLocal(layerConfigContext, CONF_CDI);
    }

    // ==================== 统一的核心处理方法 ====================

    /**
     * 从ZIP中获取配置文件内容
     */
    private static String getConfigFromZip(GirLayerConfigContext layerConfigContext,
                                           ICompressionHandler iCompressionHandler,
                                           String confFileName) throws Exception {
        String tempDirAbsolutePath = TileTempPathConfig.getInstance().buildLocalTempDirPath(layerConfigContext);
        File tempConfFile = FileUtil.file(tempDirAbsolutePath, confFileName);

        // 如果临时目录中不存在配置文件，则从zip中解压
        if (!FileUtil.exist(tempConfFile)) {
            extractConfigFromZip(layerConfigContext, tempDirAbsolutePath, confFileName, iCompressionHandler);
        }

        return readFileContent(tempConfFile);
    }

    /**
     * 从S3中获取配置文件内容
     */
    private static String getConfigFromS3(GirLayerConfigContext layerConfigContext, String confFileName) {
        String tempDirAbsolutePath = TileTempPathConfig.getInstance().buildLocalTempDirPath(layerConfigContext);
        String s3FilePath = layerConfigContext.getTilePathPrefix() + "/" + confFileName;

        // 从S3下载配置文件
        S3ClientGetter.getInstance().downloadFromS3IfNeeded(
                layerConfigContext.getObjectKey(),
                s3FilePath,
                tempDirAbsolutePath
        );

        File tempConfFile = FileUtil.file(tempDirAbsolutePath, confFileName);
        return readFileContent(tempConfFile);
    }

    /**
     * 从本地文件系统中获取配置文件内容
     */
    private static String getConfigFromLocal(GirLayerConfigContext layerConfigContext, String confFileName) {
        String rootPath = layerConfigContext.getObjectKey();
        File file = FileUtil.file(rootPath);
        String absolutePath = file.getParentFile().getAbsolutePath();
        String configFilePath = absolutePath + File.separator + confFileName;

        return readFileContent(FileUtil.file(configFilePath));
    }

    // ==================== ZIP解压相关私有方法 ====================

    /**
     * 从ZIP中提取配置文件到临时目录
     */
    private static void extractConfigFromZip(GirLayerConfigContext layerConfigContext,
                                             String tempDirAbsolutePath,
                                             String confFileName,
                                             ICompressionHandler iCompressionHandler) throws IOException {
        GirLayerConfigContextHelper helper = GirLayerConfigContextHelper.getInstance();

        try (LayerPerFileDao layerPerFileDao = helper.getLayerPerFileDao(layerConfigContext)) {
            boolean cacheEnabled = layerPerFileDao.cacheEnableIs(layerConfigContext);

            // 优先从缓存中获取，如果缓存未命中则从ZIP中获取
            if (cacheEnabled) {
                boolean extractedFromCache = extractFromCache(layerPerFileDao, layerConfigContext,
                        tempDirAbsolutePath, confFileName, iCompressionHandler);
                if (extractedFromCache) {
                    return;
                }
            }

            // 从ZIP中直接提取
            extractFromZipDirectly(layerConfigContext, tempDirAbsolutePath, confFileName, iCompressionHandler);
        } catch (Exception e) {
            throw new RuntimeException("提取配置文件失败: " + confFileName, e);
        }
    }

    /**
     * 从缓存中提取配置文件
     */
    private static boolean extractFromCache(LayerPerFileDao layerPerFileDao,
                                            GirLayerConfigContext layerConfigContext,
                                            String tempDirAbsolutePath,
                                            String confFileName,
                                            ICompressionHandler iCompressionHandler) throws IOException, SQLException {
        TileCentralDirectoryEntry entry = layerPerFileDao.findByFileName(confFileName);
        if (entry != null) {
            String targetPath = tempDirAbsolutePath + File.separator + confFileName;
            iCompressionHandler.readAndDecompressEntryToLocal(entry, layerConfigContext.getObjectKey(), targetPath);
            return true;
        }
        return false;
    }

    /**
     * 直接从ZIP中提取配置文件
     */
    private static void extractFromZipDirectly(GirLayerConfigContext layerConfigContext,
                                               String tempDirAbsolutePath,
                                               String confFileName,
                                               ICompressionHandler iCompressionHandler) throws IOException {
        String objectKey = layerConfigContext.getObjectKey();
        CentralDirectoryEntry entry = findEntryInZip(iCompressionHandler, objectKey, confFileName);

        if (entry == null) {
            throw new RuntimeException("无法找到配置文件: " + confFileName);
        }

        String targetPath = tempDirAbsolutePath + File.separator + confFileName;
        iCompressionHandler.readAndDecompressEntryToLocal(entry, objectKey, targetPath);
    }

    /**
     * 在ZIP中查找指定的配置文件条目
     */
    private static CentralDirectoryEntry findEntryInZip(ICompressionHandler iCompressionHandler,
                                                        String objectKey,
                                                        String confFileName) throws IOException {
        final CentralDirectoryEntry[] foundEntry = {null};
        final String lowerCaseFileName = confFileName.toLowerCase();

        iCompressionHandler.scanAllEntries(objectKey, (centralDirectoryEntry, allCount, currentCount) -> {
            if (!centralDirectoryEntry.isDirectoryIs()) {
                String name = centralDirectoryEntry.getName();
                if (name.toLowerCase().contains(lowerCaseFileName)) {
                    foundEntry[0] = centralDirectoryEntry;
                    return false; // 找到后停止扫描
                }
            }
            return true;
        });

        return foundEntry[0];
    }


    /**
     * 读取文件内容，如果文件不存在则返回null
     */
    private static String readFileContent(File file) {
        if (FileUtil.exist(file)) {
            return FileUtil.readUtf8String(file);
        }
        return null;
    }

}
