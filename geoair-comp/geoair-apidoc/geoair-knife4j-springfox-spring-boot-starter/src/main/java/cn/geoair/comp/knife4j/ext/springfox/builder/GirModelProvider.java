package cn.geoair.comp.knife4j.ext.springfox.builder;

import cn.geoair.base.data.model.annotation.GaModel;
import com.fasterxml.classmate.TypeResolver;

import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.schema.property.ModelSpecificationFactory;
import springfox.documentation.spi.schema.EnumTypeDeterminer;
import springfox.documentation.spi.schema.contexts.ModelContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;
import springfox.documentation.swagger.schema.ApiModelBuilder;

/**
 * @author ：张俊
 * @date ：Created in 2022/5/13 14:43 @description： GirModel 替换 apimodel注解
 */
@Component
@Primary
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER - 1)
public class GirModelProvider extends ApiModelBuilder {

	private final TypeResolver typeResolver;

	private final TypeNameExtractor typeNameExtractor;

	public GirModelProvider(TypeResolver typeResolver, TypeNameExtractor typeNameExtractor,
							EnumTypeDeterminer enumTypeDeterminer, ModelSpecificationFactory modelSpecifications) {
		super(typeResolver, typeNameExtractor, enumTypeDeterminer, modelSpecifications);
		this.typeNameExtractor = typeNameExtractor;
		this.typeResolver = typeResolver;
	}

	@Override
	public void apply(ModelContext context) {
		GaModel annotation = AnnotationUtils.findAnnotation(forClass(context), GaModel.class);
		if (annotation != null) {
			context.getBuilder().description(annotation.text());
			context.getModelSpecificationBuilder().facets(f -> f.description(annotation.text()));
			super.apply(context);
		}
	}

	private Class<?> forClass(ModelContext context) {
		return typeResolver.resolve(context.getType()).getErasedType();
	}

}
