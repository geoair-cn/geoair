package cn.geoair.map.dynamic.tools.grid;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.RangeApo;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

public class TileBoundsTest {

    public static void main(String[] args) {
        // 测试用例1：你原用例（内蒙古一带，矩形）
        testCase(
                "POLYGON((107.18924385362766 40.01587053579636,107.18865838440078 45.075679489623326,109.38964179001998 45.07550027237384,109.39029580617311 40.01569977930946,107.18924385362766 40.01587053579636))",
                7);

        // 测试用例2：北京小范围
        testCase("POLYGON((116.3 39.8,116.3 40.1,116.6 40.1,116.6 39.8,116.3 39.8))", 10);

        // 测试用例3：跨经度180（国际日期变更线附近）
        testCase("POLYGON((179.0 30.0,179.0 40.0,-179.0 40.0,-179.0 30.0,179.0 30.0))", 6);

        // 测试用例4：极地附近（北纬80）
        testCase("POLYGON((0.0 80.0,0.0 85.0,10.0 85.0,10.0 80.0,0.0 80.0))", 5);

        // 测试用例5：单点（边界退化）
        testCase("POINT(100.0 35.0)", 8);
    }

    /**
     * 通用测试方法
     *
     * @param wkt 4326的WKT
     * @param zoom 瓦片层级
     */
    public static void testCase(String wkt, int zoom) {
        try {
            System.out.println("\n===== 测试用例 zoom=" + zoom + " =====");
            System.out.println("原始WKT(4326): " + wkt);

            // 1. WKT转JTS Geometry(4326)
            Geometry geometry = GirAdvTools.getFormatOpt().wktToJtsGeometry(wkt);

            // 2. 4326 → 3857
            Geometry convert = GirAdvTools.getSridOpt().convert(geometry, 4326, 3857);
            Envelope envelope3857 = convert.getEnvelopeInternal();
            BoxReferencedEnvelope box1 = new BoxReferencedEnvelope(envelope3857, 3857);
            System.out.println("原始外包矩形(4326): " + box1.getWktString(4326));

            // 3. 按瓦片网格计算外包矩形
            RangeApo rangeApo =
                    GirAdvTools.getTileGrid3857Opt().tileRangeByBox(zoom, envelope3857, 3857);
            BoxReferencedEnvelope box2 =
                    GirAdvTools.getTileGrid3857Opt().boundsFromRangeApo(rangeApo, 4326);
            System.out.println("瓦片网格外包矩形(4326): " + box2.getWktString(4326));

            // 4. 包含关系判断
            boolean contains = box2.getJtsEnvelope().contains(box1.getJtsEnvelope());

            System.out.println(box1.getWktString(4326) + ";" + box2.getWktString(4326));

            System.out.println("瓦片包围是否包含原始范围: " + contains);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
