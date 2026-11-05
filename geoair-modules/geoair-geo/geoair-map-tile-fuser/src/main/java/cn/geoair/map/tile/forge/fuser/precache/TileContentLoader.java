package cn.geoair.map.tile.forge.fuser.precache;

import cn.geoair.map.tile.forge.fuser.execution.TileOperationContext;

/** 下载或融合一张瓦片；缺失返回 null，失败抛异常，不得用透明图片冒充成功。 */
@FunctionalInterface
public interface TileContentLoader {
    byte[] load(int zoom, int x, int y, TileOperationContext context) throws Exception;
}
