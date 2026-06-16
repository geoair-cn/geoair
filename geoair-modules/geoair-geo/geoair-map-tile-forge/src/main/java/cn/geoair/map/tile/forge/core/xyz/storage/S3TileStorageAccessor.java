package cn.geoair.map.tile.forge.core.xyz.storage;

import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * S3存储访问器（通过S3 SDK模拟目录扫描）
 */
public class S3TileStorageAccessor extends AbstractTileStorageAccessor {

    static S3TileStorageAccessor instance = null;

    private final AmazonS3 s3Client;
    private final String bucketName;
    private final String rootPrefix;

    /**
     * 单例获取
     */
    public static S3TileStorageAccessor getInstance() {
        if (instance == null) {
            instance = new S3TileStorageAccessor();
        }
        return instance;
    }

    /**
     * 构造函数
     */
    public S3TileStorageAccessor() {
        this.s3Client = S3ClientGetter.getInstance().getClient();
        this.bucketName = S3ClientGetter.getInstance().getDefaultBucket();
        this.rootPrefix = "";
    }

    /**
     * 构造函数（指定Bucket和根前缀）
     */
    public S3TileStorageAccessor(String bucketName, String rootPrefix) {
        this.s3Client = S3ClientGetter.getInstance().getClient();
        this.bucketName = StringUtils.isNotBlank(bucketName) ? bucketName : S3ClientGetter.getInstance().getDefaultBucket();
        this.rootPrefix = normalizePrefix(rootPrefix);
    }

    /**
     * 标准化前缀
     */
    private String normalizePrefix(String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return "";
        }
        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    /**
     * 构建完整路径
     */
//    private String buildPath(String basePath, String... parts) {
//        StringBuilder sb = new StringBuilder(rootPrefix);
//        if (StringUtils.isNotBlank(basePath)) {
//            sb.append(basePath).append("/");
//        }
//        sb.append(String.join("/", parts));
//        return sb.toString();
//    }

    /**
     * 判断路径是否存在
     */
    private boolean exists(String path) {
        String prefix = path.endsWith("/") ? path : path + "/";

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .withMaxKeys(1);

        ListObjectsV2Result result = s3Client.listObjectsV2(request);
        return !result.getObjectSummaries().isEmpty() || !result.getCommonPrefixes().isEmpty();
    }

    @Override
    protected boolean zLevelExists(String basePath, int z) {
        String zPath = buildPath(basePath, String.valueOf(z));
        return exists(zPath + "/");
    }

    @Override
    protected boolean xLevelExists(String basePath, int z, int x) {
        String xPath = buildPath(basePath, String.valueOf(z), String.valueOf(x));
        return exists(xPath + "/");
    }

    @Override
    protected boolean yTileExists(String basePath, int z, int x, int y, String format) {
        String yPath = buildPath(basePath, String.valueOf(z), String.valueOf(x), y + "." + format);
        return exists(yPath);
    }

    /**
     * 批量检查Z层级是否存在
     */
    @Override
    public Set<Integer> batchCheckZLevels(String basePath, List<Integer> zLevels) {
        Set<Integer> existingZs = new HashSet<>();

        // 构建所有待检查的Z路径
        List<String> pathsToCheck = zLevels.stream()
                .map(z -> buildPath(basePath, String.valueOf(z)) + "/")
                .collect(Collectors.toList());

        // 批量检查（S3需逐个检查，但可以优化请求）
        for (String path : pathsToCheck) {
            String prefix = path.startsWith("/") ? path.substring(1) : path;

            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(prefix)
                    .withMaxKeys(1);

            ListObjectsV2Result result = s3Client.listObjectsV2(request);
            if (!result.getObjectSummaries().isEmpty() || !result.getCommonPrefixes().isEmpty()) {
                // 解析Z值
                String[] parts = prefix.split("/");
                if (parts.length >= 2 && isNumeric(parts[parts.length - 2])) {
                    existingZs.add(Integer.parseInt(parts[parts.length - 2]));
                }
            }
        }

        return existingZs;
    }

    /**
     * 批量检查X层级是否存在
     */
    @Override
    public Set<Integer> batchCheckXLevels(String basePath, int z, List<Integer> xLevels) {
        Set<Integer> existingXs = new HashSet<>();

        String zPath = buildPath(basePath, String.valueOf(z)) + "/";

        for (int x : xLevels) {
            String xPath = zPath + x + "/";

            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(xPath)
                    .withMaxKeys(1);

            ListObjectsV2Result result = s3Client.listObjectsV2(request);
            if (!result.getObjectSummaries().isEmpty() || !result.getCommonPrefixes().isEmpty()) {
                existingXs.add(x);
            }
        }

        return existingXs;
    }

    /**
     * 批量检查Y瓦片是否存在
     */
    @Override
    public Set<Integer> batchCheckYLevels(String basePath, int z, int x, String format, List<Integer> yLevels) {
        Set<Integer> existingYs = new HashSet<>();

        String xPath = buildPath(basePath, String.valueOf(z), String.valueOf(x)) + "/";

        // 构建所有可能的格式后缀
        List<String> formats = new ArrayList<>();
        formats.add(format.toLowerCase());
        if (!format.equalsIgnoreCase("png")) formats.add("png");
        if (!format.equalsIgnoreCase("jpg")) formats.add("jpg");
        if (!format.equalsIgnoreCase("jpeg")) formats.add("jpeg");

        for (int y : yLevels) {
            boolean exists = false;

            for (String fmt : formats) {
                String yPath = xPath + y + "." + fmt;

                ListObjectsV2Request request = new ListObjectsV2Request()
                        .withBucketName(bucketName)
                        .withPrefix(yPath)
                        .withMaxKeys(1);

                ListObjectsV2Result result = s3Client.listObjectsV2(request);
                if (!result.getObjectSummaries().isEmpty()) {
                    exists = true;
                    break;
                }
            }

            if (exists) {
                existingYs.add(y);
            }
        }

        return existingYs;
    }

    /**
     * 构建路径辅助方法
     */
    private String buildPath(String basePath, String... parts) {
        StringBuilder sb = new StringBuilder(rootPrefix);
        if (StringUtils.isNotBlank(basePath)) {
            sb.append(basePath).append("/");
        }
        sb.append(String.join("/", parts));
        return sb.toString();
    }


}
