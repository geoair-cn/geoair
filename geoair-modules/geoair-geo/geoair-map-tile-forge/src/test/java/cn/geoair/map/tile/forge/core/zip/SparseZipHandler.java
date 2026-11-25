package cn.geoair.map.tile.forge.core.zip;

import java.io.IOException;
import java.util.Arrays;

/** 虚拟大偏移，仅持有中央目录的几个KB，绝不创建800GB测试文件。 */
class SparseZipHandler extends AbstractZipCompressionHandler {
    final long offset;
    final byte[] directory;
    int reads;

    SparseZipHandler(long offset, byte[] directory) {
        this.offset = offset;
        this.directory = directory;
    }

    @Override
    protected byte[] readRange(String source, long start, long end) throws IOException {
        if (start < offset || end >= offset + directory.length)
            throw new IOException("测试只允许读取中央目录");
        reads++;
        return Arrays.copyOfRange(
                directory, Math.toIntExact(start - offset), Math.toIntExact(end - offset + 1));
    }

    @Override
    public long getFileSize(String source) {
        return offset + directory.length;
    }
}
