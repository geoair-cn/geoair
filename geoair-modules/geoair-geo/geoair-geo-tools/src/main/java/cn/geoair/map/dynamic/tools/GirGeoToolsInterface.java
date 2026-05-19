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
 * 地理空间动态工具包 统一接口
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public interface GirGeoToolsInterface {

    /** 获取几何对象转数组操作接口 */
    GirGeom2ArrayOpt getGeom2ArrayOpt();

    /** 获取地理数据转换操作接口 */
    GirGeoFormatOpt getFormatOpt();

    /** 获取坐标转换操作接口 */
    GirCoordinateConvertOpt getCoordinateOpt();

    /** 获取WGS84坐标系瓦片转换操作接口 */
    GirTileConverterOpt getTileGrid4326Opt();

    /** 获取WGS84坐标系瓦片转换操作接口(非等轴) */
    GirTileConverterOpt getTileGrid4326SeparateOpt();

    /** 必应地图QuadKey 的生成与解析接口 */
    GirBingMapQuadKeyOpt getTileGridBingMapOpt();

    /** 获取Web墨卡托坐标系瓦片转换操作接口 */
    GirTileConverterOpt getTileGrid3857Opt();

    /** 获取地理测量操作接口 */
    GirGeoMeasureOpt getMeasureOpt();

    /** 获取地理数据合并操作接口 */
    GirGeoMergeOpt getMergeOpt();

    /** 获取SRID转换操作接口 */
    GirSridConvertOpt getSridOpt();

    /** 获取分页执行器接口 */
    <T> PageActuator<T> getPageActuatorOpt(PageConditionDef<T> pageConditionDef);
}
