package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.output;

import java.io.Closeable;
import java.io.IOException;

/**
 * V3 瓦片二进制存储会话。
 *
 * <p>一个 Spark 分区独占一个会话实例，避免在 executor 之间共享文件句柄或 S3 客户端状态。</p>
 *
 * @author 张逢吉
 */
public interface V3TileStore extends Closeable {

    /** 写入一个已完成编码的 PBF 瓦片。 */
    void writeTile(int z, int x, int y, byte[] data, boolean gzip) throws IOException;

    /** 写入任务级元数据。 */
    void writeMetadata(byte[] data) throws IOException;
}
