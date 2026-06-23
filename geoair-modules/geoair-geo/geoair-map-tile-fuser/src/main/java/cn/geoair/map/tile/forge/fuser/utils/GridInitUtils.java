package cn.geoair.map.tile.forge.fuser.utils;


import cn.geoair.map.tile.forge.core.bygwc.core.DefaultGridsets;
import cn.geoair.map.tile.forge.core.bygwc.grid.BoundingBox;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSetFactory;
import cn.geoair.map.tile.forge.core.bygwc.grid.*;
import cn.geoair.map.tile.forge.core.bygwc.grid.GridSubsetFactory;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/16 09:56
 * @description： 网格相关的通用
 */
public class GridInitUtils {

    static DefaultGridsets defaultGridsets = new DefaultGridsets(false, false);
    static GridSubset WORLD_GRID_3857 = GridSubsetFactory.
            createGridSubSet(
                    defaultGridsets.
                            worldMercatorWGS84Quad(),
                    BoundingBox.WORLD3857,
                    0,
                    21);

    /**
     * 天地图网格的最大区别就是 0级是 1.40625
     */
    static GridSet TIANDITU = GridSetFactory.createGridSet(
            "TIANDITU",
            SRS.getEPSG4326(),
            BoundingBox.WORLD4326,
            true,
            new double[]{
                    1.40625,
                    0.703125,
                    0.3515625,
                    0.17578125,
                    0.087890625,
                    0.0439453125,
                    0.02197265625,
                    0.010986328125,
                    0.0054931640625,
                    0.00274658203125,
                    0.001373291015625,
                    6.866455078125E-4,
                    3.4332275390625E-4,
                    1.71661376953125E-4,
                    8.58306884765625E-5,
                    4.291534423828125E-5,
                    2.1457672119140625E-5,
                    1.0728836059570312E-5,
                    5.364418029785156E-6,
                    2.682209014892578E-6,
                    1.341104507446289E-6,
                    6.705522537231445E-7,
                    3.3527612686157227E-7},
            null,
            null,
            GridSetFactory.DEFAULT_PIXEL_SIZE_METER,
            null,
            256,
            256,
            false);

    static GridSubset WORLD_GRID_TDT = GridSubsetFactory.
            createGridSubSet(
                    TIANDITU,
                    BoundingBox.WORLD4326,
                    0,
                    21
            );

    public static GridSubset getWorldGrid3857() {
        return WORLD_GRID_3857;
    }

    public static GridSubset getTdtGrid4490() {
        return WORLD_GRID_TDT;
    }
}
