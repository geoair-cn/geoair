package cn.geoair.map.dynamic.adv.query.apo;

import cn.geoair.map.dynamic.adv.query.result.OptNullGeomAndBasicTypeFromObjectGetter;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Pair;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author ：张逢吉
 *     <p>使用这个参数的时候，表示前面传的sql是 select * from name == #{name} 这样的mybatis标签占位符
 */
public class SqlParamMap extends LinkedHashMap<String, Object>
        implements OptNullGeomAndBasicTypeFromObjectGetter, Serializable, GirSqlParam {

    /**
     * 创建一个空的 SqlParamMap
     *
     * @return
     */
    public static SqlParamMap of() {
        return new SqlParamMap();
    }

    /**
     * 通过bean创建 SqlParamMap
     *
     * @param bean bean对象
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue 是否忽略值为空的字段
     * @return
     */
    public static SqlParamMap ofBean(
            Object bean, boolean isToUnderlineCase, boolean ignoreNullValue) {
        SqlParamMap sqlParamMap = new SqlParamMap();
        BeanUtil.beanToMap(bean, sqlParamMap, isToUnderlineCase, ignoreNullValue);
        return sqlParamMap;
    }

    @Deprecated
    public SqlParamMap addAll(Map<String, Object> all) {
        super.putAll(all);
        return this;
    }

    public SqlParamMap ofMap(Map<String, Object> all) {
        super.putAll(all);
        return this;
    }

    public SqlParamMap addOne(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public SqlParamMap addPair(Pair<String, Object> pair) {
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
