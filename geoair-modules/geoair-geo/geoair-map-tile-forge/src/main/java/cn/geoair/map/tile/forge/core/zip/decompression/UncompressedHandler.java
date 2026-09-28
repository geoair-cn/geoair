package cn.geoair.map.tile.forge.core.zip.decompression;


import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;


import java.io.IOException;

/**
 * 未压缩数据解压适配器
 */

public class UncompressedHandler implements DecompressionHandler {

    public static GiLogger log = GirLoggerFactory.getLogger();

    @Override
    public byte[] decompress(byte[] compressedData, long expectedSize) throws IOException {
        DecompressionLimits.validateExpectedSize(expectedSize);
        if (compressedData.length != expectedSize) {
            throw new IOException("未压缩数据大小不匹配，预期:" + expectedSize + ", 实际:" + compressedData.length);
        }
        // 未压缩数据直接返回
        return compressedData;
    }
}
