package cn.geoair.map.tile.forge.fuser.cache;

import cn.geoair.web.mime.GiMimeType;

/**
 * 瓦片缓存接口
 *
 * @author 张俊
 * @date Created in 2023/12/4
 * @description 定义瓦片缓存的标准操作
 */
public interface TileCache {

    /**
     * 从缓存获取瓦片
     *
     * @param layerName 图层名称
     * @param z 层级
     * @param x X坐标
     * @param y Y坐标
     * @param format
     * @return 瓦片字节数组，不存在则返回null
     */
    byte[] get(String layerName, int z, int x, int y, GiMimeType format);

    /**
     * 保存瓦片到缓存
     *
     * @param layerName 图层名称
     * @param z 层级
     * @param x X坐标
     * @param y Y坐标
     * @param data 瓦片数据
     * @param format
     * @return 是否保存成功
     */
    boolean put(String layerName, int z, int x, int y, byte[] data, GiMimeType format);

    /**
     * 删除指定图层的所有缓存
     *
     * @param layerName 图层名称
     * @return 是否删除成功
     */
    boolean deleteLayerCache(String layerName);

    /**
     * 删除指定瓦片的缓存
     *
     * @param layerName 图层名称
     * @param z 层级
     * @param x X坐标
     * @return 是否删除成功
     */
    boolean delete(String layerName, Integer z, Integer x);

    /**
     * 删除指定瓦片的缓存
     *
     * @param layerName 图层名称
     * @param z 层级
     * @param x X坐标
     * @param y Y坐标
     * @param format
     * @return 是否删除成功
     */
    boolean delete(String layerName, int z, int x, int y, GiMimeType format);

    /** 清空所有缓存 */
    void clearAll();

    /**
     * 获取缓存总大小（字节）
     *
     * @return 缓存大小
     */
    long getTotalSize();

    /**
     * 检查缓存是否存在
     *
     * @param layerName 图层名称
     * @param z 层级
     * @param x X坐标
     * @param y Y坐标
     * @param format
     * @return 是否存在
     */
    boolean exists(String layerName, int z, int x, int y, GiMimeType format);

    /**
     * 缓存是否启用
     *
     * @return 是否启用
     */
    boolean isEnabled();
}
