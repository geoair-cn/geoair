package cn.geoair.map.tile.forge.core.config;

import cn.hutool.extra.spring.SpringUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ：张俊
 * &#064;date ：Created in 2025/11/17 17:42
 * &#064;description： 配置属性类
 */
@Data
@ConfigurationProperties(prefix = "geoair.s3")
public class GirS3ConfigProperties {
    static GirS3ConfigProperties instance;


    public GirS3ConfigProperties() {
        instance = this;
    }


    public static GirS3ConfigProperties getInstance() {
        if (instance == null) {
            instance = SpringUtil.getBean(GirS3ConfigProperties.class);
        }
        return instance;
    }

    /**
     * AWS 访问密钥 ID
     */

    private String accessKeyId;

    /**
     * AWS 秘密访问密钥
     */

    private String secretKey;

    /**
     * S3 服务终端节点 (Endpoint)
     * 例如: http://localhost:9000 (MinIO), https://oss-cn-beijing.aliyuncs.com (阿里云OSS)
     * 如果为空，则使用标准 AWS S3 端点
     */
    private String endpoint;

    /**
     * S3 服务域名
     * 例如: http://localhost:9000 (MinIO), https://oss-cn-beijing.aliyuncs.com (阿里云OSS)
     * 如果为空，则使用标准 AWS S3 域名
     */
    private String domain;

    /**
     * S3 服务区域 (Region)
     * 例如: us-east-1, cn-north-1
     */

    private String region;

    /**
     * 默认 S3 Bucket 名称
     */

    private String defaultBucket;
}
