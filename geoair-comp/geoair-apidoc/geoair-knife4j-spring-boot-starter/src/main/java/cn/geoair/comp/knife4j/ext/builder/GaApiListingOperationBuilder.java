package cn.geoair.comp.knife4j.ext.builder;

import cn.geoair.base.Gir;
import cn.geoair.base.api.annotation.GaApi;
import cn.geoair.base.api.annotation.GaApiAction;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.annotation.Order;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static com.google.common.collect.Sets.union;
import static springfox.documentation.service.Tags.emptyTags;
import static springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER;

/**
 * 适配Swagger 3.0.0 (springfox 3.x)：合并Swagger原生注解和GTC自定义注解的tags
 *
 */

@Order(value = SWAGGER_PLUGIN_ORDER + 1)
public class GaApiListingOperationBuilder implements OperationBuilderPlugin {

	public GaApiListingOperationBuilder() {
		Gir.log.info("Swagger 3.0.0 (springfox 3.x)兼容启动...");
	}

	@Override
	public void apply(OperationContext context) {
		// 1. 获取默认标签（控制器类名首字母小写）
		Set<String> defaultTags = getDefaultTags(context);

		Set<String> swagger3Tags = union(operationTags(context), controllerTags(context));

		Set<String> swagger2Tags = union(controllerApis(context), operationApiOperation(context));

		Set<String> gtcTags = union(operationGaApiActionTags(context), controllerGaApiTags(context));
		Set<String> swaggeTags = union(swagger3Tags, swagger2Tags);
		Set<String> allTags = union(swaggeTags, gtcTags);

		// 3. 设置最终tags（无自定义tags则用默认）
		context.operationBuilder().tags(allTags.isEmpty() ? defaultTags : allTags);
	}

	@Override
	public boolean supports(DocumentationType documentationType) {
		// 只支持OpenAPI 3.0（Swagger 3）
		return DocumentationType.OAS_30.equals(documentationType);
	}

	// ----------------------- 核心：获取默认标签（修复Springfox 3.x API） -----------------------
	private Set<String> getDefaultTags(OperationContext context) {
		return copyOf(newHashSet(context.getGroupName()));
	}

	// ----------------------- Swagger 2.x 原生注解（@Api/@ApiOperation）处理
	// -----------------------
	private Set<String> controllerApis(OperationContext context) {
		Optional<Api> tagAnnotation = context.findControllerAnnotation(Api.class);
		return tagAnnotation.map(this::extractApiFromController).orElse(newHashSet());
	}

	private Set<String> operationApiOperation(OperationContext context) {
		Optional<ApiOperation> operationAnnotation = context.findAnnotation(ApiOperation.class);
		return operationAnnotation.map(this::extractTagsFromApiOperation).orElse(newHashSet());
	}

	// 提取@Tag注解的标签（name属性）
	private Set<String> extractApiFromController(Api api) {
		Set<String> tags = newTreeSet();
		if (api.tags() != null && !(api.tags().length == 0)) {
			for (String tag : api.tags()) {
				tags.add(tag);
			}
		}
		return tags;
	}

	// 提取@Operation注解的tags（过滤空标签）
	private Set<String> extractTagsFromApiOperation(ApiOperation operation) {
		Set<String> tags = newTreeSet();
		if (operation.tags() != null && operation.tags().length > 0) {
			tags.addAll(Arrays.stream(operation.tags()).filter(tag -> tag != null && !emptyTags().test(tag))
					.collect(Collectors.toSet()));
		}
		return tags;
	}

	// ----------------------- Swagger 3.x 原生注解（@Tag/@Operation）处理 -----------------------
	private Set<String> controllerTags(OperationContext context) {
		Optional<Tag> tagAnnotation = context.findControllerAnnotation(Tag.class);
		return tagAnnotation.map(this::extractTagFromController).orElse(newHashSet());
	}

	private Set<String> operationTags(OperationContext context) {
		Optional<Operation> operationAnnotation = context.findAnnotation(Operation.class);
		return operationAnnotation.map(this::extractTagsFromOperation).orElse(newHashSet());
	}

	// 提取@Tag注解的标签（name属性）
	private Set<String> extractTagFromController(Tag tag) {
		Set<String> tags = newTreeSet();
		if (tag.name() != null && !tag.name().isEmpty()) {
			tags.add(tag.name());
		}
		return tags;
	}

	// 提取@Operation注解的tags（过滤空标签）
	private Set<String> extractTagsFromOperation(Operation operation) {
		Set<String> tags = newTreeSet();
		if (operation.tags() != null && operation.tags().length > 0) {
			tags.addAll(Arrays.stream(operation.tags()).filter(tag -> tag != null && !emptyTags().test(tag))
					.collect(Collectors.toSet()));
		}
		return tags;
	}

	// ----------------------- GTC自定义注解（@GaApi/@GaApiAction）处理 -----------------------
	private Set<String> controllerGaApiTags(OperationContext context) {
		Optional<GaApi> gaApiAnnotation = context.findControllerAnnotation(GaApi.class);
		return gaApiAnnotation.map(this::extractTagsFromGaApi).orElse(newHashSet());
	}

	private Set<String> operationGaApiActionTags(OperationContext context) {
		Optional<GaApiAction> gaApiActionAnnotation = context.findAnnotation(GaApiAction.class);
		return gaApiActionAnnotation.map(this::extractTagsFromGaApiAction).orElse(newHashSet());
	}

	// 提取@GaApi注解的tags（过滤空标签）
	private Set<String> extractTagsFromGaApi(GaApi gaApi) {
		Set<String> tags = newTreeSet();
		if (gaApi.tags() != null && gaApi.tags().length > 0) {
			tags.addAll(Arrays.stream(gaApi.tags()).filter(tag -> tag != null && !emptyTags().test(tag))
					.collect(Collectors.toSet()));
		}
		return tags;
	}

	// 提取@GaApiAction注解的tags（过滤空标签）
	private Set<String> extractTagsFromGaApiAction(GaApiAction gaApiAction) {
		Set<String> tags = newTreeSet();
		if (gaApiAction.tags() != null && gaApiAction.tags().length > 0) {
			tags.addAll(Arrays.stream(gaApiAction.tags()).filter(tag -> tag != null && !emptyTags().test(tag))
					.collect(Collectors.toSet()));
		}
		return tags;
	}

}
