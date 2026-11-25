package cn.geoair.map.tile.forge.fuser.image;

import java.io.IOException;

/** 可插拔图像处理规则，无数据库、线程池或网络依赖，不默认启用任何具体算法。 */
public interface TileImageProcessor {
    String getRuleVersion();

    TileImageProcessingResult process(byte[] source, TileImageProcessingContext context)
            throws IOException, InterruptedException;
}
