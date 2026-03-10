package cn.geoair.map.dynamic.dbservice.starter.run;

import cn.geoair.map.dynamic.dbservice.core.DsApiUserInfoHelper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import tk.mybatis.spring.annotation.MapperScan;

/**
 * @author ：张逢吉
 * @date ：Created in 11:38
 * @description： 自动装配模块
 */
@Configuration
@ComponentScan("cn.geoair.map.dynamic.dbservice")
@MapperScan("cn.geoair.map.dynamic.dbservice.starter.mapper")
public class AutoGirDbServiceConfiguration {

    @Bean
    @ConditionalOnMissingBean(DsApiUserInfoHelper.class)
    public DsApiUserInfoHelper dsApiUserInfoHelper() {
        return new DsApiUserInfoHelper() {
            @Override
            public String getSubjectName() {
                return "geoair";
            }

            @Override
            public String getSubjectId() {
                return "geoair";
            }
        };
    }
}
