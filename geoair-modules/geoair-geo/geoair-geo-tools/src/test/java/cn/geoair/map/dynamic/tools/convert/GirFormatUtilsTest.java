package cn.geoair.map.dynamic.tools.convert;

import cn.geoair.map.dynamic.tools.ToolsConfig;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * {@link GirFormatUtils} 的并发组件回归测试。
 *
 * @author 张逢吉
 */
public class GirFormatUtilsTest {

    @Test
    public void shouldCreateIndependentNonThreadSafeFormatComponents() {
        GirFormatUtils formatUtils = new GirFormatUtils(new ToolsConfig());

        Assert.assertNotSame(formatUtils.getWKBWriter(), formatUtils.getWKBWriter());
        Assert.assertNotSame(formatUtils.getGeometryJSON(), formatUtils.getGeometryJSON());
    }

    @Test
    public void shouldConvertGeoJsonConcurrentlyWithoutSharingFormatter() throws Exception {
        final GirFormatUtils formatUtils = new GirFormatUtils(new ToolsConfig());
        final Geometry geometry =
                new ToolsConfig()
                        .getGeometryFactory()
                        .createPoint(new Coordinate(116.403874D, 39.914885D));
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Callable<String>> tasks = new ArrayList<Callable<String>>();
            for (int i = 0; i < 40; i++) {
                tasks.add(
                        new Callable<String>() {
                            @Override
                            public String call() {
                                return formatUtils.jtsGeometryToGeoJson(geometry, false);
                            }
                        });
            }
            for (Future<String> future : executor.invokeAll(tasks)) {
                Assert.assertTrue(future.get().contains("Point"));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldBindConfiguredGeometryFactoryAndRoundTripEwkt() {
        ToolsConfig config =
                new ToolsConfig()
                        .setGeometryFactory(new GeometryFactory(new PrecisionModel(), 4490));
        GirFormatUtils formatUtils = new GirFormatUtils(config);

        Geometry geometry = formatUtils.wktToJtsGeometry("POINT(116.403874 39.914885)", false);
        Geometry ewktGeometry =
                formatUtils.ewktToJtsGeometry("SRID=4326;POINT(116.403874 39.914885)", false);

        Assert.assertEquals(4490, geometry.getSRID());
        Assert.assertEquals(4326, ewktGeometry.getSRID());
        Assert.assertEquals(
                "SRID=4326;POINT (116.403874 39.914885)",
                formatUtils.jtsGeometryToEwktString(ewktGeometry, false));
    }

    @Test
    public void shouldRespectStrictModeForUnsupportedPostGisObject() {
        GirFormatUtils formatUtils = new GirFormatUtils(new ToolsConfig());
        Assert.assertNull(formatUtils.pgGeometryToJtsGeometry(new Object(), true));
        try {
            formatUtils.pgGeometryToJtsGeometry(new Object(), false);
            Assert.fail("严格模式应拒绝不支持的 PostGIS Geometry 对象");
        } catch (RuntimeException expected) {
            Assert.assertTrue(expected.getCause() instanceof IllegalArgumentException);
        }
    }
}
