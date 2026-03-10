package cn.geoair.map.dynamic.dbservice.starter.run;

import liquibase.integration.spring.SpringLiquibase;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

/**
 * @author ：张逢吉
 * @date ：Created in 11:38 @description： 自动装配模块
 */
@Configuration
@ConditionalOnClass(SpringLiquibase.class)
@AutoConfigureAfter(LiquibaseAutoConfiguration.class)
public class AutoGirDbServiceLiqiuBaseConfiguration {

	@Autowired(required = false)
	private List<SpringLiquibase> springLiquibases;

	@Autowired
	private DataSource dataSource;

	@Bean
	@ConditionalOnMissingBean(SpringLiquibase.class)
	public SpringLiquibase defaultLiquibase() {
		SpringLiquibase liquibase = new SpringLiquibase();
		liquibase.setDataSource(dataSource);
		liquibase.setChangeLog("classpath:changelog/geoair-dbservice-changelog.xml");
		liquibase.setContexts("default");
		liquibase.setShouldRun(true);
		return liquibase;
	}

	@PostConstruct
	public void appendModuleChangelog() throws Exception {
		if (springLiquibases == null || springLiquibases.isEmpty()) {
			defaultLiquibase();
			return;
		}
		for (SpringLiquibase liquibase : springLiquibases) {
			String originalChangeLog = Optional.ofNullable(liquibase.getChangeLog()).orElse("");
			String newChangeLog = originalChangeLog + (originalChangeLog.isEmpty() ? "" : ",")
					+ "classpath:changelog/geoair-dbservice-changelog.xml";
			liquibase.setChangeLog(newChangeLog);
			liquibase.setShouldRun(true);
		}
	}

}
