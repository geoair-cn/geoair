package cn.geoair.map.tile.forge.core.caches;

import java.util.concurrent.Callable;

public class NoOpCacheProvider implements CacheProvider {

    @Override
    public String getName() {
        return "";
    }

    @Override
    public void put(Object key, Object value) {}

    @Override
    public void put(Object key, Object value, long milliseconds) {}

    @Override
    public Object getObject(Object key) {
        return null;
    }

    @Override
    public boolean exists(Object key) {
        return false;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return null;
    }

    @Override
    public String getString(Object key) {
        return "";
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return null;
    }

    @Override
    public byte[] getByte(Object key) {
        return new byte[0];
    }

    @Override
    public long pttl(Object key) {
        return 0;
    }

    @Override
    public void evict(Object key) {}

    @Override
    public void evictByPreFix(Object prefix) {}

    @Override
    public void clear() {}
}
