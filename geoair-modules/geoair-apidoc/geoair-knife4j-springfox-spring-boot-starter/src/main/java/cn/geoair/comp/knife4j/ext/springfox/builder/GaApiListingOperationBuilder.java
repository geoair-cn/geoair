package cn.geoair.comp.knife4j.ext.springfox.builder;

import static springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER;

import java.util.*;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import cn.geoair.base.Gir;
import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import cn.geoair.base.util.GutilObject;

import cn.hutool.core.collection.CollUtil;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;

/**
 * 适配Swagger 3.0.0 (springfox 3.x)：合并Swagger原生注解和GTC自定义注解的tags
 */

@Component
@Order(value = SWAGGER_PLUGIN_ORDER)
public class GaApiListingOperationBuilder implements OperationBuilderPlugin {

	public GaApiListingOperationBuilder() {
		Gir.log.info("Swagger 3.0.0 (springfox 3.x)兼容启动...");
	}

	@Override
	public void apply(OperationContext context) {

		// 1. 获取默认标签（控制器类名首字母小写）
		Set<String> defaultTags = getDefaultTags(context);

		Collection<String> swagger3Tags = CollUtil.union(operationTags(context), controllerTags(context));

		Collection<String> swagger2Tags = CollUtil.union(controllerApis(context), operationApiOperation(context));

		Collection<String> gtcTags = CollUtil.union(operationGaApiActionTags(context), controllerGaApiTags(context));
		Collection<String> swaggeTags = CollUtil.union(swagger3Tags, swagger2Tags);
		Collection<String> allTags = CollUtil.union(swaggeTags, gtcTags);

		// 3. 设置最终tags（无自定义tags则用默认）
		context.operationBuilder().tags(GutilObject.isEmpty(allTags) ? defaultTags : new TreeSet<>(allTags));
	}

	@Override
	public boolean supports(DocumentationType documentationType) {
		// 只支持OpenAPI 3.0（Swagger 3）
		return true;
	}

	// ----------------------- 核心：获取默认标签（修复Springfox 3.x API） -----------------------
	private Set<String> getDefaultTags(OperationContext context) {
		Set<String> objects = new HashSet<>();
		objects.add(context.getGroupName());
		return objects;
	}

	// ----------------------- Swagger 2.x 原生注解（@Api/@ApiOperation）处理
	// -----------------------
	private Set<String> controllerApis(OperationContext context) {
		Optional<Api> tagAnnotation = context.findControllerAnnotation(Api.class);
		return tagAnnotation.map(this::extractApiFromController).orElse(new HashSet<>());
	}

	private Set<String> operationApiOperation(OperationContext context) {
		Optional<ApiOperation> operationAnnotation = context.findAnnotation(ApiOperation.class);
		return operationAnnotation.map(this::extractTagsFromApiOperation).orElse(new HashSet<>());
	}

	// 提取@Tag注解的标签（name属性）
	private Set<String> extractApiFromController(Api api) {
		Set<String> tags = new TreeSet<>();
		if (api.tags() != null && !(api.tags().length == 0)) {
			for (String tag : api.tags()) {
				tags.add(tag);
			}
		}
		return tags;
	}

	// 提取@Operation注解的tags（过滤空标签）
	private Set<String> extractTagsFromApiOperation(ApiOperation operation) {
		Set<String> tags = new TreeSet<>();
		if (operation.tags() != null) {
			for (String tag : operation.tags()) {
				tags.add(tag);
			}
		}
		return tags;
	}

	// ----------------------- Swagger 3.x 原生注解（@Tag/@Operation）处理 -----------------------
	private Collection<String> controllerTags(OperationContext context) {
		Optional<Tag> tagAnnotation = context.findControllerAnnotation(Tag.class);
		return tagAnnotation.map(this::extractTagFromController).orElse(new HashSet<>());
	}

	private Collection<String> operationTags(OperationContext context) {
		Optional<Operation> operationAnnotation = context.findAnnotation(Operation.class);
		return operationAnnotation.map(this::extractTagsFromOperation).orElse(new HashSet<>());
	}

	// 提取@Tag注解的标签（name属性）
	private Set<String> extractTagFromController(Tag tag) {
		Set<String> tags = new TreeSet<>();
		if (tag.name() != null && !tag.name().isEmpty()) {
			tags.add(tag.name());
		}
		return tags;
	}

	// 提取@Operation注解的tags（过滤空标签）
	private Set<String> extractTagsFromOperation(Operation operation) {
		Set<String> tags = new TreeSet<>();
		if (operation.tags() != null) {
			for (String tag : operation.tags()) {
				if (tag == null || tag.isEmpty()) {
					continue;
				}
				tags.add(tag);
			}
		}
		return tags;
	}

	// ----------------------- GTC自定义注解（@GaApi/@GaApiAction）处理 -----------------------
	private Set<String> controllerGaApiTags(OperationContext context) {
		Optional<GaApi> gaApiAnnotation = context.findControllerAnnotation(GaApi.class);
		if (gaApiAnnotation.isPresent()) {
			GaApi gaApiAction = gaApiAnnotation.get();
			context.operationBuilder().summary(GutilObject.isEmpty(gaApiAction.text()) ? null : gaApiAction.text());
		}
		return gaApiAnnotation.map(this::extractTagsFromGaApi).orElse(new HashSet<>());
	}

	private Set<String> operationGaApiActionTags(OperationContext context) {
		Optional<GaApiAction> gaApiActionAnnotation = context.findAnnotation(GaApiAction.class);
		if (gaApiActionAnnotation.isPresent()) {
			GaApiAction gaApiAction = gaApiActionAnnotation.get();
			context.operationBuilder().summary(GutilObject.isEmpty(gaApiAction.text()) ? null : gaApiAction.text());
		}
		return gaApiActionAnnotation.map(this::extractTagsFromGaApiAction).orElse(new HashSet<>());
	}

	// 提取@GaApi注解的tags（过滤空标签）
	private Set<String> extractTagsFromGaApi(GaApi gaApi) {
		Set<String> tags = new TreeSet<>();
		if (gaApi.tags() != null) {
			for (String tag : gaApi.tags()) {
				if (tag == null || tag.isEmpty()) {
					continue;
				}
				tags.add(tag);
			}
		}
		return tags;
	}

	// 提取@GaApiAction注解的tags（过滤空标签）
	private Set<String> extractTagsFromGaApiAction(GaApiAction gaApiAction) {
		Set<String> tags = new TreeSet<>();
		if (gaApiAction.tags() != null) {
			for (String tag : gaApiAction.tags()) {
				if (tag == null || tag.isEmpty()) {
					continue;
				}
				tags.add(tag);
			}
		}
		return tags;
	}

}
