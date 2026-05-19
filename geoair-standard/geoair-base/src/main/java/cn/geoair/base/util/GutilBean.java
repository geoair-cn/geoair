package cn.geoair.base.util;

import cn.geoair.base.bean.GirBeanException;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GkMethodHand;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Locale;

public class GutilBean {

    public static void copyProperties(Object source, Object target) throws GirBeanException {
        copyProperties(source, target, null, (String[]) null);
    }

    public static void copyProperties(Object source, Object target, Class<?> editable)
            throws GirBeanException {
        copyProperties(source, target, editable, (String[]) null);
    }

    public static void copyProperties(Object source, Object target, String... ignoreProperties)
            throws GirBeanException {
        copyProperties(source, target, null, ignoreProperties);
    }

    @GaMethodHandDefine(
            id = "cn.geoair.base.util.GutilBean.copyProperties",
            expectClassName = "org.springframework.beans.BeanUtils",
            expectMethodName = "copyProperties")
    public static void copyProperties(
            Object source, Object target, Class<?> editable, String... ignoreProperties)
            throws GirBeanException {
        GkMethodHand.invokeSelf(source, target, editable, ignoreProperties);
    }

    /**
     * copy from spring isSimpleProperty
     *
     * @param type
     * @return
     */
    public static boolean isSimpleProperty(Class<?> type) {
        GutilAssert.notNull(type, "'type' must not be null");
        return isSimpleValueType(type)
                || (type.isArray() && isSimpleValueType(type.getComponentType()));
    }

    /** copy from spring isSimpleValueType */
    public static boolean isSimpleValueType(Class<?> type) {
        return (Void.class != type
                && void.class != type
                && (GutilClass.isPrimitiveOrWrapper(type)
                        || Enum.class.isAssignableFrom(type)
                        || CharSequence.class.isAssignableFrom(type)
                        || Number.class.isAssignableFrom(type)
                        || Date.class.isAssignableFrom(type)
                        || Temporal.class.isAssignableFrom(type)
                        || URI.class == type
                        || URL.class == type
                        || Locale.class == type
                        || Class.class == type));
    }
}
