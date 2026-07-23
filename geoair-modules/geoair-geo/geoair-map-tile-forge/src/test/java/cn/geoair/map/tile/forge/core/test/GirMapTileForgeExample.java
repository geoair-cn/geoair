package cn.geoair.map.tile.forge.core.test;

import cn.geoair.map.tile.forge.core.GirLayerConfigContextHelper;
import cn.geoair.map.tile.forge.core.TileRequest;
import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.service.GirMapTileService;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.support.TileStorageSupportAdapter;

import java.util.Optional;

/**
 * geoair-map-tile-forge 核心入口示例
 */
public class GirMapTileForgeExample {

    public static void main(String[] args) throws Exception {
        GirLayerConfigContext context = new GirLayerConfigContext();
        context.setLayerName("base_layer")
            .setDataId("XYZ")
            .setMapTileType(GirMapTileType.XYZ)
            .setStorageType(GirStorageType.LOCAL_ZIP)
            .setObjectKey("E:/tiles/example.zip");

        GirLayerConfigContextHelper helper = new GirLayerConfigContextHelper() {
            @Override
            public Optional<GirLayerConfigContext> getByLayerName(String layerName) {
                return Optional.of(context);
            }
        };

        TileStorageSupportAdapter adapter = new TileStorageSupportAdapter(helper);
        ITileStorageSupport support = adapter.getSupport(context);
        System.out.println("support = " + support.getClass().getSimpleName());

        // 这里只演示入口和对象路由关系，不强依赖真实瓦片文件存在
        GirMapTileService service = new GirMapTileService(helper, adapter);
        try {
            TileRequest tileRequest = service.getLayerTile(context, "10", "388", "845");
            System.out.println("tileRequest exists = " + tileRequest.isExists());
        } catch (Exception e) {
            System.out.println("getLayerTile skipped because no real tile source was provided: " + e.getMessage());
        }
    }
}
