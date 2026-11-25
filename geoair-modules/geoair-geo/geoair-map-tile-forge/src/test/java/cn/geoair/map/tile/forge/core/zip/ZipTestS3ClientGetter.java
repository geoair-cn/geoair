package cn.geoair.map.tile.forge.core.zip;

import cn.geoair.map.tile.forge.core.config.GirS3ConfigProperties;
import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import com.amazonaws.services.s3.AmazonS3;

/** 不创建真实 S3 客户端，不访问外网。 */
class ZipTestS3ClientGetter extends S3ClientGetter {
    private final AmazonS3 client;

    ZipTestS3ClientGetter(AmazonS3 client) {
        super(new GirS3ConfigProperties());
        this.client = client;
    }

    @Override
    protected void initS3Client() {}

    @Override
    public AmazonS3 getClient() {
        return client;
    }

    @Override
    public String getDefaultBucket() {
        return "bucket";
    }
}
