package cn.geoair.comp.dynamic.ds.spring;

import cn.geoair.base.Gir;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceHelper;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceInitHelper;
import cn.geoair.comp.dynamic.ds.readwrite.spring.GirDsRdAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 15:28 @description： spring的自动装配
 */
@Configuration
@AutoConfigureBefore({GirDsRdAutoConfiguration.class})
public class DataSourceHelperAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IAdvDataSourceHelper.class)
    IAdvDataSourceHelper advDataSourceHelper() {
        Gir.log.info("自动装配IAdvDataSourceHelper");
        return new DefaultAdvDataSourceHelper();
    }

    @Bean
    @ConditionalOnMissingBean(IAdvDataSourceInitHelper.class)
    IAdvDataSourceInitHelper advDataSourceGetterHelper() {
        Gir.log.info("自动装配iAdvDataSourceInitHelper");
        return new DefaultAdvDataSourceInitHelper();
    }
}
