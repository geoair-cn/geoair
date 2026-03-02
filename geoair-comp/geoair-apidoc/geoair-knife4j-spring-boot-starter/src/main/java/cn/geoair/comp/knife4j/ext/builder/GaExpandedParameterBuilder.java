package cn.geoair.comp.knife4j.ext.builder;

import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.common.GemNull;
import cn.geoair.base.data.model.annotation.GaModelField;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import springfox.documentation.builders.ExampleBuilder;
import springfox.documentation.service.AllowableListValues;
import springfox.documentation.service.AllowableValues;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.EnumTypeDeterminer;
import springfox.documentation.spi.service.ExpandedParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterExpansionContext;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.swagger.common.SwaggerPluginSupport;
import springfox.documentation.swagger.schema.ApiModelProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;
import static springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER;
import static springfox.documentation.swagger.readers.parameter.Examples.examples;

/**
 * @author ：张逢吉
 * @date ：Created in 20:05 @description： 展开模型
 */
@Order(value = SWAGGER_PLUGIN_ORDER + 1)
public class GaExpandedParameterBuilder implements ExpandedParameterBuilderPlugin {

	private final DescriptionResolver descriptions;

	private final EnumTypeDeterminer enumTypeDeterminer;

	@Autowired
	public GaExpandedParameterBuilder(DescriptionResolver descriptions, EnumTypeDeterminer enumTypeDeterminer) {
		this.descriptions = descriptions;
		this.enumTypeDeterminer = enumTypeDeterminer;
	}

	@Override
	public void apply(ParameterExpansionContext context) {
		Optional<GaModelField> apiModelPropertyOptional = context.findAnnotation(GaModelField.class);
		apiModelPropertyOptional.ifPresent(apiModelProperty -> fromApiModelProperty(context, apiModelProperty));
		Optional<ApiParam> apiParamOptional = context.findAnnotation(ApiParam.class);
		apiParamOptional.ifPresent(apiParam -> fromApiParam(context, apiParam));
	}

	@Override
	public boolean supports(DocumentationType delimiter) {
		return SwaggerPluginSupport.pluginDoesApply(delimiter);
	}

	private void fromApiParam(ParameterExpansionContext context, ApiParam apiParam) {
		String allowableProperty = ofNullable(apiParam.allowableValues())
				.filter(((Predicate<String>) String::isEmpty).negate()).orElse(null);
		AllowableValues allowable = allowableValues(ofNullable(allowableProperty),
				context.getFieldType().getErasedType());

		maybeSetParameterName(context, apiParam.name());
		context.getParameterBuilder().description(descriptions.resolve(apiParam.value()))
				.defaultValue(apiParam.defaultValue()).required(apiParam.required())
				.allowMultiple(apiParam.allowMultiple()).allowableValues(allowable).parameterAccess(apiParam.access())
				.hidden(apiParam.hidden()).scalarExample(apiParam.example())
				.complexExamples(examples(apiParam.examples())).order(SWAGGER_PLUGIN_ORDER).build();

		context.getRequestParameterBuilder().description(descriptions.resolve(apiParam.value()))
				.required(apiParam.required()).hidden(apiParam.hidden())
				.example(new ExampleBuilder().value(apiParam.example()).build()).precedence(SWAGGER_PLUGIN_ORDER)
				.query(q -> q.enumerationFacet(e -> e.allowedValues(allowable)));
	}

	private void fromApiModelProperty(ParameterExpansionContext context, GaModelField apiModelProperty) {
		List<String> enumValues = new ArrayList<>();
		if (apiModelProperty.em() != GemNull.class) {
			Class<? extends Enum<?>> em = apiModelProperty.em();
			Object[] objects = em.getEnumConstants();
			try {
				for (Object obj : objects) {
					if (obj instanceof GiVisualValuable) {
						GiVisualValuable obj1 = (GiVisualValuable) obj;
						// 3.调用对应方法，得到枚举常量中字段的值
						String display = obj1.display();
						Object value = obj1.value();
						enumValues.add("{name: " + display + ";code: " + value + "}");
					}
				}

			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		AllowableValues allowable = allowableValues(Optional.empty(), context.getFieldType().getErasedType());

		// maybeSetParameterName(context, apiModelProperty.text());
		context.getParameterBuilder().description(apiModelProperty.text()).allowableValues(allowable)
				.order(SWAGGER_PLUGIN_ORDER).build();

		context.getRequestParameterBuilder().description(apiModelProperty.text()).precedence(SWAGGER_PLUGIN_ORDER)
				.query(q -> q.enumerationFacet(e -> e.allowedValues(allowable)));
	}

	private void maybeSetParameterName(ParameterExpansionContext context, String parameterName) {
		if (!isEmpty(parameterName)) {
			context.getParameterBuilder().name(parameterName);
			context.getRequestParameterBuilder().name(parameterName);
		}
	}

	private AllowableValues allowableValues(final Optional<String> optionalAllowable, Class<?> fieldType) {

		AllowableValues allowable = null;
		if (enumTypeDeterminer.isEnum(fieldType)) {
			allowable = new AllowableListValues(getEnumValues(fieldType), "LIST");
		}
		else if (optionalAllowable.isPresent()) {
			allowable = ApiModelProperties.allowableValueFromString(optionalAllowable.get());
		}
		return allowable;
	}

	private List<String> getEnumValues(final Class<?> subject) {
		return Stream.of(subject.getEnumConstants()).map((Function<Object, String>) Object::toString).collect(toList());
	}

}
