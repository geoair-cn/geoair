package cn.geoair.map.dynamic.tools.simple.collection.map;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Pair;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2026/7/16 12:42
 * @description： 集成快速创建方法的LinkedHashMap<String, Object>
 */
public class GirFastStrObjMap<V> extends LinkedHashMap<String, V>
        implements OptNullGeomAndBasicTypeFromObjectGetter, Serializable {
    /** 创建一个空的 GirFastStrObjMap */
    public static <V> GirFastStrObjMap<V> of() {
        return new GirFastStrObjMap<>();
    }

    /**
     * 通过bean创建 GirFastStrObjMap
     *
     * @param bean bean对象
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue 是否忽略值为空的字段
     */
    public static GirFastStrObjMap<Object> ofBean(
            Object bean, boolean isToUnderlineCase, boolean ignoreNullValue) {
        GirFastStrObjMap<Object> map = new GirFastStrObjMap<>();
        BeanUtil.beanToMap(bean, map, isToUnderlineCase, ignoreNullValue);
        return map;
    }

    public GirFastStrObjMap<V> ofMap(Map<String, V> all) {
        super.putAll(all);
        return this;
    }

    public GirFastStrObjMap<V> addOne(String key, V value) {
        super.put(key, value);
        return this;
    }

    public GirFastStrObjMap<V> addPair(Pair<String, V> pair) {
        super.put(pair.getKey(), pair.getValue());
        return this;
    }

    @Override
    public Object getObj(String key, Object defaultValue) {
        Object o = super.get(key);
        if (o == null) {
            return defaultValue;
        }
        return o;
    }
}
