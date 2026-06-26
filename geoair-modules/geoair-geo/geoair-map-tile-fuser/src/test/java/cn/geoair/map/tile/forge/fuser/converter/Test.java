package cn.geoair.map.tile.forge.fuser.converter;

import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesUtils;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/24 17:54
 * @description： TODO
 */
public class Test {

    public static void main(String[] args) {
        MbtilesUtils.compactDatabase("G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1_13_partial.mbtiles");

    }

    private static void localFileConverter() {
        // 1. 基本用法 - 转换所有瓦片到 MBTiles
        // ==================== 6. 指定层级 + 覆盖 ====================
        List<Integer> zoomLevels = java.util.Arrays.asList(0, 1, 2, 3, 4, 5);
        MbtilesFromLocalFileConverter.ConvertResult result6 = MbtilesFromLocalFileConverter.convert(
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1-13",
                "G:\\softdir\\nginx-1.18.0\\nginx_pxy\\1_13_partial.mbtiles",
                "1_13_partial",
                config -> {
                    config.setNeedReverseY(true);
                    config.setOverwrite(true);
                    config.setBatchSize(3000);
                }
        );
        System.out.println("结果6: " + result6);
    }

}
