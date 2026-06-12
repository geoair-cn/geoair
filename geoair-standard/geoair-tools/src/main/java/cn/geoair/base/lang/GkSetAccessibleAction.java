package cn.geoair.base.lang;

import cn.geoair.base.util.GutilReflection;

import java.lang.reflect.AccessibleObject;
import java.security.PrivilegedAction;

public class GkSetAccessibleAction<T extends AccessibleObject> implements PrivilegedAction<T> {

    private final T obj;

    public GkSetAccessibleAction(T obj) {
        this.obj = obj;
    }

    @Override
    public T run() {
        GutilReflection.setAccessible(obj);
        return obj;
    }
}
