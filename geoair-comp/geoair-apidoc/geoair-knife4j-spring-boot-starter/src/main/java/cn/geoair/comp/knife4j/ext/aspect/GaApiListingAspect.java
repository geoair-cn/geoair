package cn.geoair.comp.knife4j.ext.aspect;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.util.GutilObject;
import cn.geoair.base.util.GutilStr;
import io.swagger.annotations.Api;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.AnnotationUtils;
import springfox.documentation.spi.service.contexts.ApiListingContext;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Sets.newTreeSet;
import static com.google.common.collect.Sets.union;
import static springfox.documentation.service.Tags.emptyTags;

/**
 * @author ：张俊
 * @date ：Created in 2022/12/29 16:33 @description： 扫描gtc库中的tags注解 修复：移除Guava
 * Optional/Function依赖，改用JDK原生API，解决类型冲突
 */
@Aspect
public class GaApiListingAspect {

    @Around("execution(* springfox.documentation.swagger.web.SwaggerApiListingReader.apply(..))" )
    public Object typeResolverAspect(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
//        Object proceed = joinPoint.proceed(args);
        if (args[0] instanceof ApiListingContext) {
            ApiListingContext apiListingContext = (ApiListingContext) args[0];
            // 修复1：统一使用JDK原生Optional
            Optional<? extends Class<?>> controller = apiListingContext.getResourceGroup().getControllerClass();
            if (controller.isPresent()) {
                // ========== swagger 原生@Api注解处理 ==========
                // 修复：改用Spring AnnotationUtils + JDK Optional
                Api apiAnnotation = AnnotationUtils.findAnnotation(controller.get(), Api.class);
                String description = apiAnnotation != null ? apiAnnotation.description() : null;
                // 过滤空描述（替代Guava的emptyToNull）
                description = (description == null || description.trim().isEmpty()) ? null : description;

                Set<String> tagSet = apiAnnotation != null ? extractApiTags(apiAnnotation) : newTreeSet();

                // ========== GTC自定义@GaApi注解处理 ==========
                GaApi gaApiAnnotation = AnnotationUtils.findAnnotation(controller.get(), GaApi.class);
                String descriptiongtc = gaApiAnnotation != null ? gaApiAnnotation.text() : null;
                descriptiongtc = (descriptiongtc == null || descriptiongtc.trim().isEmpty()) ? null : descriptiongtc;

                Set<String> taggtcSet = gaApiAnnotation != null ? extractGaApiTags(gaApiAnnotation) : newTreeSet();

                if (GutilStr.isBlank(descriptiongtc) && GutilObject.isNotEmpty(taggtcSet)) {
                    String next = taggtcSet.iterator().next();
                    descriptiongtc = next;
                }

                // ========== 合并tags + 设置描述 ==========
                Set<String> uniontags = union(taggtcSet, tagSet).immutableCopy();
                if (uniontags.isEmpty()) {
                    tagSet.add(apiListingContext.getResourceGroup().getGroupName());
                } else {
                    apiListingContext.apiListingBuilder()
                            // 描述优先使用GTC注解，保持原有逻辑
                            .description(GutilStr.isBlank(descriptiongtc) ? description : descriptiongtc)
                            .tagNames(uniontags);
                }
            }
        }
        return null;
    }


    // ----------------------- 提取注解信息的工具方法（替换原Guava Function） -----------------------

    /**
     * 提取@GaApi注解的tags（改用JDK Stream过滤空标签）
     */
    private Set<String> extractGaApiTags(GaApi gaApi) {
        if (gaApi.tags() == null || gaApi.tags().length == 0) {
            return newTreeSet();
        }
        return Arrays.stream(gaApi.tags()).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * 提取@Api注解的tags（改用JDK Stream过滤空标签）
     */
    private Set<String> extractApiTags(Api api) {
        if (api.tags() == null || api.tags().length == 0) {
            return newTreeSet();
        }
        return Arrays.stream(api.tags()).filter(tag -> tag != null && !emptyTags().test(tag))
                .collect(Collectors.toCollection(TreeSet::new));
    }

}
