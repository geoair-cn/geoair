package cn.geoair.map.tile.forge.core.test;

import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;

/** map-tile-forge 枚举示例 */
public class TileForgeEnumExample {

    public static void main(String[] args) {
        for (GirStorageType storageType : GirStorageType.values()) {
            System.out.println(
                    "storageType = " + storageType + ", value = " + storageType.getValue());
        }

        for (GirMapTileType tileType : GirMapTileType.values()) {
            System.out.println("tileType = " + tileType + ", value = " + tileType.getValue());
        }
    }
}
