package cn.geoair.map.tile.forge.core.zip.decompression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

/** TAR.GZ解压适配器 */
public class TarGzHandler implements DecompressionHandler {

    private static final int BUFFER_SIZE = 8192;

    @Override
    public byte[] decompress(byte[] compressedData, long expectedSize) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
                GzipCompressorInputStream gzis = new GzipCompressorInputStream(bais);
                TarArchiveInputStream tis = new TarArchiveInputStream(gzis);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            TarArchiveEntry entry;
            while ((entry = tis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    // 读取tar包中的第一个文件（可根据业务调整）
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int len;
                    while ((len = tis.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    break; // 只处理第一个文件
                }
            }

            byte[] result = baos.toByteArray();
            // 校验大小（可选）
            if (expectedSize > 0 && result.length != expectedSize) {
                throw new IOException("TAR.GZ解压大小不匹配，预期:" + expectedSize + ", 实际:" + result.length);
            }
            return result;
        }
    }
}
