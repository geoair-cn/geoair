package cn.geoair.map.dynamic.ds.spring;

import cn.geoair.gtc.base.Gir;
import cn.geoair.map.dynamic.ds.IAdvDataSourceHelper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

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
