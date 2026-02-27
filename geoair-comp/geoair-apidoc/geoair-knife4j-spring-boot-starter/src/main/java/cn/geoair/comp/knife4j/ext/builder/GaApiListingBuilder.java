//package com.gtc.comp.knife4j.ext.builder;
//
//import com.google.common.base.Function;
//import com.google.common.base.Optional;
//import com.google.common.collect.Sets;
//import com.gtc.base.api.annotation.GaApi;
//import org.springframework.core.annotation.Order;
//import springfox.documentation.spi.DocumentationType;
//import springfox.documentation.spi.service.ApiListingBuilderPlugin;
//import springfox.documentation.spi.service.contexts.ApiListingContext;
//
//import java.util.Set;
//
//import static com.google.common.base.Optional.fromNullable;
//import static com.google.common.base.Strings.emptyToNull;
//import static com.google.common.collect.FluentIterable.from;
//import static com.google.common.collect.Lists.newArrayList;
//import static com.google.common.collect.Sets.newTreeSet;
//import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
//import static springfox.documentation.service.Tags.emptyTags;
//import static springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER;
//
///**
// * @author ：张俊
// * @date ：Created in 2023/3/1 10:29
// * @description： 扫描gtc库中的tags注解
// */
//@Order(value = SWAGGER_PLUGIN_ORDER +1)
//public class GaApiListingBuilder implements ApiListingBuilderPlugin {
//    @Override
//    public void apply(ApiListingContext apiListingContext) {
//        Optional<? extends Class<?>> controller = apiListingContext.getResourceGroup().getControllerClass();
//        if (controller.isPresent()) {
//            Optional<GaApi> apiAnnotation = fromNullable(findAnnotation(controller.get(), GaApi.class));
//            String description = emptyToNull(apiAnnotation.transform(descriptionExtractor()).orNull());
//            apiListingContext.apiListingBuilder()
//                    .description(description);
//            Set<String> tagSet = apiAnnotation.transform(tags())
//                    .or(Sets.<String>newTreeSet());
//            if (tagSet.isEmpty()) {
//                tagSet.add(apiListingContext.getResourceGroup().getGroupName());
//            }
//            String groupName = apiListingContext.getResourceGroup().getGroupName();
//            apiListingContext.apiListingBuilder()
//                    .description(groupName)
//                    .tagNames(tagSet);
//        }
//    }
//
//    @Override
//    public boolean supports(DocumentationType delimiter) {
//        return true;
//    }
//
//    private Function<GaApi, String> descriptionExtractor() {
//        return new Function<GaApi, String>() {
//            @Override
//            public String apply(GaApi input) {
//                return input.text();
//            }
//        };
//    }
//
//    private Function<GaApi, Set<String>> tags() {
//        return new Function<GaApi, Set<String>>() {
//            @Override
//            public Set<String> apply(GaApi input) {
//                return newTreeSet(from(newArrayList(input.tags())).filter(emptyTags()).toSet());
//            }
//        };
//    }
//}
