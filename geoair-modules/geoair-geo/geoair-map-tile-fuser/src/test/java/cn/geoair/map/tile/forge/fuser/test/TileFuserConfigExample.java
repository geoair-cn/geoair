package cn.geoair.map.tile.forge.fuser.test;

import cn.geoair.map.tile.forge.fuser.entity.PxyLayerInfo;
import cn.geoair.map.tile.forge.fuser.enums.OriginType;
import cn.geoair.map.tile.forge.fuser.enums.SrcType;

/**
 * map-tile-fuser 配置模型与枚举示例
 */
public class TileFuserConfigExample {

    public static void main(String[] args) {
        PxyLayerInfo localLayer = new PxyLayerInfo()
            .setLayerName("local_layer")
            .setPath("D:/tiles/{z}/{x}/{y}.png")
            .setSrcType(SrcType.LOCAL.getCode())
            .setOriginType(OriginType.Google.getMode())
            .setGridSrid(3857)
            .setEnableCache("true");

        PxyLayerInfo webLayer = new PxyLayerInfo()
            .setLayerName("web_layer")
            .setPath("https://tile.example.com/{z}/{x}/{y}.png")
            .setSrcType(SrcType.WEB.getCode())
            .setOriginType(OriginType.TMS.getMode())
            .setGridSrid(4490)
            .setEnableCache("false");

        System.out.println("localLayer srcType = " + localLayer.getSrcTypeEnums());
        System.out.println("localLayer originType = " + localLayer.getOriginTypeEnums());
        System.out.println("localLayer isGoogleGrid = " + localLayer.isGoogleGrid());

        System.out.println("webLayer srcType = " + webLayer.getSrcTypeEnums());
        System.out.println("webLayer originType = " + webLayer.getOriginTypeEnums());
        System.out.println("webLayer isGoogleGrid = " + webLayer.isGoogleGrid());
    }
}
