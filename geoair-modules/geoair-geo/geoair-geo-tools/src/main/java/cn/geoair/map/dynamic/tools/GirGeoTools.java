package cn.geoair.map.dynamic.tools;

import cn.geoair.map.dynamic.tools.array.GirGeom2ArrayOpt;
import cn.geoair.map.dynamic.tools.array.GirGeom2ArrayUtils;
import cn.geoair.map.dynamic.tools.convert.GirFormatUtils;
import cn.geoair.map.dynamic.tools.convert.GirGeoFormatOpt;
import cn.geoair.map.dynamic.tools.coordinate.GirCoordinateConvertOpt;
import cn.geoair.map.dynamic.tools.coordinate.GirCoordinateUtils;
import cn.geoair.map.dynamic.tools.grid.GirBingMapQuadKeyOpt;
import cn.geoair.map.dynamic.tools.grid.GirTileConverterOpt;
import cn.geoair.map.dynamic.tools.grid.bing.BingMapQuadKeyUtils;
import cn.geoair.map.dynamic.tools.grid.converter.TileConverter3857Utils;
import cn.geoair.map.dynamic.tools.grid.converter.Wgs84EqualAxisTileUtils;
import cn.geoair.map.dynamic.tools.grid.converter.Wgs84SeparateAxisTileUtils;
import cn.geoair.map.dynamic.tools.measure.GirGeoMeasureOpt;
import cn.geoair.map.dynamic.tools.measure.GirGeoMeasureUtils;
import cn.geoair.map.dynamic.tools.merge.GirGeoMergeOpt;
import cn.geoair.map.dynamic.tools.merge.GirGeoMergeUtils;
import cn.geoair.map.dynamic.tools.page.PageActuator;
import cn.geoair.map.dynamic.tools.page.PageConditionDef;
import cn.geoair.map.dynamic.tools.srid.GirSridConvertOpt;
import cn.geoair.map.dynamic.tools.srid.GirSridConvertUtils;
import lombok.Getter;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/24 15:57
 * @description： 地理空间动态工具包总入口 提供对所有工具类的统一访问接口
 */
@Getter
public class GirGeoTools implements GirGeoToolsInterface {

    private static volatile GirGeoTools INSTANCE;

    /** 按 ToolsConfig 对象身份缓存的工具入口，避免同一配置重复创建 CRS 缓存等重资源。 */
    private static final Map<ToolsConfig, GirGeoTools> CONFIGURED_INSTANCES =
            Collections.synchronizedMap(new IdentityHashMap<ToolsConfig, GirGeoTools>());

    protected ToolsConfig advToolsConfig;

    private final GirGeoFormatOpt formatOpt;
    private final GirGeoMeasureOpt measureOpt;
    private final GirSridConvertOpt sridOpt;


    public GirGeoTools(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig == null ? new ToolsConfig() : advToolsConfig;
        this.formatOpt = GirFormatUtils.getInstance(this.advToolsConfig);
        this.sridOpt = GirSridConvertUtils.getInstance(this.advToolsConfig);
        this.measureOpt = GirGeoMeasureUtils.getInstance(this.advToolsConfig);
    }

    public GirGeoTools() {
        this(new ToolsConfig());
    }

    public static GirGeoTools getInstance(ToolsConfig advToolsConfig) {
        if (advToolsConfig == null) {
            return defaultInstance();
        }
        synchronized (CONFIGURED_INSTANCES) {
            GirGeoTools tools = CONFIGURED_INSTANCES.get(advToolsConfig);
            if (tools == null) {
                tools = new GirGeoTools(advToolsConfig);
                CONFIGURED_INSTANCES.put(advToolsConfig, tools);
            }
            return tools;
        }
    }

    /**
     * 获取默认配置实例
     *
     * @return
     */
    public static GirGeoTools defaultInstance() {
        if (INSTANCE == null) {
            synchronized (GirGeoTools.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GirGeoTools();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * 命名不规范，弃用
     *
     * @return
     */
    @Deprecated
    public static GirGeoTools me() {
        return defaultInstance();
    }


    @Override
    public GirGeom2ArrayOpt getGeom2ArrayOpt() {
        return GirGeom2ArrayUtils.getInstance(advToolsConfig);
    }

    @Override
    public GirGeoFormatOpt getFormatOpt() {
        return formatOpt;
    }

    @Override
    public GirCoordinateConvertOpt getCoordinateOpt() {
        return GirCoordinateUtils.getInstance(advToolsConfig);
    }

    @Override
    public GirTileConverterOpt getTileGrid4326Opt() {
        return Wgs84EqualAxisTileUtils.getInstance(advToolsConfig);
    }

    @Override
    public GirTileConverterOpt getTileGrid4326SeparateOpt() {
        return Wgs84SeparateAxisTileUtils.getInstance(advToolsConfig);
    }

    @Override
    public GirBingMapQuadKeyOpt getTileGridBingMapOpt() {
        return BingMapQuadKeyUtils.getInstance(advToolsConfig);
    }

    @Override
    public GirTileConverterOpt getTileGrid3857Opt() {
        return TileConverter3857Utils.getInstance(advToolsConfig);
    }

    @Override
    public GirGeoMeasureOpt getMeasureOpt() {
        return measureOpt;
    }

    @Override
    public GirGeoMergeOpt getMergeOpt() {
        return GirGeoMergeUtils.getInstance(advToolsConfig);
    }

    @Override
    public GirSridConvertOpt getSridOpt() {
        return sridOpt;
    }

    @Override
    public <T> PageActuator<T> getPageActuatorOpt(PageConditionDef<T> pageConditionDef) {
        return PageActuator.getInstance(pageConditionDef);
    }
}
