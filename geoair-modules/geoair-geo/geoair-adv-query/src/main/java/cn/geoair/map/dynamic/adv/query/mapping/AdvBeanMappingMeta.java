package cn.geoair.map.dynamic.adv.query.mapping;

import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.map.dynamic.adv.anno.GirTransient;
import cn.hutool.core.bean.BeanDesc;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.PropDesc;
import cn.hutool.core.util.StrUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 * @author ：张逢吉
 * @date ：Created in 2026/7/22
 * @description： Bean映射元数据
 */
public class AdvBeanMappingMeta {

    private static final Map<Class<?>, AdvBeanMappingMeta> CACHE = new HashMap<>();

    public static AdvBeanMappingMeta of(Class<?> beanClass) {
        synchronized (CACHE) {
            AdvBeanMappingMeta meta = CACHE.get(beanClass);
            if (meta == null) {
                meta = new AdvBeanMappingMeta(beanClass);
                CACHE.put(beanClass, meta);
            }
            return meta;
        }
    }

    private final Class<?> beanClass;
    private final boolean mapType;
    private final List<AdvBeanPropertyMeta> properties = new ArrayList<>();
    private final List<AdvBeanPropertyMeta> idProperties = new ArrayList<>();
    private final Map<String, AdvBeanPropertyMeta> exactLookup = new LinkedHashMap<>();
    private final Map<String, AdvBeanPropertyMeta> normalizedLookup = new LinkedHashMap<>();

