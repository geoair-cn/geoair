package cn.geoair.spi.util;

import cn.geoair.base.util.GutilAop;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

@Component
public class SpringAopProvider4Gir implements GutilAop.AopProvider {

    static {
        GutilAop.setAopProvider(new SpringAopProvider4Gir());
    }

    @Override
    public boolean isAopProxy(Object object) {
        return AopUtils.isAopProxy(object);
    }

    @Override
    public boolean isJdkDynamicProxy(Object object) {
        return AopUtils.isJdkDynamicProxy(object);
    }

    @Override
    public boolean isCglibProxy(Object object) {
        return AopUtils.isCglibProxy(object);
    }

    @Override
    public Class<?> getTargetClass(Object candidate) {
        return AopUtils.getTargetClass(candidate);
    }
}
