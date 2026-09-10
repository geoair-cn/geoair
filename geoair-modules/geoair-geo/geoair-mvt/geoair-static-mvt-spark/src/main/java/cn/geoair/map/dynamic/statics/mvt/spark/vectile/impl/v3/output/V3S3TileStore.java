package cn.geoair.map.dynamic.statics.mvt.spark.vectile.impl.v3.output;

import cn.geoair.map.dynamic.statics.mvt.spark.vectile.dto.v3.V3TileOutputConfig;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 将 V3 瓦片写入 S3 兼容对象存储的存储会话。
 *
 * @author 张逢吉
 */
final class V3S3TileStore implements V3TileStore {

    private final AmazonS3 client;
    private final String bucket;
    private final String prefix;
    private final boolean overwrite;
    private final String metadataFileName;

    V3S3TileStore(V3TileOutputConfig config) {
        this.client = buildClient(config);
        this.bucket = config.getS3Bucket();
        this.prefix = normalizePrefix(config.getS3Prefix());
        this.overwrite = config.isOverwrite();
        this.metadataFileName = config.getMetadataFileName();
    }

    @Override
    public void writeTile(int z, int x, int y, byte[] data, boolean gzip) throws IOException {
        String key = prefix + z + "/" + x + "/" + y + ".pbf";
        put(key, data, "application/x-protobuf", gzip ? "gzip" : null);
    }

    @Override
    public void writeMetadata(byte[] data) throws IOException {
        put(prefix + metadataFileName, data, "application/json", null);
    }

    @Override
    public void close() {
        if (client != null) {
            client.shutdown();
        }
    }

    private void put(String key, byte[] data, String contentType, String contentEncoding) throws IOException {
        try {
            if (!overwrite && client.doesObjectExist(bucket, key)) {
                throw new IOException("目标 S3 对象已存在且未开启覆盖: s3://" + bucket + "/" + key);
            }
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.length);
            metadata.setContentType(contentType);
            if (contentEncoding != null) {
                metadata.setContentEncoding(contentEncoding);
            }
            client.putObject(new PutObjectRequest(bucket, key, new ByteArrayInputStream(data), metadata));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("写入 S3 瓦片失败: s3://" + bucket + "/" + key, e);
        }
    }

    private static AmazonS3 buildClient(V3TileOutputConfig config) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        if (config.getS3AccessKeyId() == null || config.getS3AccessKeyId().trim().isEmpty()) {
            builder.withCredentials(DefaultAWSCredentialsProviderChain.getInstance());
        } else {
            builder.withCredentials(new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(config.getS3AccessKeyId(), config.getS3SecretKey())));
        }
        if (config.getS3Endpoint() == null || config.getS3Endpoint().trim().isEmpty()) {
            builder.withRegion(config.getS3Region());
        } else {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(config.getS3Endpoint(), config.getS3Region()));
            builder.enablePathStyleAccess();
        }
        return builder.build();
    }

    private static String normalizePrefix(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String normalized = value.replace('\\', '/').replaceAll("^/+|/+$", "");
        return normalized.isEmpty() ? "" : normalized + "/";
    }
}
