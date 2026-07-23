package cn.geoair.map.tile.forge.core.test;

import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;

/**
 * TileRequest 返回对象示例
 */
public class TileRequestExample {

    public static void main(String[] args) {
        GirLayerConfigContext context = new GirLayerConfigContext();
        context.setDataId("base_layer")
            .setStorageType(GirStorageType.LOCAL_ZIP)
            .setMapTileType(GirMapTileType.XYZ);

        TileRequest empty = TileRequest.emptyByContext(context);

        System.out.println("layerName = " + empty.getLayerName());
        System.out.println("mapTileType = " + empty.getMapTileType());
        System.out.println("storageType = " + empty.getStorageType());
        System.out.println("exists = " + empty.isExists());
        System.out.println("size = " + empty.getSize());
    }
}
