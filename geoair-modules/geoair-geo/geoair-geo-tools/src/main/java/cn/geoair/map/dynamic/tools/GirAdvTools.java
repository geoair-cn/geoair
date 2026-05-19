package cn.geoair.map.dynamic.tools;

import cn.geoair.map.dynamic.tools.array.GirGeom2ArrayOpt;
import cn.geoair.map.dynamic.tools.convert.GirGeoFormatOpt;
import cn.geoair.map.dynamic.tools.coordinate.GirCoordinateConvertOpt;
import cn.geoair.map.dynamic.tools.grid.GirBingMapQuadKeyOpt;
import cn.geoair.map.dynamic.tools.grid.GirTileConverterOpt;
import cn.geoair.map.dynamic.tools.measure.GirGeoMeasureOpt;
import cn.geoair.map.dynamic.tools.merge.GirGeoMergeOpt;
import cn.geoair.map.dynamic.tools.page.PageActuator;
import cn.geoair.map.dynamic.tools.page.PageConditionDef;
import cn.geoair.map.dynamic.tools.srid.GirSridConvertOpt;

/**
 * 地理空间动态工具包总入口 提供对所有工具类的统一访问接口
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public class GirAdvTools {

    /**
     * 获取几何对象转数组操作接口
     *
     * @return GirGeom2ArrayOpt 几何对象转数组工具接口实例
     */
    public static GirGeom2ArrayOpt getGeom2ArrayOpt() {
        return GirGeoTools.me().getGeom2ArrayOpt();
    }

    /**
     * 获取地理数据转换操作接口
     *
     * @return GirGeoConvertOpt 地理数据转换工具接口实例
     */
    public static GirGeoFormatOpt getFormatOpt() {
        return GirGeoTools.me().getFormatOpt();
    }

    /**
     * 获取坐标转换操作接口
     *
     * @return GirCoordinateConvertOpt 坐标转换工具接口实例
     */
    public static GirCoordinateConvertOpt getCoordinateOpt() {
        return GirGeoTools.me().getCoordinateOpt();
    }

    /**
     * 获取WGS84坐标系瓦片转换操作接口
     *
     * @return GirTileConverterOpt WGS84瓦片转换工具接口实例
     */
    public static GirTileConverterOpt getTileGrid4326Opt() {
        return GirGeoTools.me().getTileGrid4326Opt();
    }

    /**
     * 获取WGS84坐标系瓦片转换操作接口(非等轴)
     *
     * @return GirTileConverterOpt WGS84瓦片转换工具接口实例
     */
    public static GirTileConverterOpt getTileGrid4326SeparateOpt() {
        return GirGeoTools.me().getTileGrid4326SeparateOpt();
    }

    /**
     * 必应地图QuadKey 的生成与解析接口
     *
     * @return 必应地图QuadKey的工具接口实例
     */
    public static GirBingMapQuadKeyOpt getTileGridBingMapOpt() {
        return GirGeoTools.me().getTileGridBingMapOpt();
    }

    /**
     * 获取Web墨卡托坐标系瓦片转换操作接口
     *
     * @return GirTileConverterOpt Web墨卡托瓦片转换工具接口实例
     */
    public static GirTileConverterOpt getTileGrid3857Opt() {
        return GirGeoTools.me().getTileGrid3857Opt();
    }

    /**
     * 获取地理测量操作接口
     *
     * @return GirGeoMeasureOpt 地理测量工具接口实例
     */
    public static GirGeoMeasureOpt getMeasureOpt() {
        return GirGeoTools.me().getMeasureOpt();
    }

    /**
     * 获取地理数据合并操作接口
     *
     * @return GirGeoMergeOpt 地理数据合并工具接口实例
     */
    public static GirGeoMergeOpt getMergeOpt() {
        return GirGeoTools.me().getMergeOpt();
    }

    /**
     * 获取SRID转换操作接口
     *
     * @return GirSridConvertOpt SRID转换工具接口实例
     */
    public static GirSridConvertOpt getSridOpt() {
        return GirGeoTools.me().getSridOpt();
    }

    /**
     * 获取分页执行器接口
     *
     * @param pageConditionDef 分页定义接口实例
     * @return GirPageActuatorOpt 分页执行工具接口实例
     */
    public static <T> PageActuator<T> getPageActuatorOpt(PageConditionDef<T> pageConditionDef) {
        return GirGeoTools.me().getPageActuatorOpt(pageConditionDef);
    }
}
