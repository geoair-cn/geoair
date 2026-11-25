package cn.geoair.map.tile.forge.fuser.image;

import cn.geoair.map.tile.forge.fuser.fuser.TileRenderPlan;
import lombok.Getter;

/** 处理算法所需的图像缩放；原始网格用单位比例，转换网格使用真实融合计划。 */
@Getter
public final class TileImageProcessingContext {
    public static final TileImageProcessingContext ORIGINAL = new TileImageProcessingContext(1, 1);
    private final double scaleX;
    private final double scaleY;

    public TileImageProcessingContext(double scaleX, double scaleY) {
        if (!Double.isFinite(scaleX) || !Double.isFinite(scaleY) || scaleX <= 0 || scaleY <= 0)
            throw new IllegalArgumentException("图像缩放比例必须为正数");
        this.scaleX = scaleX; this.scaleY = scaleY;
    }

    public static TileImageProcessingContext from(TileRenderPlan plan) {
        if (plan == null) throw new IllegalArgumentException("融合计划不能为空");
        return new TileImageProcessingContext(plan.getScaleX(), plan.getScaleY());
    }
}
