package cn.geoair.spi.bean;

import java.lang.ref.WeakReference;

import cn.geoair.base.bean.GiBeanFactory;

final class SpringBeanProviderResolver {

    private static WeakReference<GiBeanFactory> weakReference = new WeakReference<>(null);

    private SpringBeanProviderResolver() {}

    static GiBeanFactory getProvider() {
        GiBeanFactory provider = weakReference.get();
        if (provider == null) {
            provider = SpringContextBean4Gir.getCurrentProvider();
            if (provider != null) {
                weakReference = new WeakReference<>(provider);
            }
        }
        return provider;
    }

    static void setProvider(GiBeanFactory provider) {
        weakReference = new WeakReference<>(provider);
    }
}
