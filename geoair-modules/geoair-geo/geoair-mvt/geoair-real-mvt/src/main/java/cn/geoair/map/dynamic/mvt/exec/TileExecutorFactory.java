package cn.geoair.map.dynamic.mvt.exec;

import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;

/** 矢量瓦片执行器工厂类 封装版本判断逻辑，统一创建不同版本的 ITileExecutor 实例 */
public class TileExecutorFactory {

    // 私有化构造方法，禁止实例化
    private TileExecutorFactory() {}

    /**
     * 获取指定版本的矢量瓦片执行器实例
     *
     * @param executorVersion 版本号
     * @param layerName 图层名称
     * @return 对应版本的 ITileExecutor 实例
     */
    public static ITileExecutor getInstance(
            int executorVersion, TileRequestParams params, String layerName) {
        ITileExecutor executor;
        return VectorTileExecutorV2.getInstance(params, layerName);
    }
}
