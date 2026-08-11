package cn.geoair.map.dynamic.adv.query.result;

import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanMappingMeta;
import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanMappingMeta.AdvBeanPropertyMeta;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.geoair.map.dynamic.tools.simple.collection.map.OptNullGeomAndBasicTypeFromObjectGetter;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.CaseInsensitiveLinkedMap;
import cn.hutool.db.Entity;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 一行查询结果数据。
 *
 * <p>基于 {@link LinkedHashMap}，通过接口继承链提供丰富的类型安全 getter：
 * {@code getStr / getInt / getLong / getGeometry / getGeoJsonStr / getWktString} 等。</p>
 *
 * @author zhangjun
 */
public class GirAdvOneRow extends LinkedHashMap<String, Object>
        implements OptNullGeomAndBasicTypeFromObjectGetter, Serializable {

    // ==================== 工厂方法 ====================

    public static GirAdvOneRow ofByMap(Map<String, Object> row) {
        return new GirAdvOneRow(row);
    }

    public static GirAdvOneRow ofByEntity(Entity row) {
        return row == null ? new GirAdvOneRow(Collections.emptyMap()) : new GirAdvOneRow(row);
    }

    public static List<GirAdvOneRow> ofByEntityList(List<Entity> rows) {
        if (rows == null || rows.isEmpty()) return Collections.emptyList();
        return rows.stream()
                .filter(Objects::nonNull)
                .map(GirAdvOneRow::ofByEntity)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toList());
    }

    // ==================== 构造函数 ====================

    private GirAdvOneRow(Map<String, Object> map) {
        super(map);
    }

    // ==================== Bean 转换（使用 TypeHandler 体系） ====================

    /**
     * 使用 TypeHandler 体系将当前行转换为指定类型的 Bean。
     *
     * <p>Geometry / 日期 / 枚举 等复杂类型会通过 {@link AdvTypeHandlerRegistry} 进行正确的类型转换，
     * 而非简单的 BeanUtil 反射映射。推荐在持有 Registry 时使用此方法。</p>
     *
     * @param clazz   目标 Bean 类型
     * @param registry TypeHandler 注册表（通过 {@code executor.getTypeHandlerRegistry()} 获取）
     * @param <T>     Bean 类型
     * @return 转换后的 Bean 实例
     */
    public <T> T toBeanObj(Class<T> clazz, AdvTypeHandlerRegistry registry) {
        T bean = newInstance(clazz);
        AdvBeanMappingMeta mappingMeta = AdvBeanMappingMeta.of(clazz);
        for (Map.Entry<String, Object> entry : this.entrySet()) {
            String key = entry.getKey();
            Object rawValue = entry.getValue();
            AdvBeanPropertyMeta propertyMeta = mappingMeta.resolvePropertyByColumnOrProperty(key);
            if (propertyMeta == null || propertyMeta.isIgnored()) continue;

            @SuppressWarnings("rawtypes")
            AdvTypeHandler fieldHandler = propertyMeta.getAdvTypeHandler();
            Object convertedValue;
            if (fieldHandler != null) {
                convertedValue = fieldHandler.convertForRead(
                        rawValue, propertyMeta.getPropertyType(),
                        AdvTypeHandlerContext.of(clazz, propertyMeta.getPropertyName(), key, propertyMeta.getPropertyType()));
            } else {
                convertedValue = registry.convertForRead(
                        rawValue, propertyMeta.getPropertyType(),
                        AdvTypeHandlerContext.of(clazz, propertyMeta.getPropertyName(), key, propertyMeta.getPropertyType()));
            }
            propertyMeta.writeValue(bean, convertedValue);
        }
        return bean;
    }

    /**
     * 将行列表批量转换为 Bean 列表（使用 TypeHandler 体系）。
     *
     * @param rowList  行列表
     * @param clazz    目标 Bean 类型
     * @param registry TypeHandler 注册表
     * @param <T>      Bean 类型
     * @return Bean 列表
     */
    public static <T> List<T> toBeanObjList(List<GirAdvOneRow> rowList, Class<T> clazz, AdvTypeHandlerRegistry registry) {
        if (rowList == null) return Collections.emptyList();
        return rowList.stream()
                .filter(Objects::nonNull)
                .map(row -> row.toBeanObj(clazz, registry))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static <T> T newInstance(Class<T> clazz) {
        try {
            if (Map.class.isAssignableFrom(clazz)) {
                return (T) new LinkedHashMap<String, Object>();
            }
            return clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建对象失败：" + clazz.getName(), e);
        }
    }

    /**
     * 将当前行简单转换为 Bean（不使用 TypeHandler，适合无 Geometry/日期等复杂类型的 Bean）。
     *
     * <p>如需正确的 Geometry 类型转换，请使用 {@link #toBeanObj(Class, AdvTypeHandlerRegistry)}。</p>
     */
    public <T> T toBeanObj(Class<T> clazz) {
        return cn.hutool.core.bean.BeanUtil.toBean(this, clazz);
    }

    /**
     * 将行列表批量简单转换为 Bean 列表。
     */
    public static <T> List<T> toBeanObjList(List<GirAdvOneRow> rowList, Class<T> clazz) {
        if (rowList == null) return Collections.emptyList();
        return rowList.stream()
                .filter(Objects::nonNull)
                .map(row -> row.toBeanObj(clazz))
                .collect(Collectors.toList());
    }

    /** 转换为大小写不敏感的 key */
    public GirAdvOneRow toCaseInsensitive() {
        return new GirAdvOneRow(new CaseInsensitiveLinkedMap<>(this));
    }

    /**
     * 转换为驼峰 key。
     *
     * <p>仅在外部套一层适配，不复制原始数据，key 的转换是惰性的。
     * 如果只需要读取少量字段，建议直接使用 {@code getStr / getInt} 等方法，
     * 它们本身已支持大小写不敏感匹配。</p>
     */
    public GirAdvOneRow toCamelCase() {
        // 驼峰映射不可逆，需要实际转换所有 key
        Map<String, Object> camelMap = new LinkedHashMap<>(this.size());
        this.forEach((key, value) -> {
            camelMap.put(toCamelCaseKey(key), value);
        });
        return new GirAdvOneRow(camelMap);
    }

    /** 转换 key 列表为驼峰 */
    public static List<GirAdvOneRow> toCamelCaseList(List<GirAdvOneRow> rowList) {
        if (rowList == null) return Collections.emptyList();
        return rowList.stream()
                .filter(Objects::nonNull)
                .map(GirAdvOneRow::toCamelCase)
                .collect(Collectors.toList());
    }

    /** 转换 key 列表为大小写不敏感 */
    public static List<GirAdvOneRow> toCaseInsensitiveList(List<GirAdvOneRow> rowList) {
        if (rowList == null) return Collections.emptyList();
        return rowList.stream()
                .filter(Objects::nonNull)
                .map(GirAdvOneRow::toCaseInsensitive)
                .collect(Collectors.toList());
    }

    /** 下划线转驼峰（snake_case → camelCase） */
    private static String toCamelCaseKey(String key) {
        if (key == null || key.isEmpty()) return key;
        StringBuilder sb = new StringBuilder(key.length());
        boolean nextUpper = false;
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '_') {
                nextUpper = true;
            } else {
                sb.append(nextUpper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                nextUpper = false;
            }
        }
        return sb.toString();
    }

    // ==================== Map 转换 ====================

    public static Map<String, Object> toMap(GirAdvOneRow oneRow) {
        return oneRow != null ? oneRow : new LinkedHashMap<>();
    }

    public static List<Map<String, Object>> toMapList(List<GirAdvOneRow> rowList) {
        if (rowList == null) return Collections.emptyList();
        return rowList.stream()
                .filter(Objects::nonNull)
                .map(r -> (Map<String, Object>) r)
                .collect(Collectors.toList());
    }

    // ==================== 接口方法 ====================

    @Override
    public Object getObj(String key, Object defaultValue) {
        Object o = this.get(key);
        return o != null ? o : defaultValue;
    }
}
