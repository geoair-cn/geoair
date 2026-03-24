package cn.geoair.comp.db.service.starter.run;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import cn.geoair.comp.db.service.core.DsApiUserInfoHelper;

import tk.mybatis.spring.annotation.MapperScan;

/**
 * @author ：张逢吉
 * @date ：Created in 11:38 @description： 自动装配模块
 */
@Configuration
@ComponentScan("cn.geoair.comp.db.service")
@MapperScan("cn.geoair.comp.db.service.starter.mapper")
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
