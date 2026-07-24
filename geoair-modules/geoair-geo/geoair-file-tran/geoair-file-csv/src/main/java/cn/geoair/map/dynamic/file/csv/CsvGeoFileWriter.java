package cn.geoair.map.dynamic.file.csv;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.file.core.exception.ExceptionConsumer;
import cn.geoair.map.dynamic.file.core.exception.GeoFileWriteException;
import cn.geoair.map.dynamic.file.core.link.LinkInfo;
import cn.geoair.map.dynamic.file.core.write.GeoFileWriter;
import cn.geoair.map.dynamic.file.core.write.config.WriteConfig;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.locationtech.jts.geom.Geometry;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CsvGeoFileWriter implements GeoFileWriter {

    private CsvLinkInfo linkInfo;

    private WriteConfig writeConfig = new WriteConfig();

    private BufferedWriter writer;

    private boolean headerWritten = false;

    private List<String> headers = new ArrayList<>();

    @Override
    public void setLinkInfo(LinkInfo linkInfo) {
        if (!(linkInfo instanceof CsvLinkInfo)) {
            throw new IllegalArgumentException("链接信息必须是 CsvLinkInfo 类型");
        }
        this.linkInfo = (CsvLinkInfo) linkInfo;
        this.linkInfo.checkLinkInfo();
    }

    @Override
    public void setWriteConfig(WriteConfig writeConfig) {
        this.writeConfig = writeConfig == null ? new WriteConfig() : writeConfig;
    }

    @Override
    public GeoFileWriter writeHeader(
            SimpleFeatureType featureType, ExceptionConsumer exceptionConsumer) {
        try {
            if (headerWritten) {
                return this;
            }
            headers = CsvSchemaSupport.resolveWriterHeaders(featureType, linkInfo);
            writer =
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(linkInfo.getCsvFilePath()),
                                    Charset.forName(linkInfo.getCharset())));
            writer.write(join(headers));
            writer.newLine();
            headerWritten = true;
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileWriteException("初始化 CSV 写入表头失败", e);
        }
        return this;
    }

    @Override
    public GeoFileWriter writeOneRow(
            GirAdvOneRow girAdvOneRow, ExceptionConsumer exceptionConsumer) {
        try {
            if (!headerWritten || writer == null) {
                throw new IllegalStateException("请先调用 writeHeader");
            }
            if (girAdvOneRow == null || girAdvOneRow.isEmpty()) {
                return this;
            }
            List<String> values = new ArrayList<>();
            Geometry geometry = null;
            Object geomValue = girAdvOneRow.get(linkInfo.getGeometryColumnName());
            if (geomValue instanceof Geometry) {
                geometry = (Geometry) geomValue;
            }
            for (String header : headers) {
                if (linkInfo.getGeometryMode() == CsvGeometryMode.LON_LAT) {
                    if (header.equals(linkInfo.getLongitudeColumnName())) {
                        values.add(
                                escape(
                                        CsvGeometrySupport.writeGeometryValue(
                                                linkInfo, geometry, true)));
                        continue;
                    }
                    if (header.equals(linkInfo.getLatitudeColumnName())) {
                        values.add(
                                escape(
                                        CsvGeometrySupport.writeGeometryValue(
                                                linkInfo, geometry, false)));
                        continue;
                    }
                }
                if (linkInfo.getGeometryMode() == CsvGeometryMode.WKT
                        && header.equals(linkInfo.getWktColumnName())) {
                    values.add(
                            escape(
                                    CsvGeometrySupport.writeGeometryValue(
                                            linkInfo, geometry, true)));
                    continue;
                }
                Object value = girAdvOneRow.get(header);
                values.add(escape(value == null ? "" : String.valueOf(value)));
            }
            writer.write(join(values));
            writer.newLine();
        } catch (Exception e) {
            notifyException(exceptionConsumer, e);
            throw new GeoFileWriteException("写入 CSV 单行数据失败", e);
        }
        return this;
    }

    @Override
    public void close() {
        if (writer == null) {
            return;
        }
        try {
            writer.flush();
            writer.close();
        } catch (Exception e) {
            throw new GeoFileWriteException("关闭 CSV 写入器失败", e);
        } finally {
            writer = null;
            headerWritten = false;
        }
    }

    private String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(linkInfo.getDelimiter());
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needQuote =
                value.indexOf(linkInfo.getDelimiter()) >= 0
                        || value.contains("\"")
                        || value.contains("\n")
                        || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    private void notifyException(ExceptionConsumer exceptionConsumer, Exception e) {
        if (exceptionConsumer != null) {
            exceptionConsumer.accept(e);
        }
    }
}
