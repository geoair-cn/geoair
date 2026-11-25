package cn.geoair.map.dynamic.file.csv;

import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanMappingMeta;
import cn.hutool.core.util.StrUtil;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CsvSchemaSupport {

    private CsvSchemaSupport() {}

    static List<String> resolveHeaders(List<String> rawHeaders) {
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < rawHeaders.size(); i++) {
            String header = rawHeaders.get(i);
            if (StrUtil.isBlank(header)) {
                header = "column_" + (i + 1);
            }
            headers.add(sanitizeHeader(header, headers));
        }
        return headers;
    }

    static SimpleFeatureType buildFeatureType(List<String> headers, CsvLinkInfo linkInfo) {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("csv_feature");
        if (linkInfo.getSrid() > 0) {
            builder.setCRS(
                    cn.geoair.map.dynamic.tools.GirGeoTools.defaultInstance()
                            .getSridOpt()
                            .getCRS(linkInfo.getSrid()));
        }
        for (String header : headers) {
            builder.add(header, String.class);
        }
        if (linkInfo.getGeometryMode() != CsvGeometryMode.NONE) {
            builder.add(linkInfo.getGeometryColumnName(), Geometry.class);
        }
        return builder.buildFeatureType();
    }

    static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    static int findColumnIndex(List<String> headers, String columnName) {
        if (StrUtil.isBlank(columnName)) {
            return -1;
        }
        String normalized = normalize(columnName);
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (columnName.equalsIgnoreCase(header)) {
                return i;
            }
            if (normalized.equals(normalize(header))) {
                return i;
            }
            String resolved =
                    AdvBeanMappingMeta.of(HeaderHolder.class).resolveColumnName(header, false);
            if (normalized.equals(normalize(resolved))) {
                return i;
            }
        }
        return -1;
    }

    static List<String> resolveWriterHeaders(SimpleFeatureType featureType, CsvLinkInfo linkInfo) {
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < featureType.getAttributeCount(); i++) {
            String name = featureType.getDescriptor(i).getLocalName();
            if (!linkInfo.getGeometryColumnName().equals(name)) {
                headers.add(name);
            }
        }
        if (linkInfo.getGeometryMode() == CsvGeometryMode.LON_LAT) {
            headers.add(linkInfo.getLongitudeColumnName());
            headers.add(linkInfo.getLatitudeColumnName());
        } else if (linkInfo.getGeometryMode() == CsvGeometryMode.WKT) {
            headers.add(linkInfo.getWktColumnName());
        }
        return headers;
    }

    static List<String> parseLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == delimiter && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static String sanitizeHeader(String header, List<String> existingHeaders) {
        String cleaned = header.trim();
        cleaned = cleaned.replace("\"", "").replace("`", "");
        if (cleaned.contains(".")) {
            cleaned = cleaned.substring(cleaned.lastIndexOf('.') + 1);
        }
        if (StrUtil.isBlank(cleaned)) {
            cleaned = "column_" + (existingHeaders.size() + 1);
        }
        String candidate = cleaned;
        int suffix = 1;
        while (existingHeaders.contains(candidate)) {
            candidate = cleaned + "_" + suffix++;
        }
        return candidate;
    }

    private static final class HeaderHolder {
        private String value;
    }
}
