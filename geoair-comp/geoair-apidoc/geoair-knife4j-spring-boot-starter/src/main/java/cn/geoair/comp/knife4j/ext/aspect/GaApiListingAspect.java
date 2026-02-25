package cn.geoair.comp.knife4j.ext.aspect;

import cn.geoair.gtc.base.api.annotation.GaApi;
import cn.geoair.gtc.base.util.GutilStr;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import io.swagger.annotations.Api;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import springfox.documentation.spi.service.contexts.ApiListingContext;

import java.util.Set;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newTreeSet;
import static com.google.common.collect.Sets.union;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static springfox.documentation.service.Tags.emptyTags;

/**
 * @author ：张俊
 * @date ：Created in 2022/12/29 16:33
 * @description：  扫描gtc库中的tags注解
 */
@Aspect
public class GaApiListingAspect {


    @Around("execution(* springfox.documentation.swagger.web.SwaggerApiListingReader.apply(..))")
    public Object typeResolverAspect(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        if (args[0] instanceof ApiListingContext) {
            ApiListingContext apiListingContext = (ApiListingContext) args[0];
            Optional<? extends Class<?>> controller = apiListingContext.getResourceGroup().getControllerClass();

            if (controller.isPresent()) {

                //  swagger 原生功能开始

                Optional<Api> apiAnnotation = fromNullable(findAnnotation(controller.get(), Api.class));
                String description = emptyToNull(apiAnnotation.transform(descriptionExtractor()).orNull());

                Set<String> tagSet = apiAnnotation.transform(tags()).or(Sets.<String>newTreeSet());

                // swagger原生功能结束
                // gtcApi拓展功能开始
                Optional<GaApi> apiAnnotationgtc = fromNullable(findAnnotation(controller.get(), GaApi.class));
                String descriptiongtc = emptyToNull(apiAnnotationgtc.transform(descriptionExtractor1()).orNull());
                Set<String> taggtcSet = apiAnnotationgtc.transform(tags1())
                        .or(Sets.<String>newTreeSet());
                // gtcApi拓展功能结束
                // 两者的tag合并
                Sets.SetView<String> uniontags = union(taggtcSet, tagSet);
                if (uniontags.isEmpty()) {
                    tagSet.add(apiListingContext.getResourceGroup().getGroupName());
                } else {
                    apiListingContext.apiListingBuilder()
                            .description(GutilStr.isBlank(descriptiongtc) ? description : descriptiongtc)  // ，描述信息优先使用gtc的注解
                            .tagNames(uniontags);
                }
            }

        }
        return null;
    }

    public void apply1(ApiListingContext apiListingContext) {
        Optional<? extends Class<?>> controller = apiListingContext.getResourceGroup().getControllerClass();
        if (controller.isPresent()) {
            Optional<GaApi> apiAnnotation = fromNullable(findAnnotation(controller.get(), GaApi.class));
            String description = emptyToNull(apiAnnotation.transform(descriptionExtractor1()).orNull());

            Set<String> tagSet = apiAnnotation.transform(tags1())
                    .or(Sets.<String>newTreeSet());
            if (tagSet.isEmpty()) {

            }
            apiListingContext.apiListingBuilder()
                    .description(description)
                    .tagNames(tagSet);
        }
    }

    private Function<GaApi, String> descriptionExtractor1() {
        return new Function<GaApi, String>() {
            @Override
            public String apply(GaApi input) {
                return input.text();
            }
        };
    }

    private Function<GaApi, Set<String>> tags1() {
        return new Function<GaApi, Set<String>>() {
            @Override
            public Set<String> apply(GaApi input) {
                return newTreeSet(from(newArrayList(input.tags())).filter(emptyTags()).toSet());
            }
        };
    }

    private Function<Api, String> descriptionExtractor() {
        return new Function<Api, String>() {
            @Override
            public String apply(Api input) {
                return input.description();
            }
        };
    }

    private Function<Api, Set<String>> tags() {
        return new Function<Api, Set<String>>() {
            @Override
            public Set<String> apply(Api input) {
                return newTreeSet(from(newArrayList(input.tags())).filter(emptyTags()).toSet());
            }
        };
    }
}

