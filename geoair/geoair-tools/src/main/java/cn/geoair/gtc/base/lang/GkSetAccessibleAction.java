package cn.geoair.gtc.base.lang;

import java.lang.reflect.AccessibleObject;
import java.security.PrivilegedAction;

import cn.geoair.gtc.base.util.GutilReflection;

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
