package cn.geoair.map.tile.forge.core.support;

import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.hutool.core.io.FileUtil;

import java.io.File;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/17 10:16
 * &#064;description：本地配置XML获取器抽象类，用于从本地文件系统读取ArcGIS图层配置文件
 */
public abstract class ConfigXmlGetterLocal extends AbstractTileStorageSupport {


    /**
     * 根据图层配置信息获取配置XML内容
     *
     * @param layerConfigContext 图层配置信息对象
     * @return 配置文件的XML字符串内容
     * @throws Exception 读取文件异常
     */
    @Override
    public String getConfigXml(GirLayerConfigContext layerConfigContext) throws Exception {

        String confFileName = "Conf.xml";
        // 获取配置文件根路径
        return getString(layerConfigContext, confFileName);
    }

    @Override
    public String getConfigCdi(GirLayerConfigContext layerConfigContext) throws Exception {
        String confFileName = "conf.cdi";
        return getString(layerConfigContext, confFileName);
    }

    private static String getString(GirLayerConfigContext layerConfigContext, String confFileName) {
        // 获取配置文件根路径
        String rootPath = layerConfigContext.getObjectKey();
        // 创建文件对象
        File file = FileUtil.file(rootPath);
        // 获取文件所在目录的绝对路径
        String absolutePath = file.getParentFile().getAbsolutePath();
        // 构建配置文件完整路径（目录路径 + Conf.xml）
        String configXmlPath = absolutePath + File.separator + confFileName;
        // 读取配置文件内容为UTF-8编码的字符串
        String xmlString = FileUtil.readString(configXmlPath, "utf-8");
        return xmlString;
    }
}
