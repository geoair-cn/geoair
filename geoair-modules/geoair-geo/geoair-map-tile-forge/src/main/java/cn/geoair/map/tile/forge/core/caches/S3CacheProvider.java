package cn.geoair.map.tile.forge.core.caches;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.config.GirS3ConfigProperties;
import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import cn.hutool.core.util.StrUtil;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/** 基于S3对象存储的缓存实现 */
public class S3CacheProvider implements FileCacheProvider {
    public static GiLogger log = GirLoggerFactory.getLogger();
    private final AmazonS3 s3Client;
    private final String bucketName;
    private final String domain;
    private final String cachePrefix; // 缓存键前缀，用于区分不同缓存空间
    private final ObjectMapper objectMapper;
    private static final String EXPIRATION_METADATA_KEY = "cache-expires-at";
    private static final long DEFAULT_EXPIRATION = 24 * 60 * 60 * 1000L; // 默认24小时过期

    /**
     * 构造函数（使用项目工具类获取S3配置）
     *
     * @param cachePrefix 缓存前缀
     */
    public S3CacheProvider(String cachePrefix) {
        this(GirS3ConfigProperties.getInstance().getDefaultBucket(), cachePrefix);
    }

    public S3CacheProvider() {
        this(GirS3ConfigProperties.getInstance().getDefaultBucket(), "default");
    }

    /**
     * 构造函数（指定存储桶和前缀）
     *
     * @param bucketName 存储桶名称
     * @param cachePrefix 缓存前缀
     */
    public S3CacheProvider(String bucketName, String cachePrefix) {
        this(
                S3ClientGetter.getInstance().getClient(),
                bucketName,
                cachePrefix,
                GirS3ConfigProperties.getInstance().getDomain());
    }

    /** 允许复用接入方管理的客户端，也便于在模拟存储环境验证流式传输。 */
    public S3CacheProvider(AmazonS3 client, String bucketName, String cachePrefix, String domain) {
        this.s3Client = client;
        this.bucketName = bucketName;
        this.domain = domain;
        this.cachePrefix = cachePrefix.endsWith("/") ? cachePrefix : cachePrefix + "/";
        this.objectMapper = new ObjectMapper();
        initBucket();
    }

    /** 初始化存储桶（不存在则创建） */
    private void initBucket() {
        if (!s3Client.doesBucketExistV2(bucketName)) {
            s3Client.createBucket(bucketName);
        }
    }

    /** 将缓存键转换为S3对象键 */
    private String getObjectKey(Object key) {
        // 安全转换键名，避免特殊字符
        String keyStr =
                key.toString().replaceAll("[^a-zA-Z0-9-_./]", "_").replaceAll("//+", "/"); // 合并连续斜杠
        while (keyStr.endsWith("/")) { // 移除最后的文件符号，以防被解析成文件夹
            keyStr = StrUtil.replaceLast(keyStr, "/", "");
        }
        return cachePrefix + keyStr;
    }

    /** 获取对象的完整访问URL */
    private String getObjectUrl(String objectKey) {
        return domain + "/" + objectKey;
    }

    @Override
    public String getName() {
        return "s3-tile-cache:" + bucketName + ":" + cachePrefix;
    }

    @Override
    public void put(Object key, Object value) {
        put(key, value, DEFAULT_EXPIRATION);
    }

