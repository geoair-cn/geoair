package cn.geoair.comp.knife4j.ext.builder;


import cn.geoair.gtc.base.data.GiVisualValuable;
import cn.geoair.gtc.base.data.common.GemNull;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import springfox.bean.validators.plugins.Validators;
import springfox.documentation.schema.property.ModelSpecificationFactory;
import springfox.documentation.service.AllowableListValues;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelPropertyContext;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.swagger.common.SwaggerPluginSupport;
import springfox.documentation.swagger.schema.ApiModelPropertyPropertyBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static springfox.documentation.swagger.common.SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER;


/**
 * @author ：张俊
 * @date ：Created in 2022/8/23 9:36
 * @description： GtcModelField 替换 ApiModelProperty 注解（jsonbody参数）
 */
@Order(value = SWAGGER_PLUGIN_ORDER + 1)
public class GaModelFieldBuilder implements ModelPropertyBuilderPlugin {


    @Override
    public void apply(ModelPropertyContext context) {
        if (context.getAnnotatedElement().isPresent()) {
            ApiModelProperty model = AnnotationUtils.getAnnotation(context.getAnnotatedElement().get(), ApiModelProperty.class);
            if (model != null) {
                return;
            }
        }
        GaModelField column = context
                .getBeanPropertyDefinition()
                .get()
                .getField()
                .getAnnotation(GaModelField.class);
        if (column != null) {
            if (column.text() != null && !column.text().isEmpty()) {
                context.getBuilder().description(column.text());
                context.getSpecificationBuilder().description(column.text());
            }
            if (column.em() != GemNull.class) {
                Class<? extends Enum<?>> em = column.em();
                Object[] objects = em.getEnumConstants();
                try {
                    List<String> enumValues = new ArrayList<>();
                    for (Object obj : objects) {
                        if (obj instanceof GiVisualValuable) {
                            GiVisualValuable obj1 = (GiVisualValuable) obj;
                            // 3.调用对应方法，得到枚举常量中字段的值
                            String display = obj1.display();
                            Object value = obj1.value();
                            enumValues.add("{name: " + display + ";code: " + value + "}");
                        }

                    }
                    context.getBuilder().allowableValues(new AllowableListValues(enumValues, "LIST"));
                    context.getSpecificationBuilder().enumerationFacet(e -> new AllowableListValues(enumValues, "LIST"));

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        return SwaggerPluginSupport.pluginDoesApply(documentationType);
    }
}

