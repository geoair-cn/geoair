package cn.geoair.map.dynamic.tools.grid;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileLevelMetadata;
import org.locationtech.jts.geom.Envelope;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/11 15:13
 * @description： TODO
 */
public class GridTest {
    public static void main(String[] args) {
        grid3857();
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


        RangeApo rangeApo = GirAdvTools.getTileGrid3857Opt().tileRangeByBox(7, new Envelope(1.1584184510675032E7, 1.1897270578531113E7, 5189107.545991074, 5621521.486192066), 3857);

    }
}
