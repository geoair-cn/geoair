package cn.geoair.comp.knife4j.ext.springdoc.builder;

import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import io.swagger.v3.oas.models.OpenAPI;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;

import java.util.*;


public class SpringDocCustomConfig {

    /**
     * 处理@GaApi注解，将其转换为OpenAPI的Tag（修复空指针异常，保留原有核心逻辑）
     */
    @Bean
    public OpenApiCustomiser gaApiTagCustomizer() {
        return openApi -> {
            Map<String, Tag> tags = new HashMap<>();

            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem -> {

                if (pathItem == null) {
                    return;
                }

                pathItem.readOperations().forEach(operation -> {
                    Object handlerMethodObj = operation.getExtensions() == null
                            ? null
                            : operation.getExtensions().get("handlerMethod");
                    // 2. 校验类型，避免强制转换失败
                    if (!(handlerMethodObj instanceof HandlerMethod)) {
                        return;
                    }
                    HandlerMethod handlerMethod = (HandlerMethod) handlerMethodObj;
                    GaApi gaApi = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), GaApi.class);
                    if (gaApi != null) {
                        String[] tagArray = gaApi.tags();
                        if (tagArray != null) {
                            operation.setTags(new ArrayList<>());
                            for (String tagString : tagArray) {
                                if (tagString == null || tagString.trim().isEmpty()) {
                                    continue;
                                }
                                Tag tag = new Tag();
                                tag.setName(tagString);
                                tag.setDescription(tagString); // 这里可以根据需要扩展更多属性
                                tags.put(tagString, tag);
                                // 将当前operation关联到该tag
                                // ========== 核心修复6：避免重复添加tag ==========
                                if (!operation.getTags().contains(tagString)) {
                                    operation.addTagsItem(tagString);
                                }
                            }
                        }
                    }

                    // 处理方法上的@GaApiAction注解
                    GaApiAction gaApiAction = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), GaApiAction.class);
                    if (gaApiAction != null) {
                        // ========== 核心修复7：校验text属性非空 ==========
                        String actionText = gaApiAction.text();
                        if (actionText != null && !actionText.trim().isEmpty()) {
                            // 设置接口名称/摘要
                            operation.setSummary(actionText);
                            // 可以同时设置description，增强文档可读性
                            if (operation.getDescription() == null || operation.getDescription().isEmpty()) {
                                operation.setDescription(actionText);
                            }
                        }
                    }
                });
            });

            if (!tags.isEmpty()) {
                openApi.setTags(new ArrayList<>(tags.values()));
            }
        };
    }

    /**
     * 处理@GaApiAction注解，自定义Operation信息
     */
    @Bean
    public OperationCustomizer gaApiActionCustomizer() {
        return (operation, handlerMethod) -> {
            // 获取方法上的@GaApiAction注解
            GaApiAction gaApiAction = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), GaApiAction.class);
            if (gaApiAction != null) {
                // 设置接口摘要
                operation.setSummary(gaApiAction.text());
                // 可选：设置operationId（确保唯一）
                operation.setOperationId(handlerMethod.getBeanType().getSimpleName() + "_" + handlerMethod.getMethod().getName());
            }

            // 获取类上的@GaApi注解
            GaApi gaApi = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), GaApi.class);
            if (gaApi != null) {
                String[] tags = gaApi.tags();
                if (tags == null || tags.length == 0) {
                    return operation;
                }
                operation.setTags(new ArrayList<>()); // 清空原来的Tag
                for (String tag : tags) {
                    operation.addTagsItem(tag);
                }

            }

            return operation;
        };
    }


}
