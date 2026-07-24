package cn.geoair.map.dynamic.mvt.test;

import cn.geoair.map.dynamic.mvt.dto.TileExecutorConfig;

/** TileExecutorConfig 示例 */
public class TileExecutorConfigExample {

    public static void main(String[] args) {
        TileExecutorConfig config = new TileExecutorConfig();
        config.setIgnoreMinZoom(true)
                .setLowLevelOptStrategy(TileExecutorConfig.LowLevelOptStrategy.PAGING)
                .setDensityOptStrategy(TileExecutorConfig.DensityOptStrategy.DENSITY_MERGING)
                .setPagingStartLevel(10)
                .setPagingThreshold(1000)
                .setMaxPageNumber(8L)
                .setMaxPageSize(2000L)
                .setLimitStartLevel(8)
                .setMaxLimitCount(3000L);

        TileExecutorConfig copy = config.copy();

        System.out.println("lowLevelOptStrategy = " + copy.getLowLevelOptStrategy());
        System.out.println("densityOptStrategy = " + copy.getDensityOptStrategy());
        System.out.println("pagingStartLevel = " + copy.getPagingStartLevel());
        System.out.println("maxLimitCount = " + copy.getMaxLimitCount());
    }
}
