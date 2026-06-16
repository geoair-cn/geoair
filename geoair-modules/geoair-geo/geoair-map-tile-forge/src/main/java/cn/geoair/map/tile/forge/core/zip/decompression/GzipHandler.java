package cn.geoair.map.tile.forge.core.zip.decompression;


import cn.hutool.core.util.ZipUtil;
import java.io.IOException;

/**
 * GZIP解压适配器
 */
public class GzipHandler implements DecompressionHandler {



    @Override
    public byte[] decompress(byte[] compressedData, long expectedSize) throws IOException {
        // 使用hutool工具类处理GZIP解压
        byte[] uncompressed = ZipUtil.unGzip(compressedData);
        // 校验大小（可选）
        if (expectedSize > 0 && uncompressed.length != expectedSize) {
            throw new IOException("GZIP解压大小不匹配，预期:" + expectedSize + ", 实际:" + uncompressed.length);
        }
        return uncompressed;
    }
}
