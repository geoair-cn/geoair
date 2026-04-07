package cn.geoair.comp.knife4j.ext.springdoc.auto;

import cn.geoair.comp.knife4j.ext.core.config.IGirOpenApiConfig;
import cn.geoair.comp.knife4j.ext.core.model.ApiModelInfo;
import cn.geoair.comp.knife4j.ext.core.model.DocketInfo;
import cn.hutool.core.util.IdUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 🔥 终极版本
 * 支持：无限个 IGirOpenApiConfig 实现类
 * 每个配置：独立分组 + 独立文档标题/描述/版本
 * 无yml + 纯代码 + SpringDoc 2.6.5 + Spring 6.1 兼容
 */
@Configuration
public class SpringDocMultiConfigProcessor implements ApplicationContextAware {

    private ApplicationContext ctx;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.ctx = applicationContext;
        processAllOpenApiConfigs();
    }

    /**
     * 核心：处理所有 IGirOpenApiConfig 实现类
     * 每个都会生成自己的分组 + 自己的文档信息
     */
    private void processAllOpenApiConfigs() {
        Map<String, IGirOpenApiConfig> configMap = ctx.getBeansOfType(IGirOpenApiConfig.class);
        if (configMap.isEmpty()) return;

        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) ctx;

        for (IGirOpenApiConfig config : configMap.values()) {
            // 1. 执行你的加载逻辑
            config.doLoading();

            // 2. 获取当前配置的分组 + 文档信息
            List<DocketInfo> docketInfos = config.getDocketInfos();
            ApiModelInfo modelInfo = config.getApiModelInfo();

            // 3. 注册分组
            if (!CollectionUtils.isEmpty(docketInfos)) {
                for (DocketInfo info : docketInfos) {
                    String groupName = info.getGroupName();
                    String[] packages = info.getBasePackages().toArray(new String[0]);

                    BeanDefinitionBuilder builder = BeanDefinitionBuilder
                            .genericBeanDefinition(GroupedOpenApi.class, () ->
                                    GroupedOpenApi.builder()
                                            .group(groupName)
                                            .displayName(groupName)
                                            .packagesToScan(packages)
                                            .pathsToMatch("/**")
                                            .build()
                            );

                    registry.registerBeanDefinition("groupApi_" + groupName, builder.getBeanDefinition());
                }
            }

            // 4. 注册当前配置的 OpenAPI 信息（用你自己的 ApiModelInfo）
            if (modelInfo != null) {
                BeanDefinitionBuilder builder = BeanDefinitionBuilder
                        .genericBeanDefinition(OpenAPI.class, () ->
                                new OpenAPI()
                                        .info(new Info()
                                                .title(modelInfo.getTitle())
                                                .description(modelInfo.getDescription())
                                                .version(modelInfo.getVersion())
                                                .contact(new Contact().name(modelInfo.getAuthor()))
                                        )
                        );

                registry.registerBeanDefinition("openApi_" + IdUtil.getSnowflakeNextIdStr(), builder.getBeanDefinition());
            }

            config.loadEnd();
        }
    }
}
