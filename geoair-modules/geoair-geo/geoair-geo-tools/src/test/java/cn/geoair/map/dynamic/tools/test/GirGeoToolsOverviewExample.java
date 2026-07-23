package cn.geoair.map.dynamic.tools.test;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.GirGeoFormatOpt;
import cn.geoair.map.dynamic.tools.coordinate.GirCoordinateConvertOpt;
import cn.geoair.map.dynamic.tools.grid.GirTileConverterOpt;
import cn.geoair.map.dynamic.tools.measure.GirGeoMeasureOpt;
import cn.geoair.map.dynamic.tools.merge.GirGeoMergeOpt;
import cn.geoair.map.dynamic.tools.srid.GirSridConvertOpt;

/**
 * geoair-geo-tools 总入口示例
 *
 * <p>展示如何通过 GirGeoTools.defaultInstance() 获取各类核心 API。
 */
public class GirGeoToolsOverviewExample {

    public static void main(String[] args) {
        GirGeoTools tools = GirGeoTools.defaultInstance();

        GirCoordinateConvertOpt coordinateOpt = tools.getCoordinateOpt();
        GirGeoFormatOpt formatOpt = tools.getFormatOpt();
        GirGeoMeasureOpt measureOpt = tools.getMeasureOpt();
        GirGeoMergeOpt mergeOpt = tools.getMergeOpt();
        GirSridConvertOpt sridOpt = tools.getSridOpt();
        GirTileConverterOpt tile4326Opt = tools.getTileGrid4326Opt();
        GirTileConverterOpt tile3857Opt = tools.getTileGrid3857Opt();

        System.out.println("coordinateOpt = " + coordinateOpt.getClass().getSimpleName());
        System.out.println("formatOpt = " + formatOpt.getClass().getSimpleName());
        System.out.println("measureOpt = " + measureOpt.getClass().getSimpleName());
        System.out.println("mergeOpt = " + mergeOpt.getClass().getSimpleName());
        System.out.println("sridOpt = " + sridOpt.getClass().getSimpleName());
        System.out.println("tile4326Opt = " + tile4326Opt.getClass().getSimpleName());
        System.out.println("tile3857Opt = " + tile3857Opt.getClass().getSimpleName());
    }
}
