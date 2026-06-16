package cn.geoair.map.tile.forge.core.support;

import cn.geoair.map.tile.forge.core.config.TileTempPathConfig;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import cn.hutool.core.io.FileUtil;

import java.io.File;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/17 10:16
 * &#064;description：S3配置文件获取器抽象类，用于从S3存储中获取瓦片服务的配置XML文件
 */
public abstract class ConfigXmlGetterS3 extends AbstractTileStorageSupport {


    /**
     * 从S3存储中获取指定图层的配置XML文件内容
     *
     * @param layerConfigContext 图层配置信息对象，包含访问S3所需的信息
     * @return 配置文件的XML内容字符串
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    @Override
    public String getConfigXml(GirLayerConfigContext layerConfigContext) throws Exception {
        // 构建本地临时目录路径，用于缓存从S3下载的配置文件
        String tempDirAbsolutePath = TileTempPathConfig.getInstance().buildLocalTempDirPath(layerConfigContext);
        // 配置文件在S3中的路径
        String confFileName = "Conf.xml";
        return getString(layerConfigContext, confFileName, tempDirAbsolutePath);
    }

    @Override
    public String getConfigCdi(GirLayerConfigContext layerConfigContext) throws Exception {
        // 构建本地临时目录路径，用于缓存从S3下载的配置文件
        String tempDirAbsolutePath = TileTempPathConfig.getInstance().buildLocalTempDirPath(layerConfigContext);
        // 配置文件在S3中的路径
        String confFileName = "conf.cdi";
        return getString(layerConfigContext, confFileName, tempDirAbsolutePath);
    }


    private static String getString(GirLayerConfigContext layerConfigContext, String confFileName, String tempDirAbsolutePath) {

        // 如果临时目录中不存在Conf.xml，则从S3中下载
        // 通过图层配置中的objectKey和远程配置文件路径确定S3中的具体位置
        S3ClientGetter.getInstance().downloadFromS3IfNeeded(layerConfigContext.getObjectKey(), layerConfigContext.getTilePathPrefix()+"/"+confFileName, tempDirAbsolutePath);

        // 读取配置文件内容并返回
        // 构造临时配置文件的完整路径
        File tempConfFile = FileUtil.file(tempDirAbsolutePath + File.separator + confFileName);
        // 检查配置文件是否存在
        if (FileUtil.exist(tempConfFile)) {
            // 存在则读取文件内容并以UTF-8编码返回
            return FileUtil.readUtf8String(tempConfFile);
        }

        // 文件不存在时返回空字符串
        return "";
    }
}
