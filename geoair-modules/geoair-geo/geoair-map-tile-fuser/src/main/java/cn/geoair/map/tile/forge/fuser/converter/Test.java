package cn.geoair.map.tile.forge.fuser.converter;

import java.util.Arrays;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/24 17:54
 * @description： TODO
 */
public class Test {

    public static void main(String[] args) {
        // 1. 基本用法 - 转换所有瓦片到 MBTiles
        MbtilesLayerImporter.ImportConfig config = new MbtilesLayerImporter.ImportConfig()
                .setSourceMbtiles("G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1_13.mbtiles")
                .setSourceLayerName("imagery")
                .setTargetMbtiles("G:\\softdir\\nginx-1.18.0\\nginx_pxy\\copy.mbtiles")
                .setTargetLayerName("imagery_backup")
//                .setZoomLevels(Arrays.asList(0, 1, 2, 3, 4, 5, 6,7,8,9,10,11))
                .setZoomLevels(Arrays.asList(13))
                .setOverwrite(true)                // 覆盖已存在的瓦片
                .setBatchSize(20000)                // 批量插入大小
                .setCopyMetadata(true)             // 复制元数据
                .setMaxPoolSize(20)                // 连接池大小
                .setMinIdle(2);                    // 最小空闲连接数

        MbtilesLayerImporter.ImportResult result5 = MbtilesLayerImporter.importLayers(config);
    }

}
