package cn.geoair.map.dynamic.tools.grid.bing;

import cn.geoair.map.dynamic.tools.ToolsConfig;
import cn.geoair.map.dynamic.tools.grid.dto.TileZxyApo;
import org.junit.Assert;
import org.junit.Test;

/** Bing QuadKey 边界与展开保护测试。 */
public class BingMapQuadKeyUtilsTest {

    private final BingMapQuadKeyUtils quadKeyUtils = new BingMapQuadKeyUtils(new ToolsConfig());

    @Test
    public void shouldSupportWorldRootQuadKey() {
        TileZxyApo root = quadKeyUtils.quadKeyToXyz("");

        Assert.assertEquals(0, root.getZ());
        Assert.assertEquals(0, root.getX());
        Assert.assertEquals(0, root.getY());
        Assert.assertArrayEquals(
                new String[] {"0", "1", "2", "3"}, quadKeyUtils.getChildQuadKeys(""));
    }

    @Test
    public void shouldRejectExcessiveQuadKeyExpansion() {
        try {
            quadKeyUtils.getTargetLevelQuadKey("", 10);
            Assert.fail("超过上限的 QuadKey 展开应被拒绝");
        } catch (IllegalArgumentException expected) {
            Assert.assertTrue(expected.getMessage().contains("超过上限"));
        }
    }
}