    @Override
    public void put(Object key, Object value, long milliseconds) {
        String objectKey = getObjectKey(key);
        try {
            // 序列化对象
            byte[] data = serializeValue(value);

            // 设置元数据（包含过期时间）
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(data.length);

            String expiresAtString = null;
            if (milliseconds == -1) {
            } else {
                long expiresAt = System.currentTimeMillis() + milliseconds;
                expiresAtString = String.valueOf(expiresAt);
                metadata.addUserMetadata(EXPIRATION_METADATA_KEY, expiresAtString);
            }
            // 上传到S3
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            PutObjectRequest putRequest =
                    new PutObjectRequest(bucketName, objectKey, inputStream, metadata)
                            .withCannedAcl(CannedAccessControlList.Private);

            // 如果需要公共访问，可以设置为PublicRead
            // putRequest.withCannedAcl(CannedAccessControlList.PublicRead);

            s3Client.putObject(putRequest);
            log.info("缓存成功：{}", objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to put cache to S3: " + key, e);
        }
    }

    @Override
    public Object getObject(Object key) {
        try {
            byte[] aByte = getByte(key);
            if (aByte != null && aByte.length > 0) {
                return deserializeValue(aByte);
            } else {
                return aByte;
            }
        } catch (Exception e) {
            // 读取失败或异常时返回null
            return null;
        }
    }

    /** SDK 直接读取文件流，沿用原有对象键和原始字节格式。 */
    @Override
    public void putFile(Object key, Path source, long milliseconds) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(Files.size(source));
        if (milliseconds != -1)
            metadata.addUserMetadata(
                    EXPIRATION_METADATA_KEY,
                    String.valueOf(Math.addExact(System.currentTimeMillis(), milliseconds)));
        PutObjectRequest request =
                new PutObjectRequest(bucketName, getObjectKey(key), source.toFile())
                        .withMetadata(metadata)
                        .withCannedAcl(CannedAccessControlList.Private);
        // SDK 按大小选择分片上传，超大索引也不受单次 PutObject 的大小限制。
        TransferManager transfer = TransferManagerBuilder.standard().withS3Client(s3Client).build();
        Upload upload = null;
        try {
            upload = transfer.upload(request);
            upload.waitForCompletion();
        } catch (InterruptedException e) {
            if (upload != null)
                try {
                    upload.abort();
                } catch (RuntimeException cleanup) {
                    e.addSuppressed(cleanup);
                }
            Thread.currentThread().interrupt();
            throw new IOException("S3 索引备份已中断", e);
        } catch (Exception e) {
            throw new IOException("S3 索引备份失败", e);
        } finally {
            transfer.shutdownNow(false);
        }
    }

    @Override
    public boolean copyTo(Object key, Path target) throws IOException {
        try (S3Object object = s3Client.getObject(bucketName, getObjectKey(key))) {
            if (isExpired(object.getObjectMetadata())) return false;
            Files.copy(object.getObjectContent(), target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) return false;
            throw new IOException("S3 索引恢复失败", e);
        } catch (RuntimeException e) {
            throw new IOException("S3 索引恢复失败", e);
        }
    }

    @Override
    public boolean exists(Object key) {
        String objectKey = getObjectKey(key);
        return s3Client.doesObjectExist(bucketName, objectKey);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        Object value = getObject(key);
        if (value == null) {
            return null;
        }

        // 直接类型转换
        if (type.isInstance(value)) {
            return type.cast(value);
        }

        // 复杂类型转换
        try {
            return objectMapper.convertValue(value, type);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot convert cache value to type: " + type.getName(), e);
        }
    }

