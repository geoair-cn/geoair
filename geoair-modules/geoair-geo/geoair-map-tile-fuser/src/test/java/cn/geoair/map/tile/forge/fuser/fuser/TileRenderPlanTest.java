package cn.geoair.map.tile.forge.fuser.fuser;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubset;
import cn.geoair.map.tile.forge.fuser.support.FusionTestSource;
import cn.geoair.map.tile.forge.fuser.utils.GridInitUtils;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingContext;
import org.junit.Test;
import static org.junit.Assert.*;

public class TileRenderPlanTest {
    @Test public void planAndRealFusionUseSameLevelCoverageAndRoundedCanvas() throws Exception {
        GridSubset grid = GridInitUtils.getWorldGrid3857();
        BoundingBox tile = grid.boundsFromIndex(new long[]{2, 3, 4});
        BoundingBox bounds = new BoundingBox(tile.getMinX(), tile.getMinY(), tile.getMaxX() - tile.getWidth() * .108,
                tile.getMaxY() - tile.getHeight() * .105);
        FusionTestSource source = new FusionTestSource(grid, (x, y) -> null);
        GirFuserExec fusion = new GirFuserExec(source, ImageMime.png, bounds, 256, 256);
        TileRenderPlan plan = fusion.getRenderPlan();
        assertEquals(0, source.calls.get());
        fusion.toImageBytes();
        assertEquals(plan.getSourceZoom(), source.lastZ);
        assertEquals(Math.round(bounds.getWidth() / plan.getSourceResolution()), plan.getCanvasWidth());
        assertEquals(256D / plan.getCanvasWidth(), TileImageProcessingContext.from(plan).getScaleX(), 0);
        assertEquals(plan.getSourceRectangle()[0], fusion.getSrcRange().getMinX());
        long original = plan.getSourceRectangle()[0];
        long[] copy = plan.getSourceRectangle(); copy[0] = 99;
        assertEquals(original, plan.getSourceRectangle()[0]);
    }

    @Test public void rectangularOutputUsesIndependentAxisScale() {
        TileRenderPlan plan = TileRenderPlan.calculate(GridInitUtils.getWorldGrid3857(),
                BoundingBox.WORLD3857, 512, 256);
        assertEquals(.5, plan.getScaleY() / plan.getScaleX(), .0001);
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsInvalidBoundsBeforeAllocatingCanvas() {
        TileRenderPlan.calculate(GridInitUtils.getWorldGrid3857(), new BoundingBox(0, 0, 0, 1), 256, 256);
    }

    @Test public void rejectsOversizedCanvasBeforeReadingAnySourceTile() throws Exception {
        FusionTestSource source = new FusionTestSource(GridInitUtils.getWorldGrid3857(), (x, y) -> null);
        GirFuserExec fusion = new GirFuserExec(source, ImageMime.png, BoundingBox.WORLD3857, 8192, 8192);
        TileFusionResult result = fusion.toImageResult();
        assertEquals(TileFusionState.FAILED, result.getState());
        assertTrue(result.getFailure() instanceof IllegalArgumentException);
        assertEquals(0, source.calls.get());
    }
}
