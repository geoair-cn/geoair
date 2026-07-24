package cn.geoair.map.tile.forge.fuser.provider;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.MimeType;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;

import lombok.Getter;

/**
 * 谷歌切片方案的获取器基础类 不再继承CachedTileGetter，只提供基础配置
 *
 * @author 张俊
 * @date Created in 2026/5/9 14:10
 */
public abstract class BaseTileGetter implements LayerTileGetter {

    private static GiLogger log = GirLoggerFactory.getLogger();
    @Getter private final PxyLayerInfo layerInfo;

    public BaseTileGetter(PxyLayerInfo layerInfo) {
        this.layerInfo = layerInfo;
    }

    @Override
    public ImageMime getSrcFormat() {
        if (GutilObject.isEmpty(layerInfo.getImageType())) {
            return ImageMime.png;
        }
        try {
            return (ImageMime) MimeType.createFromExtension(layerInfo.getImageType());
        } catch (Exception e) {
            try {
                return (ImageMime) MimeType.createFromFormat(layerInfo.getImageType());
            } catch (Exception e1) {

            }
        }
        return ImageMime.png;
    }

    @Override
    public GridSubset getSrcGridSubset() {
        Integer gridSrid = layerInfo.getGridSrid();
        if (gridSrid.equals(3857) || gridSrid.equals(900913)) {
            return GridInitUtils.getWorldGrid3857();
        } else {
            return GridInitUtils.getTdtGrid4490();
        }
    }
}
