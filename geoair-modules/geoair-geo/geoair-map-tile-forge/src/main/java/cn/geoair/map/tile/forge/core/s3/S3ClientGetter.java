package cn.geoair.map.tile.forge.core.s3;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.config.GirS3ConfigProperties;
import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import com.amazonaws.services.s3.model.S3Object;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Paths;

/**
 * S3客户端抽象基类
 * 封装S3配置参数和客户端初始化逻辑，
 */


public class S3ClientGetter {
    public static GiLogger log = GirLoggerFactory.getLogger();

    GirS3ConfigProperties s3Config;
    static S3ClientGetter instance;

    public S3ClientGetter(GirS3ConfigProperties s3Config) {
        this.s3Config = s3Config;
        initS3Client();
        instance = this;
    }


    public static S3ClientGetter getInstance() {
        return instance = null == instance ? SpringUtil.getBean(S3ClientGetter.class) : instance;
    }


    // S3客户端实例（私有，通过getter暴露）
    private AmazonS3 s3Client;

    /**
     * 初始化S3客户端（PostConstruct确保在Bean初始化时执行）
     */

    protected void initS3Client() {
        try {

            // 校验必要参数
            if (StringUtils.isBlank(s3Config.getAccessKeyId()) || StringUtils.isBlank(s3Config.getSecretKey())) {
                throw new RuntimeException("AWS访问密钥（accessKeyId/secretKey）未配置");
            }
            if (StringUtils.isBlank(s3Config.getRegion())) {
                throw new RuntimeException("S3区域（region）未配置");
            }

            // 构建凭证
            BasicAWSCredentials credentials = new BasicAWSCredentials(s3Config.getAccessKeyId(), s3Config.getSecretKey());
            AmazonS3ClientBuilder clientBuilder = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials));

            // 配置端点（兼容非AWS S3存储，如MinIO、阿里云OSS等）
            if (StringUtils.isNotBlank(s3Config.getEndpoint())) {
                clientBuilder.withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(s3Config.getEndpoint(), s3Config.getRegion())
                );
            } else {
                // 标准AWS S3使用区域配置
                clientBuilder.withRegion(s3Config.getRegion());
            }

            // 启用路径风格访问（推荐，避免与Bucket名中的特殊字符冲突）
            clientBuilder.enablePathStyleAccess();

            // 构建客户端实例
            this.s3Client = clientBuilder.build();
            log.info("S3客户端初始化成功（endpoint: {}, region: {}, defaultBucket: {}）",
                    s3Config.getEndpoint(), s3Config.getRegion(), s3Config.getDefaultBucket());
        } catch (Exception e) {
            log.error("S3客户端初始化失败", e);
            throw new RuntimeException("S3客户端初始化失败：" + e.getMessage(), e);
        }
    }

    /**
     * 获取S3客户端实例
     *
     * @return 已初始化的AmazonS3实例
     */
    public AmazonS3 getClient() {
        if (s3Client == null) {
            throw new RuntimeException("S3客户端未初始化，请检查配置或初始化逻辑");
        }
        return s3Client;
    }

    /**
     * 获取默认Bucket（子类可直接调用）
     */
    public String getDefaultBucket() {
        GirS3ConfigProperties instance = GirS3ConfigProperties.getInstance();
        if (StringUtils.isBlank(instance.getDefaultBucket())) {
            throw new RuntimeException("默认Bucket（default-bucket）未配置");
        }
        return instance.getDefaultBucket();
    }


    /**
     * 从S3下载文件到本地临时目录（如果本地不存在）
     *
     * @param bucketName       S3桶名
     * @param remoteFilePath   远程文件路径（相对于桶根目录）
     * @param localTempDirPath 本地临时目录绝对路径
     */
    public void downloadFromS3IfNeeded(String bucketName, String remoteFilePath, String localTempDirPath) {
        try {
            String normalizedRemotePath = remoteFilePath.replace('\\', '/');
            File localFile = Paths.get(localTempDirPath, normalizedRemotePath).toFile();

            if (!FileUtil.exist(localFile)) {
                AmazonS3 s3Client = getClient();
                S3Object object = s3Client.getObject(bucketName, normalizedRemotePath);
                FileUtil.writeFromStream(object.getObjectContent(), localFile);
                log.info("从S3下载文件成功: bucket={}, key={}, local={}", bucketName, normalizedRemotePath, localFile.getAbsolutePath());
            }
        } catch (Exception e) {
            log.error("从S3下载文件失败: bucket={}, key={}", bucketName, remoteFilePath, e);
            throw new RuntimeException("从S3下载文件失败: " + remoteFilePath, e);
        }
    }
}
