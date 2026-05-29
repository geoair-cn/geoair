package cn.geoair.comp.dynamic.ds.readwrite.spring;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceHelper;
import cn.geoair.comp.dynamic.ds.readwrite.GirReadWriteDataSource;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

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
@ConditionalOnProperty(prefix = "spring.datasource.gir.readwrite", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GirDsRdAutoConfiguration {
    private static final GiLogger log = GirLogger.getLoger(GirDsRdAutoConfiguration.class);

    @Bean
    @Primary
    @ConditionalOnBean({IAdvDataSourceHelper.class, DataSourceProperties.class})
    @ConditionalOnProperty(prefix = "spring.datasource.gir.readwrite", name = "read-urls")
    public GirReadWriteDataSource girReadWriteDataSource(
            GirRdDataSourceProperties properties,
            DataSourceProperties dataSourceProperties,
            IAdvDataSourceHelper dataSourceHelper) {

        log.info("开始构建读写分离数据源，组名: {}, 从库数量: {}, 策略: {}",
                properties.getGroupName(),
                properties.getReadwrite().getReadUrls() != null ? properties.getReadwrite().getReadUrls().size() : 0,
                properties.getReadwrite().getReadStrategy() != null ? properties.getReadwrite().getReadStrategy().getDescription() : "RANDOM");

        GirReadWriteDataSource dataSource = GirReadWriteDataSourceBuilder.builder(properties, dataSourceProperties, dataSourceHelper)
                .build();

        log.info("读写分离数据源构建完成");
        return dataSource;
    }

//    /**
//     * 当没有配置 read-urls 时，返回原始主库数据源
//     */
//    @Bean
//    @Primary
//    @ConditionalOnMissingBean(name = "girReadWriteDataSource")
//    @ConditionalOnBean({IAdvDataSourceHelper.class, DataSourceProperties.class})
//    public DataSource originalDataSource(
//            DataSourceProperties dataSourceProperties,
//            IAdvDataSourceHelper dataSourceHelper) {
//
//        return dataSourceHelper.getDbDataSourceByApo(
//                GirSpringDataSourceUtils.convertToDataSourceApo(dataSourceProperties)
//        );
//    }


}
