package cn.geoair.map.tile.forge.fuser.fuser;

import cn.geoair.map.tile.forge.fuser.cache.TileCache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 带缓存的GtcTileFuser装饰器
 * 对融合后的图像结果进行缓存，避免重复计算相同参数的融合请求
 *
 * @author ：张俊
 * @date ：Created in 2026/6/15 12:37
 * @description：带缓存的GtcTileFuser
 */
@Slf4j
public class CacheTileFuserExec implements FuserExec {

    private final FuserExec target;
    @Getter
    private final TileCache tileCache;
    private final String layerName;
    private final Integer z;
    private final Integer x;
    private final Integer y;

    /**
     * 构造函数
     *
     * @param target    GtcTileFuser实例
     * @param tileCache 缓存实现
     * @param layerName 图层名称
     * @param z         zoom等级
     * @param x         x坐标
     * @param y         y坐标
     */
    public CacheTileFuserExec(FuserExec target, TileCache tileCache,
                              String layerName, Integer z, Integer x, Integer y) {
        this.target = target;
        this.tileCache = tileCache;
        this.layerName = layerName;
        this.z = z;
        this.x = x;
        this.y = y;
        log.debug("CacheTileFuser初始化完成 - layer: {}, z: {}, x: {}, y: {}", layerName, z, x, y);
    }

    /**
     * 获取融合后的图像字节数组（带缓存）
     */
    @Override
    public byte[] toImageBytes() throws Exception {
        long startTime = System.currentTimeMillis();

        // 尝试从缓存获取
        try {
            byte[] cachedResult = tileCache.get(layerName, z, x, y);
            if (cachedResult != null && cachedResult.length > 0) {
                long cacheHitTime = System.currentTimeMillis() - startTime;
                log.debug("缓存命中 - layer: {}, z: {}, x: {}, y: {}, 耗时: {} ms, 数据大小: {} bytes",
                        layerName, z, x, y, cacheHitTime, cachedResult.length);
                return cachedResult;
            }
            log.debug("缓存未命中 - layer: {}, z: {}, x: {}, y: {}", layerName, z, x, y);
        } catch (Exception e) {
            log.warn("从缓存读取失败: {}, 将重新生成 - layer: {}, z: {}, x: {}, y: {}",
                    e.getMessage(), layerName, z, x, y, e);
        }

        // 缓存未命中，执行实际融合操作
        long fusionStartTime = System.currentTimeMillis();
        byte[] result = target.toImageBytes();
        long fusionTime = System.currentTimeMillis() - fusionStartTime;

        // 将结果存入缓存
        if (result != null && result.length > 0) {
            try {
                tileCache.put(layerName, z, x, y, result);
                long totalTime = System.currentTimeMillis() - startTime;
                log.debug("融合完成并已缓存 - layer: {}, z: {}, x: {}, y: {}, 融合耗时: {} ms, 总耗时: {} ms, 数据大小: {} bytes",
                        layerName, z, x, y, fusionTime, totalTime, result.length);
            } catch (Exception e) {
                log.warn("写入缓存失败: {} - layer: {}, z: {}, x: {}, y: {}",
                        e.getMessage(), layerName, z, x, y, e);
            }
        } else {
            log.warn("融合结果为空，跳过缓存 - layer: {}, z: {}, x: {}, y: {}", layerName, z, x, y);
        }

        return result;
    }

    /**
     * 获取底层的GtcTileFuser实例
     */
    public FuserExec getTarget() {
        return target;
    }


}
