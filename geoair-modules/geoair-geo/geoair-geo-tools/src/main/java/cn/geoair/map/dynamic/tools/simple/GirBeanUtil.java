package cn.geoair.map.dynamic.tools.simple;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;

/**
 * @author ：张逢吉
 * @date ：Created in 2024/10/28 18:31 @description： TODO
 */
public class GirBeanUtil extends BeanUtil {

    public static <T> T getFieldValue(Object bean, String fieldNameOrIndex, Class<T> type) {
        Object fieldValue = getFieldValue(bean, fieldNameOrIndex);
        return Convert.convert(type, fieldValue);
    }

    public static String getFieldValueString(Object bean, String fieldNameOrIndex) {
        Object fieldValue = getFieldValue(bean, fieldNameOrIndex);
        return Convert.convert(String.class, fieldValue);
    }
}
