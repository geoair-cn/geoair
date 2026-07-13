package cn.geoair.map.tile.forge.core.zip.decompression;

import java.io.IOException;

/**
 * 压缩数据解压处理器接口
 * 每种压缩类型对应一个实现类
 */
public interface DecompressionHandler {

    /**
     * 解压数据
     * @param compressedData 压缩数据
     * @param expectedSize 预期解压后大小（用于校验）
     * @return 解压后的原始数据
     * @throws IOException 解压失败时抛出
     */
    byte[] decompress(byte[] compressedData, long expectedSize) throws IOException;

    /**
     * 判断当前解压器是否支持分块解压
     *
     * @return true-支持分块解压（可以逐块处理压缩数据流），
     *         false-不支持分块解压（需要一次性传入完整的压缩数据）
     */
    default boolean supportStreamingDecompress() {
        return false;  // 默认不支持，需要子类主动覆盖
    }
}
