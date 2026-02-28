package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.gtc.base.Gir;
import cn.geoair.map.dynamic.adv.IAdvExecutorAdapter;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 15:28 @description： spring的自动装配
 */

public class AdvAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(GirSpringAdvExecutor.class) // 仅当容器中无IAdvExecutor时才创建
	public GirSpringAdvExecutor springAdvExecutor() {
		Gir.log.info("开始自动装配GirSpringAdvExecutor，检测数据源类型...");
		IAdvExecutor advExecutorByDataSource = AdvExecutorFactory.getAdvExecutorByDataSource();
		return new GirSpringAdvExecutor(advExecutorByDataSource);
	}

	/**
	 * 自动装配执行器适配器
	 * @param advExecutor 容器中已装配的执行器（上面的bean）
	 * @return 通用适配器
	 */
	@Bean
	@ConditionalOnMissingBean(IAdvExecutorAdapter.class)
	public IAdvExecutorAdapter advExecutorAdapter(IAdvExecutor advExecutor) {
		Gir.log.info("自动装配IAdvExecutorAdapter，适配执行器类型：{}", advExecutor.getClass().getSimpleName());
		CommonAdvExecutorAdapter adapter = new CommonAdvExecutorAdapter();
		return adapter;
	}

}
