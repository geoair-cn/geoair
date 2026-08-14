package cn.geoair.map.dynamic.adv.query.result;

import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanMapMapper;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.simple.collection.map.OptNullGeomAndBasicTypeFromObjectGetter;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.CaseInsensitiveLinkedMap;
import cn.hutool.db.Entity;
import com.alibaba.fastjson2.JSONObject;
import org.locationtech.jts.geom.Geometry;

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

    /**
     * 关联的 TypeHandler 注册表，用于 {@link #toBeanObj(Class)} 做正确的类型转换
     */
    private transient AdvTypeHandlerRegistry typeHandlerRegistry;

    // ==================== 工厂方法 ====================

    public static GirAdvOneRow ofByMap(Map<String, Object> row) {
        GirAdvOneRow result = new GirAdvOneRow(row);
        if (row instanceof GirAdvOneRow) {
            result.typeHandlerRegistry = ((GirAdvOneRow) row).typeHandlerRegistry;
        }
        return result;
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

    /**
     * 设置 TypeHandler 注册表（由 bSelectList / bSelectOne 等方法自动注入）
     */
    public void setTypeHandlerRegistry(AdvTypeHandlerRegistry registry) {
        this.typeHandlerRegistry = registry;
    }

    /**
     * fluent 风格的 Registry 注入
     */
    public GirAdvOneRow withRegistry(AdvTypeHandlerRegistry registry) {
        this.typeHandlerRegistry = registry;
        return this;
    }

    // ==================== Bean 转换 ====================

    /**
     * 将当前行转换为 Bean（委托给 {@link AdvBeanMapMapper}）。
     *
     * <p>如果已注入 {@link AdvTypeHandlerRegistry}（通过 {@code bSelectList} 等方法自动注入），
     * 则走 TypeHandler 转换链路；否则回退到 Hutool BeanUtil。</p>
     */
    public <T> T toBeanObj(Class<T> clazz) {
        if (typeHandlerRegistry != null) {
            return new AdvBeanMapMapper(typeHandlerRegistry).mapToBean(this, clazz);
        }
        return cn.hutool.core.bean.BeanUtil.toBean(this, clazz);
    }

    /**
     * 使用指定 Registry 转换为 Bean。
     */
    public <T> T toBeanObj(Class<T> clazz, AdvTypeHandlerRegistry registry) {
        return new AdvBeanMapMapper(registry).mapToBean(this, clazz);
    }

    /**
     * 批量转换（自动检测各行已注入的 Registry）
     */
    public static <T> List<T> toBeanObjList(List<GirAdvOneRow> rowList, Class<T> clazz) {
        if (rowList == null) return Collections.emptyList();
        return rowList.stream()
                .filter(Objects::nonNull)
                .map(row -> row.toBeanObj(clazz))
                .collect(Collectors.toList());
    }

    /**
     * 批量转换（使用统一 Registry）
     */
    public static <T> List<T> toBeanObjList(List<GirAdvOneRow> rowList, Class<T> clazz, AdvTypeHandlerRegistry registry) {
        if (rowList == null) return Collections.emptyList();
        AdvBeanMapMapper mapper = new AdvBeanMapMapper(registry);
        return rowList.stream()
                .filter(Objects::nonNull)
                .map(row -> mapper.mapToBean(row, clazz))
                .collect(Collectors.toList());
    }

    /**
     * 转换为大小写不敏感的 key
     */
    public GirAdvOneRow toCaseInsensitive() {
        GirAdvOneRow result = new GirAdvOneRow(new CaseInsensitiveLinkedMap<>(this));
        result.typeHandlerRegistry = this.typeHandlerRegistry;
        return result;
    }

    /**
     * 转换为驼峰 key。
     */
    public GirAdvOneRow toCamelCase() {
        Map<String, Object> camelMap = new LinkedHashMap<>(this.size());
        this.forEach((key, value) -> camelMap.put(toCamelCaseKey(key), value));
        GirAdvOneRow result = new GirAdvOneRow(camelMap);
        result.typeHandlerRegistry = this.typeHandlerRegistry;
        return result;
    }

    /**
     * 转换 key 列表为驼峰
     */
    public static List<GirAdvOneRow> toCamelCaseList(List<GirAdvOneRow> rowList) {
        if (rowList == null) return Collections.emptyList();
        return rowList.stream()
                .filter(Objects::nonNull)
                .map(GirAdvOneRow::toCamelCase)
                .collect(Collectors.toList());
    }

    /**
     * 转换 key 列表为大小写不敏感
     */
    public static List<GirAdvOneRow> toCaseInsensitiveList(List<GirAdvOneRow> rowList) {
        if (rowList == null) return Collections.emptyList();
        return rowList.stream()
                .filter(Objects::nonNull)
                .map(GirAdvOneRow::toCaseInsensitive)
                .collect(Collectors.toList());
    }

    /**
     * 下划线转驼峰（snake_case → camelCase）
     */
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

    /**
     * 获取指定 key 对应的 JTS Geometry 对象。
     * <p>
     * 当 {@link #typeHandlerRegistry} 已注入时，优先使用 TypeHandler 链进行方言感知的转换
     * （如 PostGIS PGobject、MySQL binary、Oracle SDO_GEOMETRY），性能更优；
     * 对于 String 类型的值，保留原有的 GeoJSON → WKT → WKB 解析顺序。
     * <p>
     * 当 registry 未注入时（如反序列化后的实例），回退到父类默认的试错式解析。
     */
    @Override
    public Geometry getGeometry(String key) {
        Object value = getObj(key);
        if (value == null) {
            return null;
        }

        // registry 未注入时，回退到父类的全量试错实现
        if (typeHandlerRegistry == null) {
            return OptNullGeomAndBasicTypeFromObjectGetter.super.getGeometry(key);
        }

        // String 类型：保留 GeoJSON → WKT → WKB 的解析顺序
        if (value instanceof String) {
            return parseStringGeometry((String) value);
        }
        if (value instanceof Map) { // 判断是否为json对象
            JSONObject jsonObject = new JSONObject((Map<String, Object>) value);
            return
                    GirGeoTools.defaultInstance().getFormatOpt()
                            .geojsonToJtsGeometry(jsonObject.toJSONString(), true);
        }
        // 非 String 类型（PGobject、byte[]、SDO_GEOMETRY 等）：交给 registry 的方言 handler
        return (Geometry) typeHandlerRegistry.convertForRead(
                value, Geometry.class, AdvTypeHandlerContext.simple(key));
    }

    /**
     * 将字符串解析为 Geometry，按 GeoJSON → WKT → WKB 顺序尝试。
     */
    private Geometry parseStringGeometry(String str) {
        // 1. 尝试 GeoJSON
        try {
            JSONObject json = JSONObject.parseObject(str);
            if (json != null) {
                return GirGeoTools.defaultInstance().getFormatOpt()
                        .geojsonToJtsGeometry(str, true);
            }
        } catch (Exception ignored) {
        }
        // 2. 尝试 WKT
        try {
            return GirGeoTools.defaultInstance().getFormatOpt()
                    .wktToJtsGeometry(str, true);
        } catch (Exception ignored) {
        }
        // 3. 尝试 WKB
        try {
            return GirGeoTools.defaultInstance().getFormatOpt()
                    .wkbToJtsGeometry(str, true);
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public Object getObj(String key, Object defaultValue) {
        Object o = this.get(key);
        return o != null ? o : defaultValue;
    }
}
