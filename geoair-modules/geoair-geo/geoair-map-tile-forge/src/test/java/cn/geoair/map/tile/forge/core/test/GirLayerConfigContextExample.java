package cn.geoair.map.tile.forge.core.test;

import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;

/**
 * GirLayerConfigContext 示例
 */
public class GirLayerConfigContextExample {

    public static void main(String[] args) {
        GirLayerConfigContext context = new GirLayerConfigContext();
        context.setDataId("base_layer")
            .setObjectKey("E:/tiles/base_layer.zip")
            .setStorageType(GirStorageType.LOCAL_ZIP)
            .setMapTileType(GirMapTileType.XYZ)
            .setTilePathPrefix("")
            .setFormat("png")
            .setMinZ(0)
            .setMaxZ(18)
            .setMaxX(262143)
            .setMaxY(262143);

        System.out.println("layerName = " + context.getLayerName());
        System.out.println("storageType = " + context.getStorageType());
        System.out.println("mapTileType = " + context.getMapTileType());
        System.out.println("objectKey = " + context.getObjectKey());
    }
}
