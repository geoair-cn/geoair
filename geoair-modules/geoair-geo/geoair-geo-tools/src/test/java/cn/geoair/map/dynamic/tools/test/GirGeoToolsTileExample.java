package cn.geoair.map.dynamic.tools.test;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.GirBingMapQuadKeyOpt;
import cn.geoair.map.dynamic.tools.grid.GirTileConverterOpt;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import java.util.Set;
import org.locationtech.jts.geom.Envelope;

/** 瓦片与 QuadKey API 示例 */
public class GirGeoToolsTileExample {

    public static void main(String[] args) {
        GirTileConverterOpt tileOpt = GirGeoTools.defaultInstance().getTileGrid4326Opt();
        GirBingMapQuadKeyOpt quadKeyOpt = GirGeoTools.defaultInstance().getTileGridBingMapOpt();

        BoxReferencedEnvelope tileBox = tileOpt.xyzToTileBox(10, 845, 388, TileYAxis.XYZ, 4326);
        String wkt = tileOpt.xyzToWkt(10, 845, 388, TileYAxis.XYZ, 4326);
        RangeApo range =
                tileOpt.tileRangeByBox(10, new Envelope(116.35, 116.55, 39.85, 40.05), 4326);
        Set<TileZxyApo> zxyList =
                tileOpt.zxyListByBox(
                        new Envelope(116.35, 116.55, 39.85, 40.05), 4326, 10, TileYAxis.XYZ);

        System.out.println("xyzToTileBox = " + tileBox);
        System.out.println("xyzToWkt = " + wkt);
        System.out.println(
                "tileRangeByBox = minX:" + range.getMinX() + ", maxX:" + range.getMaxX());
        System.out.println("zxyListByBox size = " + zxyList.size());

        String quadKey = quadKeyOpt.xyzToQuadKey(845, 388, 10);
        TileZxyApo xyz = quadKeyOpt.quadKeyToXyz(quadKey);
        String parent = quadKeyOpt.getParentQuadKey(quadKey);

        System.out.println("xyzToQuadKey = " + quadKey);
        System.out.println("quadKeyToXyz = " + xyz.getZxyString());
        System.out.println("getParentQuadKey = " + parent);
    }
}
