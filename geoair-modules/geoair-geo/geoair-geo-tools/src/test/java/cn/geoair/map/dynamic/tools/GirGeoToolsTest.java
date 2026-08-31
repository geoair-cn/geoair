package cn.geoair.map.dynamic.tools;

import cn.geoair.map.dynamic.tools.convert.GirFormatUtils;
import cn.geoair.map.dynamic.tools.measure.GirGeoMeasureUtils;
import cn.geoair.map.dynamic.tools.srid.GirSridConvertUtils;

import org.junit.Assert;
import org.junit.Test;

/** GirGeoTools 按配置复用与 CRS 异常语义测试。 */
public class GirGeoToolsTest {

    @Test
    public void shouldReuseToolsForSameToolsConfig() {
        ToolsConfig config = ToolsConfig.of();
        GirGeoTools first = GirGeoTools.getInstance(config);
        GirGeoTools second = GirGeoTools.getInstance(config);

        Assert.assertSame(first, second);
        Assert.assertSame(first.getFormatOpt(), second.getFormatOpt());
        Assert.assertSame(first.getMeasureOpt(), second.getMeasureOpt());
        Assert.assertSame(first.getSridOpt(), second.getSridOpt());
        Assert.assertSame(first.getFormatOpt(), GirFormatUtils.getInstance(config));
        Assert.assertSame(first.getMeasureOpt(), GirGeoMeasureUtils.getInstance(config));
        Assert.assertSame(first.getSridOpt(), GirSridConvertUtils.getInstance(config));
    }

    @Test
    public void shouldReportInvalidCrsExplicitly() {
        try {
            GirGeoTools.defaultInstance().getSridOpt().getCRS(-1);
            Assert.fail("非法 SRID 应抛出异常");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("SRID"));
        }
    }

    @Test
    public void shouldUseKnownCrsTypeChecksBeforeFactoryResolution() {
        GirSridConvertUtils sridUtils = GirSridConvertUtils.getInstance(ToolsConfig.of());

        Assert.assertTrue(sridUtils.isGeographicCRS(4326));
        Assert.assertTrue(sridUtils.isGeographicCRS(4490));
        Assert.assertTrue(sridUtils.isGeographicCRS(4480));
        Assert.assertTrue(sridUtils.isGeographicCRS(4979));
        Assert.assertTrue(sridUtils.isGeographicCRS(4269));
        Assert.assertFalse(sridUtils.isGeographicCRS(3857));
        Assert.assertFalse(sridUtils.isGeographicCRS(900913));
        Assert.assertFalse(sridUtils.isGeographicCRS(32650));
        Assert.assertFalse(sridUtils.isGeographicCRS(4491));

        try {
            sridUtils.isGeographicCRS(0);
            Assert.fail("非法 SRID 应抛出异常");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("SRID"));
        }
    }
}
