package test;

import cn.geoair.map.tile.forge.core.enums.GirMapTileType;
import cn.geoair.map.tile.forge.core.enums.GirStorageType;
import cn.geoair.map.tile.forge.core.model.GirLayerConfigContext;
import cn.geoair.map.tile.forge.core.support.ITileStorageSupport;
import cn.geoair.map.tile.forge.core.support.TileStorageSupportAdapter;
import cn.geoair.map.tile.forge.core.zip.LogProgressConsumer;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/3 15:58
 * @description： TODO
 */
public class MVTTileTest {
    public static void main(String[] args) {
        GirLayerConfigContext context = new GirLayerConfigContext();
        context.setDataId("MVT_TILES")
                .setMapTileType(GirMapTileType.MVT_TILES)
                .setStorageType(GirStorageType.LOCAL_ZIP)
                .setObjectKey("E:\\gis测试数据\\测试数据\\MVT\\hgt_mapbox.zip");
        TileStorageSupportAdapter adapter =
                new TileStorageSupportAdapter(new TestGirLayerConfigContextHelper());
        ITileStorageSupport support = adapter.getSupport(context);
        support.preCacheTiles(context, new LogProgressConsumer());
    }
}
