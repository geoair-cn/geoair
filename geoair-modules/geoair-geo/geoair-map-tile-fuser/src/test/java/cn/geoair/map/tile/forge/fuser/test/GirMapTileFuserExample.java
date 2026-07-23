package cn.geoair.map.tile.forge.fuser.test;

import cn.geoair.map.tile.forge.fuser.GirFuser;
import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.provider.LayerTileGetter;
import cn.geoair.map.tile.forge.fuser.provider.TileGetterFactory;
import cn.geoair.map.tile.forge.fuser.enums.SrcType;

/**
 * geoair-map-tile-fuser 核心入口示例
 */
public class GirMapTileFuserExample {

    public static void main(String[] args) {
        PxyLayerInfo info = new PxyLayerInfo()
            .setLayerName("base_layer")
            .setPath("https://tile.example.com/{z}/{x}/{y}.png")
            .setSrcType(SrcType.WEB.getCode())
            .setGridSrid(3857)
            .setImageType("png")
            .setEnableCache("false");

        LayerTileGetter getter = TileGetterFactory.create(info);
        System.out.println("getter = " + getter.getClass().getSimpleName());
        System.out.println("layerName = " + info.getLayerName());
        System.out.println("srcType = " + info.getSrcTypeEnums());
        System.out.println("isGoogleGrid = " + info.isGoogleGrid());

        // 这里只保留快速入口演示，避免依赖具体 Spring Bean 环境
        System.out.println("GirFuser quick entry methods: getLayerTileGetter(layerName), getPxyLayerInfo(layerName)");
    }
}
