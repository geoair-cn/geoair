package cn.geoair.comp.dynamic.ds.readwrite.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceHelper;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceInitHelper;
import cn.geoair.comp.dynamic.ds.readwrite.GirReadWriteDataSource;
import cn.geoair.comp.dynamic.ds.readwrite.log.RdLog;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 读写分离数据源自动配置
 *
 * @author 张逢吉
 * @date Created in 2025/10/9 15:28
 */
@Configuration
@EnableConfigurationProperties(GirRdDataSourceProperties.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnClass({IAdvDataSourceHelper.class, DataSource.class})
@ConditionalOnProperty(
        prefix = "spring.datasource.geoair.readwrite",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false)
public class GirDsRdAutoConfiguration {
    private static final GiLogger log = GirLoggerFactory.getLogger(GirDsRdAutoConfiguration.class);

    public GirDsRdAutoConfiguration() {
        log.info("GirDsRdAutoConfiguration initialized");
    }

    @Bean
    @Primary
    public GirReadWriteDataSource girReadWriteDataSource(
            GirRdDataSourceProperties properties,
            DataSourceProperties dataSourceProperties,
            ObjectProvider<IAdvDataSourceInitHelper> provider) {
        IAdvDataSourceInitHelper initHelper = provider.getIfAvailable();
        if (initHelper == null) {
            log.warn("IAdvDataSourceInitHelper not available, skip creating readwrite datasource");
            return null;
        }
        if (GutilObject.isEmpty(properties.getReadwrite().findReadUrlList())) {
            throw new RuntimeException("readwrite readUrlList is empty!");
        }
        log.info(
                "开始构建读写分离数据源，组名: {}, 从库数量: {}, 策略: {}",
                properties.getGroupName(),
                GutilObject.isNotEmpty(properties.getReadwrite().findValidDataSources())
                        ? properties.getReadwrite().findReadUrlList().size()
                        : 0,
                properties.getReadwrite().getReadStrategy() != null
                        ? properties.getReadwrite().getReadStrategy().getDescription()
                        : "轮询策略");

        GirReadWriteDataSource dataSource =
                GirSpringReadWriteDataSourceBuilder.builder(
                                properties, dataSourceProperties, initHelper)
                        .build();

        RdLog.minLogLevel = properties.minLogLevel;
        RdLog.useIndependentLog = properties.useIndependentLog;

        log.info("读写分离数据源构建完成");
        return dataSource;
    }
}
