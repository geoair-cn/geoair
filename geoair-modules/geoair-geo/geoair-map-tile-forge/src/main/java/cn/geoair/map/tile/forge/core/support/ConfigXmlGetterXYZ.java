package cn.geoair.map.tile.forge.core.support;

import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;

/**
 * @author 张俊
 * @date 2025/11/17 10:16
 * @description 配置XML获取器空实现抽象类，提供默认的未实现异常抛出
 */
public abstract class ConfigXmlGetterXYZ extends AbstractTileStorageSupport {

    /**
     * 从存储中获取配置XML文件内容
     *
     * @param layerConfigContext 图层配置信息对象
     * @return String 配置XML文件内容
     * @throws Exception 未实现异常
     */
    @Override
    public String getConfigXml(GirLayerConfigContext layerConfigContext) throws Exception {
        throw new Exception("未实现");
    }

    @Override
    public String getConfigCdi(GirLayerConfigContext layerConfigContext) throws Exception {
        throw new Exception("未实现");
    }

    /**
     * 获取瓦片的边界信息
     *
     * @param layerConfigContext 瓦片的配置信息
     * @return 瓦片的边界信息
     * @throws Exception 获取过程中可能出现的异常，如网络错误、文件读取错误等
     */
    public BoundingBox getBoundingBox(GirLayerConfigContext layerConfigContext) throws Exception {
        return BoundingBox.WORLD3857;

    }


}
