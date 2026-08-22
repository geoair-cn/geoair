package cn.geoair.map.tile.forge.core.zip.decompression;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 压缩条目的资源限制，避免异常归档文件耗尽 JVM 内存。
 */
public final class DecompressionLimits {

    /** 单个压缩条目的最大压缩大小，默认 128 MiB。 */
    private static volatile long maxCompressedEntrySize = 128L * 1024L * 1024L;

    /** 单个解压条目的最大大小，默认 256 MiB。 */
    private static volatile long maxDecompressedEntrySize = 256L * 1024L * 1024L;

    private DecompressionLimits() {
    }

    public static void validateCompressedSize(long size) throws IOException {
        if (size < 0 || size > maxCompressedEntrySize) {
            throw new IOException("压缩条目大小超出限制: " + size);
        }
    }

    public static void validateExpectedSize(long size) throws IOException {
        if (size < 0 || size > maxDecompressedEntrySize) {
            throw new IOException("解压条目大小超出限制: " + size);
        }
    }

    public static long getMaxCompressedEntrySize() {
        return maxCompressedEntrySize;
    }

    /**
     * 设置单个压缩条目的最大压缩大小，单位为字节。
     */
    public static void setMaxCompressedEntrySize(long maxCompressedEntrySize) {
        validateLimit(maxCompressedEntrySize, "最大压缩条目大小");
        DecompressionLimits.maxCompressedEntrySize = maxCompressedEntrySize;
    }

    public static long getMaxDecompressedEntrySize() {
        return maxDecompressedEntrySize;
    }

    /**
     * 设置单个解压条目的最大大小，单位为字节。
     */
    public static void setMaxDecompressedEntrySize(long maxDecompressedEntrySize) {
        validateLimit(maxDecompressedEntrySize, "最大解压条目大小");
        DecompressionLimits.maxDecompressedEntrySize = maxDecompressedEntrySize;
    }

    public static byte[] readAllLimited(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            total += length;
            if (total > maxDecompressedEntrySize) {
                throw new IOException("解压条目大小超出限制: " + total);
            }
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }

    private static void validateLimit(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + "必须大于 0");
        }
    }
}
