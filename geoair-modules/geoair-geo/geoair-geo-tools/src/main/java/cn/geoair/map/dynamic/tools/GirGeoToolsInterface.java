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
 * 地理空间工具包的统一入口契约。
 *
 * <p>实现类负责按 {@link ToolsConfig} 提供各类工具；接口本身不定义全局单例语义。
 *
 * @author 张逢吉
 * @date 2024/12/05
 */
public interface GirGeoToolsInterface {

    /**
     * 获取几何对象转数组操作接口
     *
     * @return 几何对象与坐标数组互转工具
     */
    GirGeom2ArrayOpt getGeom2ArrayOpt();

    /**
     * 获取地理数据转换操作接口
     *
     * @return GeoJSON、WKT、WKB 与 JTS 等格式转换工具
     */
    GirGeoFormatOpt getFormatOpt();

    /**
     * 获取坐标转换操作接口
     *
     * @return WGS84、GCJ02、BD09 与 Web Mercator 坐标转换工具
     */
    GirCoordinateConvertOpt getCoordinateOpt();

    /**
     * 获取 WGS84 等轴瓦片转换工具。
     *
     * <p>该网格沿用既有的 Google/XYZ 顶部原点 Y 轴约定。
     *
     * @return EPSG:4326 等轴网格工具
     */
    GirTileConverterOpt getTileGrid4326Opt();

    /**
     * 获取 WGS84 非等轴瓦片转换工具。
     *
     * <p>该网格在 z=3 时为 8 列 × 4 行；Y 轴方向仍默认 Google/XYZ 顶部原点。 如需 TMS 行号，应通过 {@code TileYAxis} 相关 API
     * 显式转换。
     *
     * @return EPSG:4326 非等轴网格工具
     */
    GirTileConverterOpt getTileGrid4326SeparateOpt();

    /**
     * 必应地图QuadKey 的生成与解析接口
     *
     * @return Bing QuadKey 工具
     */
    GirBingMapQuadKeyOpt getTileGridBingMapOpt();

    /**
     * 获取Web墨卡托坐标系瓦片转换操作接口
     *
     * @return EPSG:3857 瓦片网格工具
     */
    GirTileConverterOpt getTileGrid3857Opt();

    /**
     * 获取地理测量操作接口
     *
     * @return 面积、长度和距离测量工具
     */
    GirGeoMeasureOpt getMeasureOpt();

    /**
     * 获取地理数据合并操作接口
     *
     * @return 几何合并工具
     */
    GirGeoMergeOpt getMergeOpt();

    /**
     * 获取SRID转换操作接口
     *
     * @return CRS/SRID 转换工具
     */
    GirSridConvertOpt getSridOpt();

    /**
     * 创建分页执行器。
     *
     * @param pageConditionDef 分页查询、消费与异常处理定义
     * @param <T> 单条记录类型
     * @return 新建的分页执行器；每次调用均返回独立执行器
     */
    <T> PageActuator<T> getPageActuatorOpt(PageConditionDef<T> pageConditionDef);
}
