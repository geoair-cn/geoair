package cn.geoair.map.dynamic.geoserver.config;

import cn.geoair.base.Gir;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 解决 Spring Security 对 PasswordEncoder 的歧义依赖： 1. 给 GeoServer 的 pbePasswordEncoder
 * 标记 @Primary 2. 确保在 Spring Security 自动配置前执行
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 最高优先级执行，抢在 Spring Security 自动配置前
public class PasswordEncoderPrimaryResolver implements BeanDefinitionRegistryPostProcessor {

	// 选择 GeoServer 默认的 PBE 加密编码器作为主 Bean
	private static final String PRIMARY_PASSWORD_ENCODER = "pbePasswordEncoder";

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		// 1. 检查目标 Bean 是否存在
		if (registry.containsBeanDefinition(PRIMARY_PASSWORD_ENCODER)) {
			BeanDefinition encoderDef = registry.getBeanDefinition(PRIMARY_PASSWORD_ENCODER);
			// 2. 标记为 @Primary，让 Spring 优先选择该 Bean
			encoderDef.setPrimary(true);
			Gir.log.info("[GeoServer Jar] 已将 " + PRIMARY_PASSWORD_ENCODER + " 标记为 @Primary，解决 PasswordEncoder 歧义");
		}
		else {
			// 兜底：若 pbePasswordEncoder 不存在，选择 plainTextPasswordEncoder（最低兼容）
			String fallbackEncoder = "plainTextPasswordEncoder";
			if (registry.containsBeanDefinition(fallbackEncoder)) {
				BeanDefinition fallbackDef = registry.getBeanDefinition(fallbackEncoder);
				fallbackDef.setPrimary(true);
				Gir.log.info("[GeoServer Jar] pbePasswordEncoder 不存在，将 " + fallbackEncoder + " 标记为 @Primary");
			}
		}
	}

	@Override
	public void postProcessBeanFactory(
			org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory)
			throws BeansException {
		// 无需额外处理
	}

}
