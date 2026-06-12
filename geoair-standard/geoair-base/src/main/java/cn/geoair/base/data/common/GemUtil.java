package cn.geoair.base.data.common;

import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.support.GirVisualValueKid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GemUtil {

    /**
     * 提取枚举类的所有值和显示名称
     *
     * @param clz 枚举类class对象
     * @return 包含valueField和displayField的Map列表
     */
    public static List<?> extractEnum(Class<?> clz) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> p;
        for (GiVisualValuable<?> obj : (GiVisualValuable[]) clz.getEnumConstants()) {
            p = new HashMap<>();
            p.put("valueField", obj.value());
            p.put("displayField", obj.display());
            list.add(p);
        }
        return list;
    }

    /**
     * 枚举转换成GirVisualValueKid模型
     *
     * @param <T> 枚举类型
     * @param <K> 枚举值类型
     * @param enumClz 枚举类class对象
     * @return GirVisualValueKid列表
     */
    public static <T extends Enum<T> & GiVisualValuable<K>, K>
            List<GirVisualValueKid<K>> extractEnumVisualValueKid(Class<T> enumClz) {
        List<GirVisualValueKid<K>> list = new ArrayList<>();
        for (GiVisualValuable<K> obj : (GiVisualValuable<K>[]) enumClz.getEnumConstants()) {
            list.add(new GirVisualValueKid<K>(obj.value(), obj.display()));
        }
        return list;
    }

    /**
     * 提取枚举类的所有值
     *
     * @param clz 枚举类class对象
     * @return 值列表
     */
    public static List<String> extractEnumValues(Class<?> clz) {
        List<String> list = new ArrayList<>();
        for (GiVisualValuable<?> obj : (GiVisualValuable[]) clz.getEnumConstants()) {
            list.add((String) obj.value());
        }
        return list;
    }

    /**
     * 根据显示名称获取枚举值
     *
     * @param <T> 返回值类型
     * @param clz 枚举类class对象
     * @param key 显示名称
     * @param tClass 返回值类型的class对象
     * @return 枚举值
     */
    public static <T> T[] getEnumValue(Class<?> clz, Object key, Class<T> tClass) {
        for (GiVisualValuable<?> obj : (GiVisualValuable[]) clz.getEnumConstants()) {
            if (key.equals(obj.display())) {
                try {
                    return (T[]) obj.value();
                } catch (Exception ignore) {
                }
            }
        }
        return null;
    }

    /**
     * 根据枚举值获取显示名称
     *
     * @param clz 枚举类class对象
     * @param key 枚举值
     * @return 显示名称
     */
    public static String getEnumDisplay(Class<?> clz, Object key) {
        for (GiVisualValuable<?> obj : (GiVisualValuable[]) clz.getEnumConstants()) {
            if (obj.value() instanceof Integer) {
                if (key instanceof String) {
                    try {
                        if (obj.value().equals(Integer.valueOf((String) key))) {
                            return obj.display();
                        }
                    } catch (Exception ignore) {
                    }
                }
                if (key instanceof Integer) {
                    if (obj.value().equals(key)) {
                        return obj.display();
                    }
                }
            } else {
                if (obj.value().equals(key)) {
                    return obj.display();
                }
            }
        }
        return "";
    }

    /**
     * 根据枚举值获取枚举对象
     *
     * @param clz 枚举类class对象
     * @param key 枚举值
     * @return 枚举对象
     */
    public static GiVisualValuable<?> getEnumClass(Class<?> clz, Object key) {
        for (GiVisualValuable<?> obj : (GiVisualValuable[]) clz.getEnumConstants()) {
            if (obj.value() instanceof Integer) {
                if (key instanceof String) {
                    try {
                        if (obj.value().equals(Integer.valueOf((String) key))) {
                            return obj;
                        }
                    } catch (Exception ignore) {

                    }
                }
                if (key instanceof Integer) {
                    if (obj.value().equals(key)) {
                        return obj;
                    }
                }
            } else {
                if (obj.value().equals(key)) {
                    return obj;
                }
            }
        }
        return null;
    }

    /**
     * 根据枚举值获取枚举对象
     *
     * @param <T> 枚举类型
     * @param <K> 枚举值类型
     * @param enumType 枚举类class对象
     * @param value 枚举值
     * @param defalutE 默认枚举对象
     * @return 枚举对象
     */
    public static <T extends Enum<T> & GiVisualValuable<K>, K> T valueOf(
            Class<T> enumType, K value, T defalutE) {
        T[] ts = enumType.getEnumConstants();
        for (T t : ts) {
            if (Objects.equals(t.value(), value)) {
                return t;
            }
        }
        return defalutE;
    }
}
