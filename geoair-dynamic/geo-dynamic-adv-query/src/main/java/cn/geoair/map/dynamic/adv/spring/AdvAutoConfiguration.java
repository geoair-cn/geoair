package cn.geoair.map.dynamic.adv.spring;



import cn.geoair.gtc.base.Gir;
import cn.geoair.map.dynamic.adv.IAdvExecutorAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/9 15:28
 * @description： spring的自动装配
 */

public class AdvAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(IAdvExecutorAdapter.class)
    IAdvExecutorAdapter advExecutorAdapter() {
       Gir.log.info("自动装配IAdvExecutorAdapter");
        return new CommonAdvExecutorAdapter();
    }

}
