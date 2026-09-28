package cn.geoair.map.tile.forge.core.zip;

import java.io.EOFException;
import java.io.IOException;

/**
 * ZIP 中央目录的分块读取器。每次扫描独享固定大小缓冲，跨块条目连续读取，
 * 不缓存整个压缩包，也不为每个条目单独发起范围请求。
 */
final class CentralDirectoryReader {
    private final AbstractZipCompressionHandler compressionHandler;
    private final String source;
    private long nextOffset;
    private long remaining;
    private byte[] buffer = new byte[0];
    private int position;

    CentralDirectoryReader(AbstractZipCompressionHandler compressionHandler,
                           String source, long offset, long size) {
        this.compressionHandler = compressionHandler;
        this.source = source;
        this.nextOffset = offset;
        this.remaining = size;
    }

    byte[] read(int length) throws IOException {
        if (length > remaining + buffer.length - position) {
            throw new EOFException("ZIP中央目录条目不完整");
        }
        byte[] result = new byte[length];
        int copied = 0;
        while (copied < length) {
            if (position == buffer.length) {
                int count = (int) Math.min(remaining, AbstractZipCompressionHandler.MAX_CHUNK_SIZE);
                buffer = compressionHandler.readRange(source, nextOffset, nextOffset + count - 1);
                if (buffer.length != count) throw new EOFException("ZIP中央目录读取不完整");
                nextOffset += count;
                remaining -= count;
                position = 0;
            }
            int count = Math.min(length - copied, buffer.length - position);
            System.arraycopy(buffer, position, result, copied, count);
            copied += count;
            position += count;
        }
        return result;
    }
}
