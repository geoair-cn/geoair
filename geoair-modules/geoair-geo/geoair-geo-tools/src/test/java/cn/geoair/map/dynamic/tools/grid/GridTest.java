package cn.geoair.map.dynamic.tools.grid;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileLevelMetadata;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

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
        String s = GirAdvTools.getTileGrid4326Opt().xyzToWkt(4, 12, 2, TileYAxis.XYZ, 4326);
        System.out.println(s);
    }

    private static void grid4326Separate() {
        List<TileLevelMetadata> tileLevelMetadataList = GirAdvTools.getTileGrid4326SeparateOpt().getTileLevelMetadataList(0, 20, 256, 96);
        tileLevelMetadataList.forEach(System.out::println);
    }

    private static void grid3857() {
        List<TileLevelMetadata> tileLevelMetadataList = GirAdvTools.getTileGrid3857Opt().getTileLevelMetadataList(0, 20, 256, 90.7);
        tileLevelMetadataList.forEach(System.out::println);
//        String wkt = "POLYGON((104.15712743065644 42.47649994632756,104.15694674023413 44.79331315761985,108.5220365492146 44.79338132926078,108.52231600742141 42.47657770242648,104.15712743065644 42.47649994632756))";
        String wkt = "POLYGON ((104.0625 42.1875, 106.875 42.1875, 106.875 44.99999999999999, 104.0625 44.99999999999999, 104.0625 42.1875))";
//        String wkt = "POLYGON((104.85190975779834 40.6964286539505,104.85150473702095 45.65467549999515,110.45700838222794 45.65426173885738,110.45768707117826 40.696040777091945,104.85190975779834 40.6964286539505))";
//        String wkt = "POLYGON((107.18924385362766 40.01587053579636,107.18865838440078 45.075679489623326,109.38964179001998 45.07550027237384,109.39029580617311 40.01569977930946,107.18924385362766 40.01587053579636))";
        Geometry geometry = GirAdvTools.getFormatOpt().wktToJtsGeometry(wkt);
        Geometry convert = GirAdvTools.getSridOpt().convert(geometry, 4326, 3857);
        ReferencedEnvelope envelope = JTS.toEnvelope(convert);

        BoxReferencedEnvelope boxReferencedEnvelope1 = new BoxReferencedEnvelope(envelope, 3857);
        System.out.println(boxReferencedEnvelope1.getWktString(4326));
        RangeApo rangeApo = GirAdvTools.getTileGrid3857Opt().tileRangeByBox(7, envelope, 3857);
        BoxReferencedEnvelope boxReferencedEnvelope = GirAdvTools.getTileGrid3857Opt().boundsFromRangeApo(rangeApo, 4326);

        System.out.println(boxReferencedEnvelope1.getWktString(4326)+";"+boxReferencedEnvelope.getWktString(4326));
        System.out.println(boxReferencedEnvelope.getJtsEnvelope().contains(boxReferencedEnvelope1.getJtsEnvelope()));
    }
}
