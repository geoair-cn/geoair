package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3;

import java.io.Serializable;

/**
 * V3 多图层切片任务的写出统计。
 * <p>
 * 统计在切片过程中由 Spark 累加器汇总，任务结束后可直接读取，
 * 使用方无需再遍历输出目录或查询输出表。
 *
 * @author 张逢吉
 */
public class V3TileWriteStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 实际写出的瓦片数量 */
    private final long tilesWritten;
    /** 写出的瓦片数据总字节数（gzip 时为压缩后的字节数） */
    private final long bytesWritten;
    /** 批量写入的批次数量 */
    private final long batchesWritten;
    /** 读取到的要素数量 */
    private final long featuresRead;
    /** 归档文件最终大小（MBTiles / PMTiles），非归档输出为 0 */
    private final long archiveBytes;

    public V3TileWriteStats(long tilesWritten, long bytesWritten, long batchesWritten,
                            long featuresRead, long archiveBytes) {
        this.tilesWritten = tilesWritten;
        this.bytesWritten = bytesWritten;
        this.batchesWritten = batchesWritten;
        this.featuresRead = featuresRead;
        this.archiveBytes = archiveBytes;
    }

    public long getTilesWritten() {
        return tilesWritten;
    }

    public long getBytesWritten() {
        return bytesWritten;
    }

    public long getBatchesWritten() {
        return batchesWritten;
    }

    public long getFeaturesRead() {
        return featuresRead;
    }

    public long getArchiveBytes() {
        return archiveBytes;
    }

    /**
     * 最终占用大小：归档输出取归档文件大小，其余取瓦片数据总字节数。
     */
    public long getOutputBytes() {
        return archiveBytes > 0L ? archiveBytes : bytesWritten;
    }

    @Override
    public String toString() {
        return "V3TileWriteStats{tiles=" + tilesWritten + ", bytes=" + bytesWritten
                + ", archiveBytes=" + archiveBytes + ", features=" + featuresRead + "}";
    }
}
