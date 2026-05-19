package cn.geoair.map.dynamic.tools;

import lombok.Data;
import lombok.experimental.Accessors;

import org.geotools.geojson.geom.GeometryJSON;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.io.WKTReader;

import java.util.function.Supplier;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/24 15:42
 * @description：全局配置类
 */
@Data
@Accessors(chain = true)
public class ToolsConfig {

    public static ToolsConfig of() {
        return new ToolsConfig();
    }

    // 线程安全（可全局单例）
    private GeometryFactory geometryFactory = new GeometryFactory();
    private WKBWriter wkbWriter = new WKBWriter();
    private GeometryJSON geometryJSON = new GeometryJSON();

    // 非线程安全（只持有构造器，不持有实例）
    private Supplier<WKTReader> wktReaderSupplier = WKTReader::new;
    private Supplier<WKBReader> wkbReaderSupplier = WKBReader::new;

    private int tilePixelSize = 256; // 默认瓦片尺寸
    private int dpi = 96; // 默认DPI（屏幕标准DPI）
}
