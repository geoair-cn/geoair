package cn.geoair.map.dynamic.adv.spring;

import cn.geoair.base.Gir;
import cn.geoair.comp.dynamic.ds.dswrapper.AdvDataSourceWrapper;
import cn.geoair.comp.dynamic.ds.dswrapper.DataSourceWrapperRegistry;
import cn.geoair.map.dynamic.adv.IAdvExecutorAdapter;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 15:28 @description： spring的自动装配
 */
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
@EnableConfigurationProperties({DataSourceProperties.class})
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@Order(100)
@Configuration
public class AdvAutoConfiguration {
    public AdvAutoConfiguration() {
        Gir.log.info("AdvAutoConfiguration initialized");
    }

    @Bean
    @ConditionalOnMissingBean(IAdvExecutor.class)

    public IAdvExecutor springAdvExecutor(ObjectProvider<DataSource> dataSourceProvider) {
        DataSource dataSource = dataSourceProvider.getIfAvailable();

        if (dataSource == null) {
            Gir.log.warn("DataSource Bean 不存在，跳过 IAdvExecutor 创建");
            return null;
        }
        Gir.log.info("开始自动装配springAdvExecutor，检测数据源类型...");
        Optional<AdvDataSourceWrapper> wrapper = DataSourceWrapperRegistry.getWrapper(dataSource);
        String dataSourceName = null;
        if (wrapper.isPresent()) {
            dataSourceName = wrapper.get().getJdbcUrl();
        }
        if (StrUtil.isEmpty(dataSourceName)) {
            dataSourceName = "master_by_spring_" + IdUtil.simpleUUID();
        }
        IAdvExecutor advExecutorByDataSource =
                AdvExecutorFactory.getAdvExecutorByDataSource(dataSource, dataSourceName);
        GirSpringAdvExecutor girSpringAdvExecutor = new GirSpringAdvExecutor(advExecutorByDataSource);
        Gir.log.info(
                "自动装配SpringIAdvExecutor，IAdvExecutor的数据库类型：{}",
                advExecutorByDataSource.getClass().getSimpleName());
        return girSpringAdvExecutor;
    }

    /**
     * 自动装配执行器适配器（依赖上面的IAdvExecutor Bean）   参数注入IAdvExecutor，确保依赖顺序
     */
    @Bean
    @ConditionalOnMissingBean(IAdvExecutorAdapter.class)
    @ConditionalOnBean(IAdvExecutor.class)
    public IAdvExecutorAdapter advExecutorAdapter(
            IAdvExecutor advExecutor) { // 注入IAdvExecutor，自动保证顺序
        Gir.log.info("自动装配IAdvExecutorAdapter  ");
        CommonAdvExecutorAdapter adapter = new CommonAdvExecutorAdapter();
        return adapter;
    }
}
