package cn.geoair.map.tile.forge.core.zip;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LocalCompressionHandler extends AbstractZipCompressionHandler {

    @Override
    public List<byte[]> readFileByChunks(String source, long startOffset, long totalSize, int chunkSize ) throws IOException {
        List<byte[]> chunks = new ArrayList<>();
        long remaining = totalSize;
        long currentOffset = startOffset;

        try (RandomAccessFile raf = new RandomAccessFile(source, "r")) {
            while (remaining > 0) {
                int readSize = (int) Math.min(chunkSize, remaining);
                byte[] chunk = new byte[readSize];
                raf.seek(currentOffset);
                raf.readFully(chunk);
                chunks.add(chunk);

                currentOffset += readSize;
                remaining -= readSize;
            }
        }
        return chunks;
    }

    @Override
    protected byte[] readRange(String source, long start, long end) throws IOException {
        if (start > end) {
            throw new IllegalArgumentException("无效的范围：start=" + start + ", end=" + end);
        }
        int length = (int) (end - start + 1);
        byte[] data = new byte[length];
        try (RandomAccessFile raf = new RandomAccessFile(source, "r")) {
            raf.seek(start);
            raf.readFully(data);
        }
        return data;
    }

    @Override
    public long getFileSize(String source) {
        File file = new File(source);
        if (!file.exists()) {
            throw new RuntimeException("本地文件不存在：" + source);
        }
        return file.length();
    }


}
