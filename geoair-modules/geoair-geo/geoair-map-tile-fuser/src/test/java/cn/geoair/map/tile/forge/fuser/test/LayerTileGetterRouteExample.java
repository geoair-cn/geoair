package cn.geoair.map.tile.forge.fuser.test;

import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.OriginType;
import cn.geoair.map.tile.forge.fuser.enums.SrcType;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.TileGetterFactory;

/**
 * LayerTileGetter 路由示例
 */
public class LayerTileGetterRouteExample {

    public static void main(String[] args) {
        PxyLayerInfo local3857 = new PxyLayerInfo()
            .setLayerName("local_google")
            .setPath("D:/tiles/{z}/{x}/{y}.png")
            .setSrcType(SrcType.LOCAL.getCode())
            .setOriginType(OriginType.Google.getMode())
            .setGridSrid(3857)
            .setEnableCache("false");

        PxyLayerInfo web4490 = new PxyLayerInfo()
            .setLayerName("web_4490")
            .setPath("https://tile.example.com/{z}/{x}/{y}.png")
            .setSrcType(SrcType.WEB.getCode())
            .setOriginType(OriginType.TMS.getMode())
            .setGridSrid(4490)
            .setEnableCache("false");

        PxyLayerInfo mbtiles = new PxyLayerInfo()
            .setLayerName("mbtiles_layer")
            .setPath("D:/tiles/base.mbtiles")
            .setSrcType(SrcType.MBTILES.getCode())
            .setOriginType(OriginType.Google.getMode())
            .setGridSrid(3857)
            .setEnableCache("false");

        LayerTileGetter getter1 = TileGetterFactory.create(local3857);
        LayerTileGetter getter2 = TileGetterFactory.create(web4490);
        LayerTileGetter getter3 = TileGetterFactory.create(mbtiles);

        System.out.println("local3857 getter = " + getter1.getClass().getSimpleName());
        System.out.println("web4490 getter = " + getter2.getClass().getSimpleName());
        System.out.println("mbtiles getter = " + getter3.getClass().getSimpleName());
    }
}
