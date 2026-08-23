package cn.geoair.map.dynamic.tools.grid;

import cn.geoair.base.Gir;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Geometry;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/12/14 18:22 @description： TODO
 */
public class GirGridTest {

    public static void main(String[] args) {}

    public static void test4326(int level, int x, int y) {
        GirTileConverterOpt tileConverterOpt = GirGeoTools.defaultInstance().getTileGrid4326Opt();
        int srid = 4326;
        test(level, x, y, tileConverterOpt, srid);
    }

    private static void test(
            int level, int x, int y, GirTileConverterOpt tileConverterOpt, int srid) {
        BoxReferencedEnvelope referencedEnvelope = tileConverterOpt.xyzToTileBox(level, x, y, TileYAxis.XYZ, srid);
        Gir.log.info("tile:{}", referencedEnvelope);
        RangeApo envelope = tileConverterOpt.tileRangeByBox(level, referencedEnvelope);
        Gir.log.info("envelope:{}", envelope);
        double v = tileConverterOpt.tileXToCoordinateX(x, level);
        Gir.log.info("tileXToLon:{}", String.valueOf(v));
        double y1 = tileConverterOpt.tileYToCoordinateY(y, level, TileYAxis.XYZ);
        Gir.log.info("tileYToLat:{}", String.valueOf(y1));
        Set<TileZxyApo> tileZxyApos =
                tileConverterOpt.zxyListByBox(referencedEnvelope, srid, level, TileYAxis.XYZ);
        Gir.log.info("zxyListByBox:{}", tileZxyApos);
        Geometry geometry = GirGeoTools.defaultInstance().getSridOpt().convertToGeom(referencedEnvelope, srid, srid);
        Gir.log.info("geometry:{}", geometry);
        Set<TileZxyApo> tileZxyApos1 = tileConverterOpt.zxyListByGeom(geometry, srid, level);
        Gir.log.info("zxyListByGeom:{}", tileZxyApos1);
        GirBingMapQuadKeyOpt tileGridBingMapOpt = GirGeoTools.defaultInstance().getTileGridBingMapOpt();
        List<String> xyzToQuadKeyBatch = tileGridBingMapOpt.xyzToQuadKeyBatch(tileZxyApos1);
        Gir.log.info("xyzToQuadKeyBatch:{}", xyzToQuadKeyBatch);
        String quadKey = tileGridBingMapOpt.xyzToQuadKey(x, y, level);
        Gir.log.info("xyzToQuadKey:{}", quadKey);
        String parentQuadKey = tileGridBingMapOpt.getParentQuadKey(quadKey);
        Gir.log.info("parentQuadKey:{}", parentQuadKey);
        int quadKeyZLevel = tileGridBingMapOpt.getQuadKeyZLevel(quadKey);
        Gir.log.info("quadKeyZLevel:{}", quadKeyZLevel);
        String[] targetLevelQuadKey = tileGridBingMapOpt.getTargetLevelQuadKey(quadKey, 10);
        Gir.log.info("targetLevelQuadKey:{}", (Object[]) targetLevelQuadKey);
        TileZxyApo tileZxyApo = tileGridBingMapOpt.quadKeyToXyz(quadKey);
        Gir.log.info("quadKeyToXyz:{}", tileZxyApo);
    }

    public static void test3857(int level, int x, int y) {
        GirTileConverterOpt tileConverterOpt = GirGeoTools.defaultInstance().getTileGrid3857Opt();
        int srid = 3857;
        test(level, x, y, tileConverterOpt, srid);
    }
}
