package cn.geoair.map.tile.forge.fuser.converter;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/24 17:54
 * @description： TODO
 */
public class Test {

    public static void main(String[] args) {
        // 1. 基本用法 - 转换所有瓦片到 MBTiles
        TileToMbtilesConverter.ConvertResult result = TileToMbtilesConverter.convert(
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1-13",  // 源根路径
                "{z}\\{y}\\{x}.png",                     // 路径模板
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1_13.mbtiles",          // MBTiles 文件路径
                "1_13",// 图层名称
                true
        );
    }

}
