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
 * 地理空间工具的配置化入口。
 *
 * <p>一个 {@link ToolsConfig} 对象对应一个入口实例及其格式转换、SRID 转换等共享组件。
 * {@link #getInstance(ToolsConfig)} 按配置对象的<strong>引用身份</strong>缓存，而不是按配置字段值比较；
 * 因此需要复用缓存时，应复用同一个 {@code ToolsConfig} 实例。未传配置时使用
 * {@link #defaultInstance()} 的默认配置。</p>
 *
 * @author 张逢吉
 */
@Getter
public class GirGeoTools implements GirGeoToolsInterface {

    private static volatile GirGeoTools INSTANCE;

    /** 按 ToolsConfig 对象身份缓存的工具入口，避免同一配置重复创建 CRS 缓存等重资源。 */
    private static final Map<ToolsConfig, GirGeoTools> CONFIGURED_INSTANCES =
            Collections.synchronizedMap(new IdentityHashMap<ToolsConfig, GirGeoTools>());

    /** 此入口及其子工具使用的配置对象。 */
    protected ToolsConfig advToolsConfig;

    private final GirGeoFormatOpt formatOpt;
    private final GirGeoMeasureOpt measureOpt;
    private final GirSridConvertOpt sridOpt;


    /**
     * 使用指定配置创建工具入口。
     *
     * @param advToolsConfig 工具配置；为 {@code null} 时创建默认配置
     */
    public GirGeoTools(ToolsConfig advToolsConfig) {
        this.advToolsConfig = advToolsConfig == null ? new ToolsConfig() : advToolsConfig;
        this.formatOpt = GirFormatUtils.getInstance(this.advToolsConfig);
        this.sridOpt = GirSridConvertUtils.getInstance(this.advToolsConfig);
        this.measureOpt = GirGeoMeasureUtils.getInstance(this.advToolsConfig);
    }

    /** 使用默认配置创建独立工具入口，不参与 {@link #defaultInstance()} 的单例缓存。 */
    public GirGeoTools() {
        this(new ToolsConfig());
    }

    /**
     * 获取与配置对象绑定的工具入口。
     *
     * @param advToolsConfig 工具配置；为 {@code null} 时返回默认入口
     * @return 与该配置对象对应的工具入口
     */
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
     * 获取进程内共享的默认工具入口。
     *
     * @return 使用默认 {@link ToolsConfig} 的工具入口
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
     * @deprecated 请使用 {@link #defaultInstance()}，该方法名无法表达默认实例的含义。
     * @return 默认工具入口
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
