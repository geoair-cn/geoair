package cn.geoair.map.tile.forge.core.zip.decompression;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/** TAR.BZIP2解压适配器 */
public class TarBzip2Handler implements DecompressionHandler {

    private static final int BUFFER_SIZE = 8192;

    @Override
    public byte[] decompress(byte[] compressedData, long expectedSize) throws IOException {
        DecompressionLimits.validateExpectedSize(expectedSize);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
                BZip2CompressorInputStream bzis = new BZip2CompressorInputStream(bais);
                TarArchiveInputStream tis = new TarArchiveInputStream(bzis)) {

            TarArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    byte[] result = DecompressionLimits.readAllLimited(tis);
                    if (expectedSize > 0 && result.length != expectedSize) {
                        throw new IOException(
                                "TAR.BZIP2解压大小不匹配，预期:" + expectedSize + ", 实际:" + result.length);
                    }
                    return result;
                }
            }
            return new byte[0];
        }
    }

    @Override
    public boolean supportStreamingDecompress() {
        return false;
    }
}