    private AdvBeanMappingMeta(Class<?> beanClass) {
        this.beanClass = beanClass;
        this.mapType = Map.class.isAssignableFrom(beanClass);
        build();
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public boolean isMapType() {
        return mapType;
    }

    public List<AdvBeanPropertyMeta> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    public List<AdvBeanPropertyMeta> getWritableProperties(List<String> ignoreFieldNames) {
        if (mapType) {
            return Collections.emptyList();
        }
        if (ignoreFieldNames == null || ignoreFieldNames.isEmpty()) {
            List<AdvBeanPropertyMeta> all = new ArrayList<>();
            for (AdvBeanPropertyMeta property : properties) {
                if (!property.isIgnored()) {
                    all.add(property);
                }
            }
            return all;
        }
        List<AdvBeanPropertyMeta> list = new ArrayList<>();
        for (AdvBeanPropertyMeta property : properties) {
            if (property.isIgnored()) {
                continue;
            }
            if (matchesIgnore(property, ignoreFieldNames)) {
                continue;
            }
            list.add(property);
        }
        return list;
    }

    public List<String> getIdColumnNames(boolean toUnderlineCase) {
        List<String> list = new ArrayList<>();
        for (AdvBeanPropertyMeta idProperty : idProperties) {
            list.add(idProperty.resolveColumnName(toUnderlineCase));
        }
        return list;
    }

    public List<String> getIdPropertyNames() {
        List<String> list = new ArrayList<>();
        for (AdvBeanPropertyMeta idProperty : idProperties) {
            list.add(idProperty.getPropertyName());
        }
        return list;
    }

    public AdvBeanPropertyMeta resolvePropertyByColumnOrProperty(String name) {
        if (mapType) {
            return null;
        }
        String cleanedName = cleanName(name);
        if (StrUtil.isBlank(cleanedName)) {
            return null;
        }
        AdvBeanPropertyMeta meta = exactLookup.get(cleanedName.toLowerCase(Locale.ROOT));
        if (meta != null) {
            return meta;
        }
        return normalizedLookup.get(normalize(cleanedName));
    }

    public String resolveColumnName(String fieldOrColumnName, boolean toUnderlineCase) {
        if (StrUtil.isBlank(fieldOrColumnName)) {
            return fieldOrColumnName;
        }
        if (mapType) {
            return toUnderlineCase ? StrUtil.toUnderlineCase(fieldOrColumnName) : fieldOrColumnName;
        }
        AdvBeanPropertyMeta property = resolvePropertyByColumnOrProperty(fieldOrColumnName);
        if (property != null) {
            return property.resolveColumnName(toUnderlineCase);
        }
        return toUnderlineCase ? StrUtil.toUnderlineCase(fieldOrColumnName) : fieldOrColumnName;
    }

    private void build() {
        if (mapType) {
            return;
        }
        BeanDesc beanDesc = BeanUtil.getBeanDesc(beanClass);
        Map<String, PropDesc> propMap = beanDesc.getPropMap(false);
        if (propMap == null || propMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, PropDesc> entry : propMap.entrySet()) {
            PropDesc propDesc = entry.getValue();
            Field field = propDesc.getField();
            if (field == null) {
                continue;
            }
            field.setAccessible(true);
            AdvBeanPropertyMeta propertyMeta = AdvBeanPropertyMeta.of(beanClass, field);
            properties.add(propertyMeta);
            registerLookup(propertyMeta.getPropertyName(), propertyMeta);
            registerLookup(propertyMeta.resolveColumnName(false), propertyMeta);
            registerLookup(StrUtil.toUnderlineCase(propertyMeta.getPropertyName()), propertyMeta);
            registerLookup(
                    StrUtil.toUnderlineCase(propertyMeta.resolveColumnName(false)), propertyMeta);
            if (propertyMeta.isId()) {
                idProperties.add(propertyMeta);
            }
        }
    }

    private void registerLookup(String key, AdvBeanPropertyMeta meta) {
        String cleaned = cleanName(key);
        if (StrUtil.isBlank(cleaned)) {
            return;
        }
        exactLookup.put(cleaned.toLowerCase(Locale.ROOT), meta);
        normalizedLookup.put(normalize(cleaned), meta);
    }

    private boolean matchesIgnore(
            AdvBeanPropertyMeta property, Collection<String> ignoreFieldNames) {
        for (String ignore : ignoreFieldNames) {
            if (ignore == null) {
                continue;
            }
            String cleanedIgnore = cleanName(ignore);
            if (StrUtil.isBlank(cleanedIgnore)) {
                continue;
            }
            String ignoreLower = cleanedIgnore.toLowerCase(Locale.ROOT);
            if (ignoreLower.equals(property.getPropertyName().toLowerCase(Locale.ROOT))) {
                return true;
            }
            String columnName = property.resolveColumnName(false);
            if (columnName != null && ignoreLower.equals(columnName.toLowerCase(Locale.ROOT))) {
                return true;
            }
            if (normalize(cleanedIgnore).equals(normalize(property.getPropertyName()))) {
                return true;
            }
            if (columnName != null && normalize(cleanedIgnore).equals(normalize(columnName))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String name) {
        if (name == null) {
            return null;
        }
        return name.replace("_", "").trim().toLowerCase(Locale.ROOT);
    }

    private static String cleanName(String name) {
        if (name == null) {
            return null;
        }
        String cleaned = name.trim();
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\""))
                || (cleaned.startsWith("`") && cleaned.endsWith("`"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if ((cleaned.startsWith("[") && cleaned.endsWith("]"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < cleaned.length() - 1) {
            cleaned = cleaned.substring(dotIndex + 1);
        }
        return cleaned;
    }

    public static final class AdvBeanPropertyMeta {
        private final Class<?> beanClass;
        private final Field field;
        private final String propertyName;
        private final String explicitColumnName;
        private final Class<?> propertyType;
        private final boolean ignored;
        private final boolean id;

        private AdvBeanPropertyMeta(
                Class<?> beanClass,
                Field field,
                String propertyName,
                String explicitColumnName,
                Class<?> propertyType,
                boolean ignored,
                boolean id) {
            this.beanClass = beanClass;
            this.field = field;
            this.propertyName = propertyName;
            this.explicitColumnName = explicitColumnName;
            this.propertyType = propertyType;
            this.ignored = ignored;
            this.id = id;
        }

        public static AdvBeanPropertyMeta of(Class<?> beanClass, Field field) {
            String propertyName = field.getName();
            String explicitColumnName = resolveExplicitColumnName(field);
            boolean ignored =
                    field.getAnnotation(GirTransient.class) != null
                            || field.getAnnotation(Transient.class) != null;
            boolean id = field.getAnnotation(Id.class) != null;
            GaModelField gaModelField = field.getAnnotation(GaModelField.class);
            if (gaModelField != null && gaModelField.isID()) {
                id = true;
            }
            return new AdvBeanPropertyMeta(
                    beanClass,
                    field,
                    propertyName,
                    explicitColumnName,
                    field.getType(),
                    ignored,
                    id);
        }

        public Class<?> getBeanClass() {
            return beanClass;
        }

        public Field getField() {
            return field;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Class<?> getPropertyType() {
            return propertyType;
        }

        public boolean isIgnored() {
            return ignored;
        }

        public boolean isId() {
            return id;
        }

        public String resolveColumnName(boolean toUnderlineCase) {
            if (StrUtil.isNotBlank(explicitColumnName)) {
                return explicitColumnName;
            }
            return toUnderlineCase ? StrUtil.toUnderlineCase(propertyName) : propertyName;
        }

        public Object readValue(Object bean) {
            try {
                field.setAccessible(true);
                return field.get(bean);
            } catch (Exception e) {
                try {
                    return BeanUtil.getProperty(bean, propertyName);
                } catch (Exception ex) {
                    return null;
                }
            }
        }

        public void writeValue(Object bean, Object value) {
            try {
                field.setAccessible(true);
                field.set(bean, value);
            } catch (Exception e) {
                try {
                    BeanUtil.setFieldValue(bean, propertyName, value);
                } catch (Exception ex) {
                    BeanUtil.setProperty(bean, propertyName, value);
                }
            }
        }

        private static String resolveExplicitColumnName(Field field) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && StrUtil.isNotBlank(column.name())) {
                return column.name();
            }
            GaModelField gaModelField = field.getAnnotation(GaModelField.class);
            if (gaModelField != null && StrUtil.isNotBlank(gaModelField.columnName())) {
                return gaModelField.columnName();
            }
            return null;
        }
    }
}
