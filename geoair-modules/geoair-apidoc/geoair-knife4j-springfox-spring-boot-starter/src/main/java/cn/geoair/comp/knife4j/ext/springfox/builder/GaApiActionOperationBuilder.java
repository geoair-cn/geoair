package cn.geoair.comp.knife4j.ext.springfox.builder;

import static springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import io.swagger.annotations.ApiOperation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;

/**
 * @author ：张俊
 * @date ：Created in 2023/3/1 10:16 @description： 使用 GaApi 替换 ApiOperation
 */
@Component
@Order(value = SWAGGER_PLUGIN_ORDER)
public class GaApiActionOperationBuilder implements OperationBuilderPlugin {

    @Override
    public void apply(OperationContext context) {
        List<ApiOperation> list = context.findAllAnnotations(ApiOperation.class);
        if (list.isEmpty()) {
            List<GaApiAction> explainList = context.findAllAnnotations(GaApiAction.class);
            if (!explainList.isEmpty()) {
                Optional<GaApi> controllerAnnotation =
                        context.findControllerAnnotation(GaApi.class);
                GaApiAction explain = explainList.get(0);
                context.operationBuilder().summary(explain.text()); // 替换默认值
                if (controllerAnnotation.isPresent()) {
                    GaApi gaApi = controllerAnnotation.get();
                    context.operationBuilder()
                            .tags(Arrays.asList(gaApi.tags()).stream().collect(Collectors.toSet()));
                }
            }
        }
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }
}
