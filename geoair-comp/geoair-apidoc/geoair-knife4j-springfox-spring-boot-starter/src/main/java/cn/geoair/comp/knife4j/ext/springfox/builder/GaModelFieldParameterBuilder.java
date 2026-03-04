package cn.geoair.comp.knife4j.ext.springfox.builder;

import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.common.GemNull;
import cn.geoair.base.data.model.annotation.GaModelField;
import com.fasterxml.classmate.ResolvedType;
import io.swagger.annotations.ApiParam;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import springfox.bean.validators.plugins.Validators;
import springfox.documentation.builders.RequestParameterBuilder;
import springfox.documentation.schema.Example;
import springfox.documentation.service.ParameterType;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static springfox.documentation.schema.Collections.collectionElementType;
import static springfox.documentation.schema.Collections.isContainerType;

/**
 * 适配高版本Springfox（3.0+）的GaModelField注解解析器 支持：1.单个@RequestParam参数 2.非@RequestBody实体类参数的字段
 * 基于你提供的ParameterContext真实API重构，无不存在的方法
 */
@Component
public class GaModelFieldParameterBuilder implements ParameterBuilderPlugin {

    @Override
    public void apply(ParameterContext context) {
        ResolvedMethodParameter resolvedMethodParam = context.resolvedMethodParameter();
        Class<?> parameterType = resolvedMethodParam.getParameterType().getErasedType();

        // 修复swagger3.0的文件上传参数的解析异常
        if (isFileType(resolvedMethodParam.getParameterType()) || isListOfFiles(resolvedMethodParam.getParameterType())) {
            context.parameterBuilder().parameterType("formData" );
            context.requestParameterBuilder()
                    .in("formData" )
                    .accepts(Collections.singleton(MediaType.MULTIPART_FORM_DATA)).description("待上传的文件");
        }


        // 2. 优先检查原生@ApiParam注解（优先级最高）
        Optional<ApiParam> optionalApiParam = Validators.annotationFromParameter(context, ApiParam.class);
        if (optionalApiParam.isPresent()) {
            return; // 有原生注解，跳过自定义注解解析
        }

        // 3. 解析@GaModelField注解（先实体类字段 → 再单个参数）
        GaModelField gaModelField = null;

        // 3.1 非基础类型 → 按实体类字段解析
        if (!isBasicType(parameterType)) {
            gaModelField = getEntityFieldAnnotation(parameterType, resolvedMethodParam.defaultName().get());
        }

        // 3.2 基础类型/实体类字段无注解 → 按单个参数解析
        if (gaModelField == null) {
            Optional<GaModelField> gaModelField1 = Validators.annotationFromParameter(context, GaModelField.class);
            if (gaModelField1.isPresent()) {
                gaModelField = gaModelField1.get();
            }
        }

        // 4. 无自定义注解 → 直接返回
        if (gaModelField == null) {
            return;
        }

        // 5. 构建参数文档信息（使用高版本的RequestParameterBuilder）
        RequestParameterBuilder requestParamBuilder = context.requestParameterBuilder();

        // 5.1 设置参数描述
        if (gaModelField.text() != null && !gaModelField.text().isEmpty()) {
            requestParamBuilder.description(gaModelField.text());
        }

        // 5.2 设置枚举值（兼容原有逻辑）
        if (gaModelField.em() != GemNull.class) {
            parseEnumValues(gaModelField, requestParamBuilder);
        }

        // 可选：设置参数类型（QUERY/FORM等，默认自动识别）
        requestParamBuilder.in(ParameterType.QUERY);
    }

    /**
     * 判断是否是基础类型（包括包装类、字符串、基本类型）
     */
    private boolean isBasicType(Class<?> clazz) {
        return clazz.isPrimitive() || String.class.isAssignableFrom(clazz) || Number.class.isAssignableFrom(clazz)
                || Boolean.class.isAssignableFrom(clazz) || Character.class.isAssignableFrom(clazz);
    }

    /**
     * 递归解析实体类（含父类）字段上的@GaModelField注解
     */
    private GaModelField getEntityFieldAnnotation(Class<?> entityClass, String fieldName) {
        // 查找当前类的字段
        Field field = ReflectionUtils.findField(entityClass, fieldName);
        if (field != null) {
            return AnnotationUtils.findAnnotation(field, GaModelField.class);
        }

        // 递归查找父类字段（排除Object类）
        Class<?> superClass = entityClass.getSuperclass();
        if (superClass != null && !superClass.equals(Object.class)) {
            return getEntityFieldAnnotation(superClass, fieldName);
        }

        return null;
    }

    /**
     * 解析枚举值并设置到请求参数中（适配高版本API）
     */
    private void parseEnumValues(GaModelField gaModelField, RequestParameterBuilder builder) {
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
                    enumValues.add(String.format("{name: %s; code: %s}" , display, value));
                }
            }

            if (!enumValues.isEmpty()) {
                List<Example> examples = new ArrayList<>();
                for (String enumValue : enumValues) {
                    Example example = new Example(enumValue);
                    examples.add(example);
                }
                builder.examples(examples);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        // 支持Swagger2和OpenAPI3
        return DocumentationType.SWAGGER_2.equals(documentationType)
                || DocumentationType.OAS_30.equals(documentationType);
    }

    private static boolean isListOfFiles(ResolvedType parameterType) {
        return isContainerType(parameterType) && isFileType(collectionElementType(parameterType));
    }

    private static boolean isFileType(ResolvedType parameterType) {
        return MultipartFile.class.isAssignableFrom(parameterType.getErasedType());
    }


}
