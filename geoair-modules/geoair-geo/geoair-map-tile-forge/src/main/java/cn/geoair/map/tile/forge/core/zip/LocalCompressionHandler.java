package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
 

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

 
public class LocalCompressionHandler extends AbstractZipCompressionHandler {
    public static GiLogger log = GirLoggerFactory.getLogger();

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
