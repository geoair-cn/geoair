package cn.geoair.map.tile.forge.core.support.arcgis;

import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.utils.ArcgisTileUtils;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/17 10:16
 * &#064;description：本地配置XML获取器抽象类，用于从本地文件系统读取ArcGIS图层配置文件
 */
public abstract class ConfigXmlGetterLocal extends AbstractArcgisSupport {


    /**
     * 根据图层配置信息获取配置XML内容
     *
     * @param layerConfigContext 图层配置信息对象
     * @return 配置文件的XML字符串内容
     * @throws Exception 读取文件异常
     */
    @Override
    public String getConfigXml(GirLayerConfigContext layerConfigContext) throws Exception {
        return ArcgisTileUtils.getConfigXmlByLocal(layerConfigContext);
    }

    @Override
    public String getConfigCdi(GirLayerConfigContext layerConfigContext) throws Exception {
        return ArcgisTileUtils.getConfigCdiByLocal(layerConfigContext);
    }


}
