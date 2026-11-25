package cn.geoair.spi.cache;

import cn.geoair.base.Gir;
import java.lang.ref.WeakReference;
import org.springframework.cache.CacheManager;

final class SpringCacheManagerProvider {

    private static WeakReference<CacheManager> weakReference = new WeakReference<>(null);

    private SpringCacheManagerProvider() {}

    static CacheManager getCacheManager() {
        CacheManager cacheManager = weakReference.get();
        if (cacheManager == null) {
            cacheManager = Gir.beans.getBean(CacheManager.class);
            weakReference = new WeakReference<>(cacheManager);
        }
        return cacheManager;
    }
}
