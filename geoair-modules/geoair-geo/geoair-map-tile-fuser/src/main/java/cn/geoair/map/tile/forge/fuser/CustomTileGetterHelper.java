package cn.geoair.map.tile.forge.fuser;

import cn.geoair.map.dynamic.tools.GirService;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/22 09:47
 * @description： 客户端自定义的tileGetter实现
 */
public interface CustomTileGetterHelper {

    static CustomTileGetterHelper getInstance() {
        return GirService.getPxyBeanC(CustomTileGetterHelper.class);
    }

    /**
     *  通过图层对象获取TileGetter的具体实现
     * @param layerInfo 图层对象
     * @return TileGetter具体自定义实现
     */
    LayerTileGetter getTileGetterByPxyLayerInfo(PxyLayerInfo layerInfo);


}
