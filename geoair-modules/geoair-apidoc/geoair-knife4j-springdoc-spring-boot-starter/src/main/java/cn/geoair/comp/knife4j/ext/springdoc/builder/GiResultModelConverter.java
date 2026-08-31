package cn.geoair.comp.knife4j.ext.springdoc.builder;

import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.data.result.GiResult;
import cn.geoair.web.data.result.GirWebResult;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.*;

import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;

/**
 * 解析GirWebResult<T>泛型响应的自定义Model转换器（纯原生反射，无第三方依赖）
 *
 * @author Administrator
 * @version $Id: $Id
 */
public class GiResultModelConverter implements ModelConverter {

    /** {@inheritDoc} */
    @Override
    public Schema<?> resolve(
            AnnotatedType annotatedType,
            ModelConverterContext context,
            Iterator<ModelConverter> chain) {

        // 先执行默认解析，获取基础Schema
        Schema<?> schema = chain.next().resolve(annotatedType, context, chain);
        if (schema == null) {
            return null;
        }

        Type type = annotatedType.getType();
        // 1. 判断是否是GirWebResult泛型类型
        if (type instanceof ParameterizedType) {
            // 替换ReflectionUtil.getRawClass：原生反射获取原始类
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Class<?> rawClass = getRawClass(parameterizedType);
            if (!(rawClass == GiResult.class)) {
                return schema; // 非GirWebResult类型，返回默认解析结果
            }

            // 2. 构建完整的GirWebResult响应Schema
            ObjectSchema resultSchema = new ObjectSchema();
            resultSchema.setName(rawClass.getSimpleName()); // 设置Schema名称

            // 3.1 添加GirResult父类的固定字段
            addParentResultFields(resultSchema);

            // 3.2 添加GirWebResult自身的location字段（解析@GaModelField）
            addGirWebResultFields(resultSchema);

            // 4. 解析泛型参数（如DemoVo1）并添加value字段
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments != null && actualTypeArguments.length > 0) {
                Type genericType = actualTypeArguments[0];
                AnnotatedType genericAnnotatedType = new AnnotatedType(genericType);
                Schema<?> genericSchema = context.resolve(genericAnnotatedType);
                if (genericSchema != null) {
                    resultSchema.addProperty("value", genericSchema).description("响应数据");
                }
            }

            return resultSchema;
        }

        return schema;
    }

    /** 原生反射：获取ParameterizedType的原始类（替代ReflectionUtil.getRawClass） */
    private Class<?> getRawClass(ParameterizedType parameterizedType) {
        Type rawType = parameterizedType.getRawType();
        if (rawType instanceof Class<?>) {
            return (Class<?>) rawType;
        }
        return null;
    }

    /** 添加GirResult父类的固定字段（根据实际GirResult的字段调整） */
    private void addParentResultFields(ObjectSchema resultSchema) {
        resultSchema
                .addProperty("code", new IntegerSchema().description("响应码").example(200))
                .addProperty("msg", new StringSchema().description("响应信息").example("操作成功"))
                .addProperty("success", new BooleanSchema().description("是否成功").example(true));
    }

    /** 添加GirWebResult自身的location字段，解析@GaModelField注解 */
    private void addGirWebResultFields(ObjectSchema resultSchema) {
        try {
            // 获取location字段
            Field locationField = GirWebResult.class.getDeclaredField("location");
            // 解析@GaModelField注解
            GaModelField gaModelField =
                    AnnotationUtils.findAnnotation(locationField, GaModelField.class);

            String fieldDesc = "跳转地址";
            if (gaModelField != null && gaModelField.text() != null) {
                fieldDesc = gaModelField.text(); // 优先使用注解中的描述
            }

            // 添加location字段到Schema
            resultSchema.addProperty(
                    "location", new StringSchema().description(fieldDesc).example("/index"));
        } catch (NoSuchFieldException e) {
            // 字段不存在时添加默认描述
            resultSchema.addProperty("location", new StringSchema().description("跳转地址"));
        }
    }
}
