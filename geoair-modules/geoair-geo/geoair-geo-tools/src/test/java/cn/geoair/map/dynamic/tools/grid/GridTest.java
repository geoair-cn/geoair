package cn.geoair.map.dynamic.tools.grid;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileLevelMetadata;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/11 15:13
 * @description： TODO
 */
public class GridTest {
    public static void main(String[] args) {
        grid4326();
    }

    private static void grid4326() {
        List<TileLevelMetadata> tileLevelMetadataList = GirAdvTools.getTileGrid4326Opt().getTileLevelMetadataList(0, 20, 256, 96);
        tileLevelMetadataList.forEach(System.out::println);
    }
    private static void grid4326Separate() {
        List<TileLevelMetadata> tileLevelMetadataList = GirAdvTools.getTileGrid4326SeparateOpt().getTileLevelMetadataList(0, 20, 256, 96);
        tileLevelMetadataList.forEach(System.out::println);
    }
    private static void grid3857() {
        List<TileLevelMetadata> tileLevelMetadataList = GirAdvTools.getTileGrid3857Opt().getTileLevelMetadataList(0, 20, 256, 90.7);
        tileLevelMetadataList.forEach(System.out::println);
    }
}