    @Override
    public byte[] getByte(Object key) {
        String objectKey = getObjectKey(key);
        try {
            // 检查对象是否存在
            if (!exists(key)) {
                return null;
            }

            // 获取对象并检查过期时间
            S3Object s3Object = s3Client.getObject(bucketName, objectKey);
            if (isExpired(s3Object.getObjectMetadata())) {
                evict(key); // 清理过期缓存
                return null;
            }

            // 读取并反序列化数据
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; // 使用4KB缓冲区
            int bytesRead;
            while ((bytesRead = s3Object.getObjectContent().read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            s3Object.close(); // 关闭流

            //            return deserializeValue(outputStream.toByteArray());
            return outputStream.toByteArray();
        } catch (Exception e) {
            // 读取失败或异常时返回null
            return null;
        }
    }

    @Override
    public String getString(Object key) {
        // 修复：之前错误地调用了getObjectKey而不是getObject
        Object value = getObject(key);
        return value != null ? value.toString() : null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        T value = get(key, (Class<T>) Object.class);
        if (value == null) {
            try {
                // 加载新值
                value = valueLoader.call();
                if (value != null) {
                    put(key, value); // 存入缓存
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load value for key: " + key, e);
            }
        }
        return value;
    }

    @Override
    public long pttl(Object key) {
        String objectKey = getObjectKey(key);
        try {
            if (!s3Client.doesObjectExist(bucketName, objectKey)) {
                return -2; // 不存在
            }

            ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, objectKey);
            String expiresAtStr = metadata.getUserMetadata().get(EXPIRATION_METADATA_KEY);

            if (expiresAtStr == null) {
                return -1; // 永不过期
            }

            long expiresAt = Long.parseLong(expiresAtStr);
            long now = System.currentTimeMillis();

            return expiresAt > now ? (expiresAt - now) : 0; // 已过期返回0
        } catch (Exception e) {
            return -2; // 异常视为不存在
        }
    }

    @Override
    public void evict(Object key) {
        String objectKey = getObjectKey(key);
        try {
            s3Client.deleteObject(bucketName, objectKey);
        } catch (Exception e) {
            // 忽略删除失败（对象可能已不存在）
        }
    }

    @Override
    public void evictByPreFix(Object prefix) {
        try {
            String objectKey = getObjectKey(prefix);
            // 批量删除前缀下的所有对象
            ObjectListing objectListing = s3Client.listObjects(bucketName, objectKey);

            while (true) {
                if (!objectListing.getObjectSummaries().isEmpty()) {
                    // 创建删除请求
                    DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName);
                    List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>();

                    for (S3ObjectSummary summary : objectListing.getObjectSummaries()) {
                        keys.add(new DeleteObjectsRequest.KeyVersion(summary.getKey()));
                    }

                    deleteRequest.setKeys(keys);
                    s3Client.deleteObjects(deleteRequest);
                }

                if (objectListing.isTruncated()) {
                    objectListing = s3Client.listNextBatchOfObjects(objectListing);
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear S3 cache: " + cachePrefix, e);
        }
    }

    @Override
    public void clear() {
        evictByPreFix("");
    }

    /** 序列化对象（支持多种类型） */
    private byte[] serializeValue(Object value) throws IOException {
        if (value == null) {
            return new byte[0];
        }

        if (value instanceof byte[]) {
            return (byte[]) value;
        }

        if (value instanceof String) {
            return ((String) value).getBytes(StandardCharsets.UTF_8);
        }

        if (value instanceof Serializable) {
            // 使用JSON序列化复杂对象
            return objectMapper.writeValueAsBytes(value);
        }

        // 其他类型转为字符串
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** 反序列化对象 */
    private Object deserializeValue(byte[] data) throws IOException {
        if (data.length == 0) {
            return null;
        }

        try {
            // 尝试JSON反序列化
            return objectMapper.readValue(data, Object.class);
        } catch (Exception e) {
            // 失败则直接返回处理
            return data;
        }
    }

    /** 检查对象是否过期 */
    private boolean isExpired(ObjectMetadata metadata) {
        String expiresAtStr = metadata.getUserMetadata().get(EXPIRATION_METADATA_KEY);
        if (expiresAtStr == null) {
            return false; // 没有过期时间则视为永不过期
        }

        try {
            long expiresAt = Long.parseLong(expiresAtStr);
            return System.currentTimeMillis() > expiresAt;
        } catch (NumberFormatException e) {
            return true; // 元数据异常视为过期
        }
    }

    /** 获取缓存对象的URL（如果需要对外访问） */
    public String getObjectUrl(Object key) {
        String objectKey = getObjectKey(key);
        if (s3Client.doesObjectExist(bucketName, objectKey)) {
            return getObjectUrl(objectKey);
        }
        return null;
    }

    /** 关闭资源（建议在应用销毁时调用） */
    public void destroy() {
        // 如果需要关闭客户端连接，可以在这里处理
        // s3Client.shutdown();
    }
}
