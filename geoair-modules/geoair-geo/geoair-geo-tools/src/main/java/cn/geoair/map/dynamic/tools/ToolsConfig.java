package cn.geoair.map.dynamic.tools;

import java.util.Objects;
import java.util.function.Supplier;
import lombok.Data;
import lombok.experimental.Accessors;
import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;

/**
 * 地理空间工具的可配置依赖与默认参数。
 *
 * <p>该对象是可变的。{@link GirGeoTools#getInstance(ToolsConfig)} 及各工具类会按对象身份
 * 缓存工具入口，并持有同一个配置对象；对配置的修改会作用于后续操作。为保证调用结果可预期， 建议在创建工具入口前完成配置，且不要在并发使用期间修改本对象。
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

    /** WKB 写入器提供器。每次调用必须返回独立实例，避免并发时共享非线程安全写入器。 */
    private Supplier<WKBWriter> wkbWriterSupplier =
            () -> new WKBWriter(2, ByteOrderValues.BIG_ENDIAN, true);

    /** GeoJSON 转换器提供器。每次调用必须返回独立实例，避免并发时共享非线程安全转换器。 */
    private Supplier<GeometryJSON> geometryJsonSupplier = GeometryJSON::new;

    /** WKTReader 提供器；默认绑定 {@link #geometryFactory}，每次解析取得新的 reader，避免共享可变读取状态。 */
    private Supplier<WKTReader> wktReaderSupplier = () -> new WKTReader(geometryFactory);

    /** WKBReader 提供器；默认绑定 {@link #geometryFactory}，每次解析取得新的 reader，避免共享可变读取状态。 */
    private Supplier<WKBReader> wkbReaderSupplier = () -> new WKBReader(geometryFactory);

    /**
     * 获取一个 WKB 写入器。
     *
     * @deprecated 请使用 {@link #getWkbWriterSupplier()} 配置每次创建独立实例的提供器。
     * @return 当前提供器创建的 WKB 写入器
     */
    @Deprecated
    public WKBWriter getWkbWriter() {
        return getWkbWriterSupplier().get();
    }

    /**
     * 设置共享 WKB 写入器。
     *
     * <p>该写入器会在多线程间被复用，不具备线程安全保证；新代码请改用 {@link #setWkbWriterSupplier(Supplier)}。
     *
     * @param wkbWriter 历史写入器实例
     * @return 当前配置
     */
    @Deprecated
    public ToolsConfig setWkbWriter(final WKBWriter wkbWriter) {
        final WKBWriter sharedWriter = Objects.requireNonNull(wkbWriter, "wkbWriter不能为空");
        this.wkbWriterSupplier = () -> sharedWriter;
        return this;
    }

    /**
     * 获取一个 GeoJSON 转换器。
     *
     * @deprecated 请使用 {@link #getGeometryJsonSupplier()} 配置每次创建独立实例的提供器。
     * @return 当前提供器创建的 GeoJSON 转换器
     */
    @Deprecated
    public GeometryJSON getGeometryJSON() {
        return getGeometryJsonSupplier().get();
    }

    /**
     * 设置共享 GeoJSON 转换器。
     *
     * <p>该转换器会在多线程间被复用，不具备线程安全保证；新代码请改用 {@link #setGeometryJsonSupplier(Supplier)}。
     *
     * @param geometryJSON 历史转换器实例
     * @return 当前配置
     */
    @Deprecated
    public ToolsConfig setGeometryJSON(final GeometryJSON geometryJSON) {
        final GeometryJSON sharedGeometryJson =
                Objects.requireNonNull(geometryJSON, "geometryJSON不能为空");
        this.geometryJsonSupplier = () -> sharedGeometryJson;
        return this;
    }

    /** 默认瓦片边长，单位为像素。 */
    private int tilePixelSize = 256;

    /** 默认显示设备 DPI，用于比例尺等元数据计算。 */
    private int dpi = 96;
}
