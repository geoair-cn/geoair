package cn.geoair.map.tile.forge.fuser.converter;


import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.tile.forge.fuser.mbtiles.MbtilesInfo;

import java.util.function.BiFunction;

/**
 * 瓦片文件名处理器接口
 * 参数：entryName - ZIP 条目名称，tileData - 瓦片二进制数据
 * 返回：MbtilesInfo 对象，如果返回 null 则跳过该瓦片
 */
@FunctionalInterface
public interface TileNameProcessor extends BiFunction<String, byte[], MbtilesInfo> {


    /**
     * 默认的文件名处理器
     * 支持格式：z/y/x.png 或 z/y/x.jpg
     */
    static final TileNameProcessor DEFAULT_PROCESSOR = (entryName, tileData) -> {
        String[] pathArr = entryName.split("/");
        if (pathArr.length != 3) {
            return null;
        }
        try {
            int z = Integer.parseInt(pathArr[0]);
            int y = Integer.parseInt(pathArr[1]);
            String yFile = pathArr[2];
            int x = Integer.parseInt(yFile.replaceAll("\\.(png|jpg)$", ""));
            int reverseY = GirAdvTools.getTileGrid3857Opt().convertY(z, y, TileYAxis.XYZ, TileYAxis.TMS);
            return MbtilesInfo.of()
                    .setX(x)
                    .setZoomLevel(z)
                    .setY(reverseY)
                    .setTileData(tileData);
        } catch (NumberFormatException e) {
            return null;
        }
    };
    /**
     * 示例：自定义处理器 - 支持 z/x/y 格式（注意顺序不同）
     */
    static final TileNameProcessor PROCESSOR_ZXY = (entryName, tileData) -> {
        String[] pathArr = entryName.split("/");
        if (pathArr.length != 3) {
            return null;
        }
        try {
            int z = Integer.parseInt(pathArr[0]);
            int x = Integer.parseInt(pathArr[1]);
            int y = Integer.parseInt(pathArr[2].replaceAll("\\.(png|jpg)$", ""));
            int reverseY = GirAdvTools.getTileGrid3857Opt().convertY(z, y, TileYAxis.XYZ, TileYAxis.TMS);
            return MbtilesInfo.of()
                    .setX(x)
                    .setZoomLevel(z)
                    .setY(reverseY)
                    .setTileData(tileData);
        } catch (NumberFormatException e) {
            return null;
        }
    };
    /**
     * 示例：自定义处理器 - 支持 z/y/x 格式但 y 不需要翻转
     */
    static final TileNameProcessor PROCESSOR_ZYX_NO_REVERSE = (entryName, tileData) -> {
        String[] pathArr = entryName.split("/");
        if (pathArr.length != 3) {
            return null;
        }
        try {
            int z = Integer.parseInt(pathArr[0]);
            int y = Integer.parseInt(pathArr[1]);
            String yFile = pathArr[2];
            int x = Integer.parseInt(yFile.replaceAll("\\.(png|jpg)$", ""));
            return MbtilesInfo.of()
                    .setX(x)
                    .setZoomLevel(z)
                    .setY(y)
                    .setTileData(tileData);
        } catch (NumberFormatException e) {
            return null;
        }
    };

}
