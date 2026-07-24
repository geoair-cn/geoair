package cn.geoair.map.tile.forge.core.test;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.support.TileStorageSupportAdapter;
import java.util.Optional;

/** TileStorageSupportAdapter 路由示例 */
public class TileStorageSupportAdapterRouteExample {

    public static void main(String[] args) {
        GirLayerConfigContextHelper helper =
                new GirLayerConfigContextHelper() {
                    @Override
                    public Optional<GirLayerConfigContext> getByLayerName(String layerName) {
                        return Optional.empty();
                    }
                };

        TileStorageSupportAdapter adapter = new TileStorageSupportAdapter(helper);

        GirLayerConfigContext localZipXyz =
                new GirLayerConfigContext()
                        .setDataId("xyz_layer")
                        .setStorageType(GirStorageType.LOCAL_ZIP)
                        .setMapTileType(GirMapTileType.XYZ);

        GirLayerConfigContext localUnzippedCompact =
                new GirLayerConfigContext()
                        .setDataId("compact_layer")
                        .setStorageType(GirStorageType.LOCAL_UNZIPPED)
                        .setMapTileType(GirMapTileType.COMPACT_V2);

        GirLayerConfigContext s3ZipTerrain =
                new GirLayerConfigContext()
                        .setDataId("terrain_layer")
                        .setStorageType(GirStorageType.S3_ZIP)
                        .setMapTileType(GirMapTileType.TERRAIN_3D);

        ITileStorageSupport support1 = adapter.getSupport(localZipXyz);
        ITileStorageSupport support2 = adapter.getSupport(localUnzippedCompact);
        ITileStorageSupport support3 = adapter.getSupport(s3ZipTerrain);

        System.out.println("LOCAL_ZIP + XYZ -> " + support1.getClass().getSimpleName());
        System.out.println("LOCAL_UNZIPPED + COMPACT_V2 -> " + support2.getClass().getSimpleName());
        System.out.println("S3_ZIP + TERRAIN_3D -> " + support3.getClass().getSimpleName());
    }
}
