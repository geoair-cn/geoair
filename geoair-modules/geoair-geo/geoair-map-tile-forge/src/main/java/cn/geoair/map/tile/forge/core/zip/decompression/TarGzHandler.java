package cn.geoair.map.tile.forge.core.zip.decompression;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/** TAR.GZ解压适配器 */
public class TarGzHandler implements DecompressionHandler {

    private static final int BUFFER_SIZE = 8192;

    @Override
    public byte[] decompress(byte[] compressedData, long expectedSize) throws IOException {
        DecompressionLimits.validateExpectedSize(expectedSize);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
                GzipCompressorInputStream gzis = new GzipCompressorInputStream(bais);
                TarArchiveInputStream tis = new TarArchiveInputStream(gzis)) {

            TarArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    // 读取tar包中的第一个文件（可根据业务调整）
                    byte[] result = DecompressionLimits.readAllLimited(tis);
                    if (expectedSize > 0 && result.length != expectedSize) {
                        throw new IOException(
                                "TAR.GZ解压大小不匹配，预期:" + expectedSize + ", 实际:" + result.length);
                    }
                    return result;
                }
            }
            return new byte[0];
        }
    }
}
