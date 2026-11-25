package cn.geoair.map.tile.forge.fuser.image.watermark;

import cn.geoair.map.dynamic.tools.GirAdvTools;
import cn.geoair.map.dynamic.tools.grid.dto.BoxReferencedEnvelope;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.core.bygwc.io.ByteArrayResource;
import cn.geoair.map.tile.forge.fuser.fuser.GirFuserExec;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingContext;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingResult;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingState;
import cn.geoair.map.tile.forge.fuser.support.FusionTestSource;
import cn.geoair.map.tile.forge.fuser.provider.PngLayerTileGetter;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;
import cn.geoair.map.tile.forge.fuser.utils.TileImageUtils;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/** 转换缓存必须用真实融合计划变换模板，不能直接沿用原始文字大小。 */
public class GoogleWatermarkScalingTest {
    @Test public void convertedMercatorTileUsesRenderPlanInsteadOfDuplicateScaleMath() throws Exception {
        verify(3857, TileImageProcessingState.PROCESSED);
    }

    @Test public void uncertainHalfSizeGeographicTextIsLeftForReview() throws Exception {
        verify(4490, TileImageProcessingState.REVIEW);
    }

    private void verify(int srid, TileImageProcessingState expected) throws Exception {
        byte[] marked;
        try (InputStream input = getClass().getResourceAsStream("/watermark/google-sea-12-3441-1680.png")) {
            marked = TileImageUtils.readAllLimited(input);
        }
        GridSubset grid = srid == 3857 ? GridInitUtils.getWorldGrid3857() : GridInitUtils.getTdtGrid4490();
        FusionTestSource source = new FusionTestSource(grid, (x, y) ->
                x == (srid == 3857 ? 6 : 12) && y == 4 ? new ByteArrayResource(marked) : null);
        BoxReferencedEnvelope projected = srid == 3857
                ? GirAdvTools.getTileGrid4326Opt().xyzToTileBox(3, 6, 1, TileYAxis.XYZ, 3857)
                : GirAdvTools.getTileGrid3857Opt().xyzToTileBox(3, 6, 3, TileYAxis.XYZ, 4326);
        BoundingBox bounds = new BoundingBox(projected.getMinX(), projected.getMinY(), projected.getMaxX(), projected.getMaxY());
        // 测试样本扩展名为 PNG、实际字节为 JPEG，和线上瓦片服务一致。
        GirFuserExec fusion = new GirFuserExec(new PngLayerTileGetter(source), ImageMime.png, bounds, 256, 256);
        TileImageProcessingContext context = TileImageProcessingContext.from(fusion.getRenderPlan());
        byte[] converted = fusion.toImageBytes();
        TileImageProcessingResult result = new GoogleWatermarkProcessor().process(converted, context);
        assertEquals("缩放=" + context.getScaleX() + "," + context.getScaleY() + " 得分=" + result.getScore(),
                expected, result.getState());
        if (expected == TileImageProcessingState.REVIEW) assertNull(result.getBytes());
    }
}
