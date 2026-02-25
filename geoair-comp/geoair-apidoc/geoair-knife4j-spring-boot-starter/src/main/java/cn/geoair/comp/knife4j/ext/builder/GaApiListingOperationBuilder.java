package cn.geoair.comp.knife4j.ext.builder;

import cn.geoair.gtc.base.api.annotation.GaApi;
import cn.geoair.gtc.base.api.annotation.GaApiAction;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spring.web.readers.operation.DefaultTagsProvider;

import java.util.Set;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newTreeSet;
import static com.google.common.collect.Sets.union;
import static springfox.documentation.service.Tags.emptyTags;
import static springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER;

/**
 * @author ：张俊
 * @date ：Created in 2023/3/1 10:29
 * @description： 将swagger的注解中的tags与 gtc库中的tags注解合并
 */
@Order(value = SWAGGER_PLUGIN_ORDER + 1)
public class GaApiListingOperationBuilder implements OperationBuilderPlugin {

    private final DefaultTagsProvider tagsProvider;

    @Autowired
    public GaApiListingOperationBuilder(DefaultTagsProvider tagsProvider) {
        this.tagsProvider = tagsProvider;
    }

    @Override
    public void apply(OperationContext context) {
        Set<String> defaultTags = tagsProvider.tags(context);
        //  合并 swagger的注解
        Sets.SetView<String> tags = union(operationTags(context), controllerTags(context));
        //  合并gtc的注解
        Sets.SetView<String> tagsGtc = union(operationGaApiActionTags(context), controllerGaApiTags(context));
        Sets.SetView<String> uniontags = union(tags, tagsGtc);
        if (uniontags.isEmpty()) {
            context.operationBuilder().tags(defaultTags);
        } else {
            //  两个tag合并
            context.operationBuilder().tags(uniontags);
        }
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return true;
    }

    //-----------------------swagger原生功能开始-------------------------------------------

    private Set<String> controllerTags(OperationContext context) {
        Optional<Api> controllerAnnotation = context.findControllerAnnotation(Api.class);
        return controllerAnnotation.transform(tagsFromController()).or(Sets.<String>newHashSet());
    }

    private Set<String> operationTags(OperationContext context) {
        Optional<ApiOperation> annotation = context.findAnnotation(ApiOperation.class);
        return annotation.transform(tagsFromOperation()).or(Sets.<String>newHashSet());
    }

    private Function<ApiOperation, Set<String>> tagsFromOperation() {
        return new Function<ApiOperation, Set<String>>() {
            @Override
            public Set<String> apply(ApiOperation input) {
                Set<String> tags = newTreeSet();
                tags.addAll(from(newArrayList(input.tags())).filter(emptyTags()).toSet());
                return tags;
            }
        };
    }

    private Function<Api, Set<String>> tagsFromController() {
        return new Function<Api, Set<String>>() {
            @Override
            public Set<String> apply(Api input) {
                Set<String> tags = newTreeSet();
                tags.addAll(from(newArrayList(input.tags())).filter(emptyTags()).toSet());
                return tags;
            }
        };
    }
//-----------------------swagger原生功能结束-------------------------------------------

    //-----------------------gtc拓展功能开始-------------------------------------------

    private Set<String> controllerGaApiTags(OperationContext context) {
        Optional<GaApi> controllerAnnotation = context.findControllerAnnotation(GaApi.class);
        return controllerAnnotation.transform(tagsFromGaApiController()).or(Sets.<String>newHashSet());
    }

    private Set<String> operationGaApiActionTags(OperationContext context) {
        Optional<GaApiAction> annotation = context.findAnnotation(GaApiAction.class);
        return annotation.transform(tagsGaApiActionFromOperation()).or(Sets.<String>newHashSet());
    }

    private Function<GaApiAction, Set<String>> tagsGaApiActionFromOperation() {
        return new Function<GaApiAction, Set<String>>() {
            @Override
            public Set<String> apply(GaApiAction input) {
                Set<String> tags = newTreeSet();
                tags.addAll(from(newArrayList(input.tags())).filter(emptyTags()).toSet());
                return tags;
            }
        };
    }

    private Function<GaApi, Set<String>> tagsFromGaApiController() {
        return new Function<GaApi, Set<String>>() {
            @Override
            public Set<String> apply(GaApi input) {
                Set<String> tags = newTreeSet();
                tags.addAll(from(newArrayList(input.tags())).filter(emptyTags()).toSet());
                return tags;
            }
        };
    }
//-----------------------gtc拓展功能结束-------------------------------------------

}
