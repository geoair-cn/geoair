package cn.geoair.map.tile.forge.fuser.support;

import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingContext;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessingResult;
import cn.geoair.map.tile.forge.fuser.image.TileImageProcessor;
import java.util.concurrent.atomic.AtomicInteger;

/** 让缓存测试只验证读写行为，不把图像算法误差混进存储契约。 */
public final class ConstantTileImageProcessor implements TileImageProcessor {
    private final TileImageProcessingResult result;
    public final AtomicInteger calls = new AtomicInteger();

    public ConstantTileImageProcessor(TileImageProcessingResult result) {
        this.result = result;
    }

    @Override
    public String getRuleVersion() {
        return "test-v1";
    }

    @Override
    public TileImageProcessingResult process(byte[] bytes, TileImageProcessingContext context) {
        calls.incrementAndGet();
        return result;
    }
}
