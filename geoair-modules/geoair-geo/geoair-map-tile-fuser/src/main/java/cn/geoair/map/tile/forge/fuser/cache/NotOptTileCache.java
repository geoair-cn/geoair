package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.map.tile.forge.core.bygwc.core.mime.ImageMime;

/**
 * @author ：张俊
 * @date ：Created in 2026/6/16 14:40
 * @description： 什么都不干的缓存实现
 */
public class NotOptTileCache implements TileCache {
    @Override
    public byte[] get(String layerName, int z, int x, int y, ImageMime format) {
        return new byte[0];
    }

    @Override
    public boolean put(String layerName, int z, int x, int y, byte[] data, ImageMime format) {
        return true;
    }

    @Override
    public boolean deleteLayerCache(String layerName) {
        return true;
    }

    @Override
    public boolean delete(String layerName, Integer z, Integer x) {
        return true;
    }

    @Override
    public boolean delete(String layerName, int z, int x, int y, ImageMime format) {
        return true;
    }

    @Override
    public void clearAll() {

    }

    @Override
    public long getTotalSize() {
        return 0;
    }

    @Override
    public boolean exists(String layerName, int z, int x, int y, ImageMime format) {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
