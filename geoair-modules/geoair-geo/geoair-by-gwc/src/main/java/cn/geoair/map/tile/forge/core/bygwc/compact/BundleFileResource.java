package cn.geoair.map.tile.forge.core.bygwc.compact;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;

import org.apache.commons.io.input.BoundedInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * 资源类，用于访问ArcGIS紧凑型缓存中的单个瓦片数据
 *
 * @author Bjoern Saxe
 */
public class BundleFileResource {
    /** 日志记录器 */
    private static GiLogger log = GirLoggerFactory.getLogger(BundleFileResource.class);

    /** 瓦片包文件路径 */
    private final String bundleFilePath;

    /** 瓦片在文件中的偏移量 */
    private final long tileOffset;

    /** 瓦片数据大小 */
    private final int tileSize;

    /**
     * 构造函数，初始化BundleFileResource实例
     *
     * @param bundleFilePath 瓦片包文件路径
     * @param tileOffset 瓦片在文件中的偏移量
     * @param tileSize 瓦片数据大小
     */
    public BundleFileResource(String bundleFilePath, long tileOffset, int tileSize) {
        this.bundleFilePath = bundleFilePath;
        this.tileOffset = tileOffset;
        this.tileSize = tileSize;
    }

    /**
     * 获取瓦片数据大小
     *
     * @return 瓦片数据大小（字节）
     */
    public long getSize() {
        return tileSize;
    }

    /**
     * 将瓦片数据传输到指定的目标通道
     *
     * @param target 目标可写通道
     * @return 传输的数据大小
     * @throws IOException 当发生I/O异常时抛出
     */
    @SuppressWarnings("PMD.EmptyWhileStmt")
    public long transferTo(WritableByteChannel target) throws IOException {
        try (FileInputStream fin = new FileInputStream(new File(bundleFilePath));
                FileChannel in = fin.getChannel()) {
            final long size = tileSize;
            long written = 0;
            while ((written += in.transferTo(tileOffset + written, size, target)) < size)
                ;
            return size;
        }
    }

    /**
     * 不支持的操作，因为ArcGIS缓存是只读的
     *
     * @param channel 源可读通道
     * @return 传输的数据大小
     * @throws IOException 当发生I/O异常时抛出
     */
    public long transferFrom(ReadableByteChannel channel) throws IOException {
        // 不支持写入操作
        return 0;
    }

    /**
     * 获取瓦片数据的输入流
     *
     * @return 瓦片数据输入流
     * @throws IOException 当发生I/O异常时抛出
     */
    public InputStream getInputStream() throws IOException {
        // 这里有bug，从这里tileOffset跳过了之后，直接读到了文件末尾，会导致这个流特别大
        //        FileInputStream fis = new FileInputStream(bundleFilePath);
        //        long skipped = fis.skip(tileOffset);
        //        if (skipped != tileOffset) {
        //            log.error(
        //                    "tried to skip to tile offset "
        //                            + tileOffset
        //                            + " in "
        //                            + bundleFilePath
        //                            + " but skipped "
        //                            + skipped
        //                            + " instead.");
        //        }
        //
        //        return fis;

        FileInputStream fis = new FileInputStream(bundleFilePath);
        long skipped = fis.skip(tileOffset);
        if (skipped != tileOffset) {
            log.error(
                    "tried to skip to tile offset {} in {} but skipped {} instead.",
                    tileOffset,
                    bundleFilePath,
                    skipped);
        }
        return new BoundedInputStream(fis, tileSize);
    }

    /**
     * 获取文件最后修改时间
     *
     * @return 文件最后修改时间戳
     */
    public long getLastModified() {
        File f = new File(bundleFilePath);

        return f.lastModified();
    }
}
