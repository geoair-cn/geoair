package cn.geoair.map.tile.forge.core.zip.decompression;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/** BZIP2压缩解压适配器（ZIP规范 method=12） */
public class Bzip2Handler implements DecompressionHandler {

    private static final int BUFFER_SIZE = 8192;

    @Override
    public byte[] decompress(byte[] compressedData, long expectedSize) throws IOException {
        DecompressionLimits.validateExpectedSize(expectedSize);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
                BZip2CompressorInputStream bzis = new BZip2CompressorInputStream(bais)) {

            byte[] result = DecompressionLimits.readAllLimited(bzis);
            if (expectedSize > 0 && result.length != expectedSize) {
                throw new IOException("BZIP2解压大小不匹配，预期:" + expectedSize + ", 实际:" + result.length);
            }
            return result;
        }
    }

    @Override
    public boolean supportStreamingDecompress() {
        return false;
    }
}
