package cn.geoair.map.dynamic.file.csv;

import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import java.io.File;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class CsvLinkInfo extends LinkInfo {

    private String csvFilePath;

    private String charset = "UTF-8";

    private char delimiter = ',';

    private boolean hasHeader = true;

    private int srid = 4326;

    private CsvGeometryMode geometryMode = CsvGeometryMode.NONE;

    private String longitudeColumnName = "lon";

    private String latitudeColumnName = "lat";

    private String wktColumnName = "wkt";

    private String geometryColumnName = "geometry";

    @Override
    public void checkLinkInfo() {
        if (csvFilePath == null || csvFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV 文件路径不能为空");
        }
        File csvFile = new File(csvFilePath);
        if (csvFile.exists()) {
            if (!csvFile.isFile()) {
                throw new IllegalArgumentException("指定路径不是文件：" + csvFilePath);
            }
        } else {
            File parentDir = csvFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                throw new IllegalArgumentException("无法创建 csv 父目录：" + parentDir.getAbsolutePath());
            }
        }
        if (!csvFile.getName().toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("文件不是合法的 CSV 文件：" + csvFilePath);
        }
        if (geometryMode == CsvGeometryMode.LON_LAT) {
            if (isBlank(longitudeColumnName) || isBlank(latitudeColumnName)) {
                throw new IllegalArgumentException("经纬度模式下必须配置 longitudeColumnName 和 latitudeColumnName");
            }
        }
        if (geometryMode == CsvGeometryMode.WKT && isBlank(wktColumnName)) {
            throw new IllegalArgumentException("WKT 模式下必须配置 wktColumnName");
        }
        if (isBlank(geometryColumnName)) {
            throw new IllegalArgumentException("geometryColumnName 不能为空");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
