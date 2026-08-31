package cn.geoair.map.dynamic.tools.array;

import cn.geoair.map.dynamic.tools.ToolsConfig;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.Collections;

/**
 * {@link GirGeom2ArrayUtils} 的坐标数组转换回归测试。
 *
 * @author 张逢吉
 */
public class GirGeom2ArrayUtilsTest {

    @Test
    public void shouldReuseConfiguredInstanceAndBuildPolygonWithHoles() {
        ToolsConfig config =
                new ToolsConfig()
                        .setGeometryFactory(new GeometryFactory(new PrecisionModel(), 4326));
        GirGeom2ArrayUtils arrayUtils = GirGeom2ArrayUtils.getInstance(config);

        double[][] shell = {
            {116.40D, 39.90D}, {116.45D, 39.90D}, {116.45D, 39.95D}, {116.40D, 39.95D}
        };
        double[][] hole = {
            {116.41D, 39.91D}, {116.42D, 39.91D}, {116.42D, 39.92D}, {116.41D, 39.92D}
        };
        Polygon polygon =
                arrayUtils.doubleArrayToPolygon(
                        shell,
                        Collections.singletonList(hole),
                        GirGeom2ArrayOpt.CoordOrder.X_FIRST,
                        null);

        Assert.assertSame(arrayUtils, GirGeom2ArrayUtils.getInstance(config));
        Assert.assertEquals(4326, polygon.getSRID());
        Assert.assertEquals(1, polygon.getNumInteriorRing());
        Assert.assertTrue(polygon.getExteriorRing().isClosed());
        Assert.assertEquals(
                2,
                arrayUtils.polygonToDoubleArrays(polygon, GirGeom2ArrayOpt.CoordOrder.X_FIRST)
                        .length);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectLineStringWithOnlyOneCoordinate() {
        GirGeom2ArrayUtils.getInstance(new ToolsConfig())
                .doubleArrayToLineString(new double[][] {{116.40D, 39.90D}});
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectInvalidPointStringInsteadOfReturningNull() {
        GirGeom2ArrayUtils.getInstance(new ToolsConfig()).pointByString("invalid", "39.90");
    }
}
