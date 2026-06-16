package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.MimeException;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.provider.util.GridInitUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


/**
 * 谷歌切片方案的获取器基础类
 * 不再继承CachedTileGetter，只提供基础配置
 *
 * @author 张俊
 * @date Created in 2026/5/9 14:10
 */
@Slf4j
public abstract class BaseTileGetter implements LayerTileGetter {


    @Getter
    private final PxyLayerInfo layerInfo;

    public BaseTileGetter(PxyLayerInfo layerInfo) {
        this.layerInfo = layerInfo;
    }

    @Override
    public ImageMime getSrcFormat() {
        try {
            return (ImageMime) ImageMime.createFromFormat(layerInfo.getImageType());
        } catch (MimeException e) {
            return ImageMime.png;
        }
    }

    @Override
    public GridSubset getSrcGridSubset() {
        Integer gridSrid = layerInfo.getGridSrid();
        if (gridSrid.equals(3857)) {
            return GridInitUtils.getWorldGrid3857();
        } else {
            return GridInitUtils.getTdtGrid4490();
        }
    }
}
