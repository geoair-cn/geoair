package cn.geoair.map.dynamic.statics.mvt.v4.input;

import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Geometry;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GeoJSON Feature → 切片链路行对象的映射。
 *
 * <p>映射规则与 V3 的 {@code V3GeoJsonFeatureMapper} 保持一致（属性字段在前、几何字段在最后，
 * 属性值为空时给空串），这样 V4 与 V3 的产物才具备可比性。V4 在此之上多一层
 * {@code --attribute-type} 的类型强制。</p>
 *
 * @author 张逢吉
 */
public final class V4GeoJsonFeatureMapper {

    private V4GeoJsonFeatureMapper() {
    }

    /** 把一个 GeoJSON Feature 映射为行对象。 */
    public static GirAdvOneRow toRow(JsonNode feature, String geomFieldName,
            String idFieldName, int sourceSrid) {
        if (feature == null || !feature.isObject()
                || !"Feature".equals(feature.path("type").asText())) {
            throw new IllegalArgumentException("GeoJSON 节点不是 Feature");
        }
        JsonNode geometryNode = feature.get("geometry");
        if (geometryNode == null || geometryNode.isNull()) {
            throw new IllegalArgumentException("GeoJSON Feature 缺少 geometry");
        }
        Geometry geometry = GirGeoTools.defaultInstance().getFormatOpt()
                .geojsonToJtsGeometry(geometryNode.toString(), false);
        if (geometry == null || geometry.isEmpty()) {
            throw new IllegalArgumentException("GeoJSON Feature geometry 为空或无法解析");
        }
        geometry.setSRID(sourceSrid);

        Map<String, Object> values = new LinkedHashMap<>();
        JsonNode properties = feature.get("properties");
        if (properties != null && properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                values.put(field.getKey(), toAttributeValue(field.getValue()));
            }
        }
        if (idFieldName != null && !idFieldName.trim().isEmpty()
                && !values.containsKey(idFieldName) && feature.has("id")) {
            values.put(idFieldName, toAttributeValue(feature.get("id")));
        }
        values.put(geomFieldName, geometry);
        return GirAdvOneRow.ofByMap(values);
    }

    /**
     * 按 {@code --attribute-type} 的声明强制属性类型。
     *
     * <p>转换失败时保留原值 —— 切片的职责是把数据切出来，不是替用户清洗数据；
     * 一条脏数据不该让整批任务失败。转换失败的条数由返回值上报，便于任务结束后查看。</p>
     *
     * @return 转换失败的字段个数
     */
    public static int applyAttributeTypes(Map<String, Object> row,
            Map<String, String> attributeTypes, String geomFieldName) {
        if (attributeTypes == null || attributeTypes.isEmpty()) {
            return 0;
        }
        int failed = 0;
        for (Map.Entry<String, String> entry : attributeTypes.entrySet()) {
            String field = entry.getKey();
            if (field == null || field.trim().isEmpty() || field.equals(geomFieldName)) {
                continue;
            }
            if (!row.containsKey(field)) {
                continue;
            }
            Object raw = row.get(field);
            Object converted = coerce(raw, entry.getValue());
            if (converted == null) {
                failed++;
                continue;
            }
            row.put(field, converted);
        }
        return failed;
    }

    /** 单值类型强制；无法转换时返回 null 表示"保持原值"。 */
    private static Object coerce(Object raw, String type) {
        if (raw == null || type == null) {
            return null;
        }
        String declared = type.trim().toLowerCase();
        String text = String.valueOf(raw);
        try {
            switch (declared) {
                case "int":
                case "integer":
                    return (int) Math.round(Double.parseDouble(text));
                case "long":
                    return Math.round(Double.parseDouble(text));
                case "double":
                case "float":
                    return Double.parseDouble(text);
                case "boolean":
                case "bool":
                    return Boolean.parseBoolean(text);
                case "string":
                    return text;
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 与 V3 相同的属性取值规则：null 给空串，其余按 JSON 原生类型落地。 */
    private static Object toAttributeValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }
        if (value.isTextual()) {
            return value.textValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isInt()) {
            return value.intValue();
        }
        if (value.isIntegralNumber()) {
            return value.longValue();
        }
        if (value.isFloatingPointNumber()) {
            return value.doubleValue();
        }
        return value.toString();
    }
}
