package cn.geoair.comp.knife4j.ext.builder;

import cn.geoair.gtc.base.data.GiVisualValuable;
import cn.geoair.gtc.base.data.common.GemNull;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import io.swagger.annotations.ApiParam;
import springfox.bean.validators.plugins.Validators;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.service.AllowableListValues;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 处理非@RequestBody参数（@RequestParam/@PathVariable等）的GaModelField注解扩展
 */
public class GaModelFieldParameterBuilder implements ParameterBuilderPlugin {

    @Override
    public void apply(ParameterContext context) {
        // 1. 先检查是否有原生@ApiParam注解，有则跳过（保留优先级）
        ApiParam apiParam = getApiParamAnnotation(context);
        if (apiParam != null && (apiParam.value() != null && !apiParam.value().isEmpty())) {
            return;
        }

        // 2. 获取自定义的GaModelField注解
        GaModelField gaModelField = getGaModelFieldAnnotation(context);
        if (gaModelField == null) {
            return; // 没有自定义注解，直接返回
        }

        // 3. 解析GaModelField注解的信息并设置到参数中
        ParameterBuilder builder = context.parameterBuilder();
        // 3.1 设置参数描述
        if (gaModelField.text() != null && !gaModelField.text().isEmpty()) {
            builder.description(gaModelField.text());
        }

        // 3.2 解析枚举值（和原有逻辑一致）
        if (gaModelField.em() != GemNull.class) {
            parseEnumValues(gaModelField, builder);
        }
    }

    /**
     * 获取原生@ApiParam注解（兼容不同版本API）
     */
    private ApiParam getApiParamAnnotation(ParameterContext context) {

        Optional<ApiParam> optionalApiParam = Validators.annotationFromParameter(context, ApiParam.class);
        return optionalApiParam.orElse(null);

    }

    /**
     * 获取自定义GaModelField注解（兼容不同版本API）
     */
    private GaModelField getGaModelFieldAnnotation(ParameterContext context) {
        Optional<GaModelField> optionalApiParam = Validators.annotationFromParameter(context, GaModelField.class);
        return optionalApiParam.orElse(null);
    }

    /**
     * 解析枚举值并设置到参数中
     */
    private void parseEnumValues(GaModelField gaModelField, ParameterBuilder builder) {
        Class<? extends Enum<?>> enumClass = gaModelField.em();
        Object[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants == null || enumConstants.length == 0) {
            return;
        }

        try {
            List<String> enumValues = new ArrayList<>();
            for (Object enumObj : enumConstants) {
                if (enumObj instanceof GiVisualValuable) {
                    GiVisualValuable visualValuable = (GiVisualValuable) enumObj;
                    String display = visualValuable.display();
                    Object value = visualValuable.value();
                    enumValues.add("{name: " + display + ";code: " + value + "}");
                }
            }
            if (!enumValues.isEmpty()) {
                builder.allowableValues(new AllowableListValues(enumValues, "LIST"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        // 支持所有Swagger文档类型
        return true;
    }
}
