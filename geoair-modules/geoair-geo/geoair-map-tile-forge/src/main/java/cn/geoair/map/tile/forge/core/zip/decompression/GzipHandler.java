package cn.geoair.map.tile.forge.core.zip.decompression;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * GZIP解压适配器
 */
public class GzipHandler implements DecompressionHandler {



    @Override
    public byte[] decompress(byte[] compressedData, long expectedSize) throws IOException {
        DecompressionLimits.validateExpectedSize(expectedSize);
        byte[] uncompressed;
        try (GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(compressedData))) {
            uncompressed = DecompressionLimits.readAllLimited(inputStream);
        }
        // 校验大小（可选）
        if (expectedSize > 0 && uncompressed.length != expectedSize) {
            throw new IOException("GZIP解压大小不匹配，预期:" + expectedSize + ", 实际:" + uncompressed.length);
        }
        return uncompressed;
    }
}
