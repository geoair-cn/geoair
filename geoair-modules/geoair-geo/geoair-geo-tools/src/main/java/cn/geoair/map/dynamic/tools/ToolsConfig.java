package cn.geoair.map.dynamic.tools;

import lombok.Data;
import lombok.experimental.Accessors;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;

import java.util.function.Supplier;

/**
 * 地理空间工具的可配置依赖与默认参数。
 *
 * <p>该对象是可变的。{@link GirGeoTools#getInstance(ToolsConfig)} 及各工具类会按对象身份
 * 缓存实例，因此建议在创建工具入口前完成配置；入口创建后再修改本对象，不会重建已经创建的
 * 读取器、转换器或缓存。</p>
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class ToolsConfig {

    /**
     * 创建使用默认值的配置对象。
     *
     * @return 新的、可链式设置的配置对象
     */
    public static ToolsConfig of() {
        return new ToolsConfig();
    }

    /** 供 JTS 几何对象创建使用的工厂，可设置 SRID、精度模型等。 */
    private GeometryFactory geometryFactory = new GeometryFactory();

    /** 默认用于输出二维、大端序且携带 SRID 的 WKB 写入器。 */
    private WKBWriter wkbWriter = new WKBWriter(2, ByteOrderValues.BIG_ENDIAN, true);

    /** GeoJSON 与 JTS Geometry 间转换使用的组件。 */
    private GeometryJSON geometryJSON = new GeometryJSON();

    /** WKTReader 提供器；每次解析由调用方取得新的 reader，避免共享可变读取状态。 */
    private Supplier<WKTReader> wktReaderSupplier = WKTReader::new;

    /** WKBReader 提供器；每次解析由调用方取得新的 reader，避免共享可变读取状态。 */
    private Supplier<WKBReader> wkbReaderSupplier = WKBReader::new;

    /** 默认瓦片边长，单位为像素。 */
    private int tilePixelSize = 256;

    /** 默认显示设备 DPI，用于比例尺等元数据计算。 */
    private int dpi = 96;
}
