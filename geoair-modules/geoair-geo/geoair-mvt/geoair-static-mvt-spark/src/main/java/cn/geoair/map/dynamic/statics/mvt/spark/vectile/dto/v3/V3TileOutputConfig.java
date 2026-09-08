package cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.DataSourceConfig;
import cn.geoair.map.dynamic.tools.grid.dto.TileYAxis;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * V3 静态 MVT 的输出配置。
 *
 * <p>该配置仅由 V3 使用，统一承载全部输出介质的参数。目录和对象存储均使用
 * {@code z/x/y.pbf} 结构；PostgreSQL 使用 {@link #postgresqlOutputSource}。</p>
 *
 * @author 张逢吉
 */
@Data
@Accessors(chain = true)
public class V3TileOutputConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 输出介质，默认保持 V3 既有的 PostgreSQL 行为。 */
    private V3TileOutputType outputType = V3TileOutputType.POSTGRESQL;

    /** PostgreSQL 输出数据源，仅 {@link V3TileOutputType#POSTGRESQL} 使用。 */
    private DataSourceConfig postgresqlOutputSource;

    /**
     * 本地目录输出根路径，仅 {@link V3TileOutputType#LOCAL_DIRECTORY} 使用。
     * Spark 集群模式下必须是所有 executor 都可访问的共享挂载目录，不能只填写 Driver 本机路径。
     */
    private String localDirectory;

    /** MBTiles 或 PMTiles 归档时的共享本地中间目录。 */
    private String stagingDirectory;

    /** MBTiles 输出文件路径，仅 {@link V3TileOutputType#MBTILES} 使用。 */
    private String mbtilesFile;

    /** PMTiles V3 输出文件路径，仅 {@link V3TileOutputType#PMTILES} 使用。 */
    private String pmtilesFile;

    /** 归档写入 SQLite 时每批提交的瓦片数。 */
    private int archiveBatchSize = 1000;

    /** S3 桶名称，仅 {@link V3TileOutputType#S3} 使用。 */
    private String s3Bucket;

    /** S3 对象前缀，例如 {@code tiles/base-map/v1}。 */
    private String s3Prefix = "";

    /** S3 服务端点；填写后可连接 MinIO、OSS 等 S3 兼容服务。 */
    private String s3Endpoint;

    /** S3 区域；普通 AWS S3 必填，S3 兼容服务建议填写。 */
    private String s3Region = "us-east-1";

    /** S3 Access Key ID；为空时由 AWS SDK 默认凭证链提供凭证。 */
    private String s3AccessKeyId;

    /** S3 Secret Key。 */
    private String s3SecretKey;

    /** 目录名或对象 key 中使用的 Y 轴约定，默认 XYZ。 */
    private TileYAxis tileYAxis = TileYAxis.XYZ;

    /** 是否允许覆盖已存在的瓦片文件或对象。 */
    private boolean overwrite = true;

    /** 是否在输出根目录写入 V3 任务元数据。 */
    private boolean writeMetadata = true;

    /** 元数据文件名或对象 key 名称。 */
    private String metadataFileName = "geoair-v3-mvt.json";

    /** 创建本地目录输出配置。 */
    public static V3TileOutputConfig localDirectory(String directory) {
        return new V3TileOutputConfig()
                .setOutputType(V3TileOutputType.LOCAL_DIRECTORY)
                .setLocalDirectory(directory);
    }

    /** 创建 PostgreSQL 输出配置。 */
    public static V3TileOutputConfig postgresql(DataSourceConfig outputSource) {
        return new V3TileOutputConfig()
                .setOutputType(V3TileOutputType.POSTGRESQL)
                .setPostgresqlOutputSource(outputSource);
    }

    /** 创建 S3 输出配置。 */
    public static V3TileOutputConfig s3(String bucket, String prefix) {
        return new V3TileOutputConfig()
                .setOutputType(V3TileOutputType.S3)
                .setS3Bucket(bucket)
                .setS3Prefix(prefix);
    }

    /** 创建 MBTiles 两阶段输出配置。 */
    public static V3TileOutputConfig mbtiles(String stagingDirectory, String mbtilesFile) {
        return new V3TileOutputConfig()
                .setOutputType(V3TileOutputType.MBTILES)
                .setStagingDirectory(stagingDirectory)
                .setMbtilesFile(mbtilesFile);
    }

    /** 创建 PMTiles 两阶段输出配置。 */
    public static V3TileOutputConfig pmtiles(String stagingDirectory, String pmtilesFile) {
        return new V3TileOutputConfig()
                .setOutputType(V3TileOutputType.PMTILES)
                .setStagingDirectory(stagingDirectory)
                .setPmtilesFile(pmtilesFile);
    }
}
