package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.output;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputConfig;
import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputType;

/**
 * 创建 V3 瓦片存储会话。
 *
 * @author 张逢吉
 */
public final class V3TileStoreFactory {

    private V3TileStoreFactory() {
    }

    /** 根据输出配置创建一个独立的存储会话。 */
    public static V3TileStore open(V3TileOutputConfig config) {
        validate(config);
        if (config.getOutputType() == V3TileOutputType.LOCAL_DIRECTORY) {
            return new V3LocalTileStore(config);
        }
        if (config.getOutputType() == V3TileOutputType.MBTILES
                || config.getOutputType() == V3TileOutputType.PMTILES) {
            return new V3LocalTileStore(config.getStagingDirectory(), config.isOverwrite(), config.getMetadataFileName());
        }
        if (config.getOutputType() == V3TileOutputType.S3) {
            return new V3S3TileStore(config);
        }
        throw new IllegalArgumentException("不支持以 TileStore 方式写入的 V3 输出类型: " + config.getOutputType());
    }

    /** 校验目录与 S3 输出所需的参数。 */
    public static void validate(V3TileOutputConfig config) {
        if (config == null || config.getOutputType() == null) {
            throw new IllegalArgumentException("V3 输出配置不能为空");
        }
        if (config.isWriteMetadata() && isBlank(config.getMetadataFileName())) {
            throw new IllegalArgumentException("V3 启用元数据输出时必须设置 metadataFileName");
        }
        if (config.getOutputType() == V3TileOutputType.LOCAL_DIRECTORY
                && isBlank(config.getLocalDirectory())) {
            throw new IllegalArgumentException("V3 本地目录输出必须设置 localDirectory");
        }
        if ((config.getOutputType() == V3TileOutputType.MBTILES
                || config.getOutputType() == V3TileOutputType.PMTILES)
                && isBlank(config.getStagingDirectory())) {
            throw new IllegalArgumentException("V3 归档输出必须设置 stagingDirectory");
        }
        if (config.getOutputType() == V3TileOutputType.MBTILES && isBlank(config.getMbtilesFile())) {
            throw new IllegalArgumentException("V3 MBTiles 输出必须设置 mbtilesFile");
        }
        if (config.getOutputType() == V3TileOutputType.PMTILES && isBlank(config.getPmtilesFile())) {
            throw new IllegalArgumentException("V3 PMTiles 输出必须设置 pmtilesFile");
        }
        if (config.getArchiveBatchSize() <= 0) {
            throw new IllegalArgumentException("V3 archiveBatchSize 必须大于 0");
        }
        if (config.getOutputType() == V3TileOutputType.S3) {
            if (isBlank(config.getS3Bucket())) {
                throw new IllegalArgumentException("V3 S3 输出必须设置 s3Bucket");
            }
            if (isBlank(config.getS3Region())) {
                throw new IllegalArgumentException("V3 S3 输出必须设置 s3Region");
            }
            if (isBlank(config.getS3AccessKeyId()) != isBlank(config.getS3SecretKey())) {
                throw new IllegalArgumentException("V3 S3 accessKeyId 与 secretKey 必须同时配置或同时留空");
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
