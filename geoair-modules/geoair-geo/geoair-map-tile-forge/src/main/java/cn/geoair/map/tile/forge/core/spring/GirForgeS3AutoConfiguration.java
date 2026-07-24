package cn.geoair.map.tile.forge.core.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.tile.forge.core.config.GirS3ConfigProperties;
import cn.geoair.map.tile.forge.core.s3.S3ClientGetter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GirS3ConfigProperties.class)
public class GirForgeS3AutoConfiguration {
    private static final GiLogger log =
            GirLoggerFactory.getLogger(GirForgeS3AutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public S3ClientGetter s3ClientGetter(GirS3ConfigProperties girS3ConfigProperties) {
        return new S3ClientGetter(girS3ConfigProperties);
    }
}
