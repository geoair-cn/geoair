package cn.geoair.comp.dynamic.ds.spring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import cn.geoair.base.Gir;
import cn.geoair.comp.dynamic.ds.IAdvDataSourceHelper;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 15:28 @description： spring的自动装配
 */

public class DataSourceHelperAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(IAdvDataSourceHelper.class)
	IAdvDataSourceHelper advDataSourceHelper() {
		Gir.log.info("自动装配IAdvDataSourceHelper");
		return new DefaultAdvDataSourceHelper();
	}

}
