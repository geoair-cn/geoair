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
public class ArcgisV2Test {
    public static void main(String[] args) {
        GirLayerConfigContext
                context = new GirLayerConfigContext();
        context.setDataId("COMPACT_V2").setMapTileType(GirMapTileType.COMPACT_V2).setStorageType(GirStorageType.LOCAL_ZIP).setObjectKey("E:\\gis测试数据\\测试数据\\ArcgisV2\\0-10.zip");
        TileStorageSupportAdapter adapter = new TileStorageSupportAdapter(new TestGirLayerConfigContextHelper());
        ITileStorageSupport support = adapter.getSupport(context);
        support.preCacheTiles(context, new LogProgressConsumer());

    }

}
