package cn.geoair.map.dynamic.file.test;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;

/**
 * @author ：张逢吉
 * @date ：Created in 17:06 @description： TODO
 */
public class GeoToolsUtils {

    /**
     * 在原有 SimpleFeatureType 基础上 新增字段
     *
     * @param originalType 原始的featureType（从shp/geojson读出来的）
     * @param fieldName 新增字段名
     * @param fieldType 新增字段类型（String.class, Integer.class, Double.class 等）
     * @return 新增字段后的新 SimpleFeatureType
     */
    public static SimpleFeatureType addFieldToFeatureType(
            SimpleFeatureType originalType, String fieldName, Class<?> fieldType) {
        // 1. 基于原始类型创建 builder
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.init(originalType);
        builder.setName(originalType.getName());

        // 2. 检查字段是否已存在，不存在才添加
        boolean exists =
                originalType.getAttributeDescriptors().stream()
                        .map(AttributeDescriptor::getLocalName)
                        .anyMatch(fieldName::equalsIgnoreCase);

        if (!exists) {
            builder.add(fieldName, fieldType);
        }

        // 3. 构建新的 FeatureType
        return builder.buildFeatureType();
    }
}
