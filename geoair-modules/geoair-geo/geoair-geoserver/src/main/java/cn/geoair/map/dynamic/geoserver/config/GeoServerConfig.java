package cn.geoair.map.dynamic.geoserver.config;

import java.io.File;

import javax.servlet.ServletContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

import cn.geoair.base.Gir;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ImportResource(locations = { "classpath*:/applicationContext.xml"
// , "classpath*:/applicationContext.xml"
})
@Configuration
@EnableConfigurationProperties(GirGeoServerProperties.class)
public class GeoServerConfig
		implements BeanPostProcessor, ApplicationContextAware, BeanDefinitionRegistryPostProcessor {

	ApplicationContext applicationContext;

	@Autowired
	GirGeoServerProperties girGeoServerProperties;

	@Bean
	public ServletContextInitializer geoserverContextInitializer() {
		return servletContext -> {
			String dataDir = null;
			try {
				dataDir = applicationContext.getEnvironment().getProperty("geoair.gs.dataDir");
				if (dataDir == null)
					dataDir = FileUtil.getTmpDirPath();
				File dataDirFile = new File(dataDir);
				File mkdir = FileUtil.mkdir(dataDirFile + File.separator + "geoserverdir");
				dataDir = mkdir.getAbsolutePath();
				servletContext.setInitParameter("GEOSERVER_DATA_DIR", dataDir);
				log.info("=== GEOSERVER_DATA_DIR 已设置: " + dataDir);
			}
			catch (Exception e) {
				log.info("无法创建 GEOSERVER_DATA_DIR 目录: " + dataDir);
			}
		};
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory registry) throws BeansException {
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
	}

	/** 可选：添加 Bean 后置处理器，验证参数是否生效 */
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		// 调试：查看 GWC 相关 Bean 初始化时的参数
		if (beanName.contains("gwcXmlConfigResourceProvider") || beanName.contains("GeoSeverTileLayerCatalog")) {
			ServletContext servletContext = applicationContext.getBean(ServletContext.class);
			String dataDir = servletContext.getInitParameter("GEOSERVER_DATA_DIR");
			Gir.log.info("=== " + beanName + " 初始化时，GEOSERVER_DATA_DIR: " + dataDir);
		}
		return bean;
	}

}
