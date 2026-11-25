package cn.geoair.comp.knife4j.ext.springdoc.builder;

import cn.geoair.web.data.result.GirWebResult;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * GiResultOperationConfig class.
 *
 * @author Administrator
 * @version $Id: $Id
 */
@Configuration
public class GiResultOperationConfig {

    /**
     * giResultOperationCustomizer.
     *
     * @return a {@link org.springdoc.core.customizers.OperationCustomizer} object
     */
    @Bean
    public OperationCustomizer giResultOperationCustomizer() {
        return (operation, handlerMethod) -> {
            // 1. 获取方法返回值类型
            MethodParameter returnParameter = handlerMethod.getReturnType();
            Type returnType = returnParameter.getGenericParameterType();

            // 2. 判断是否是GirWebResult泛型类型
            if (returnType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) returnType;
                // 替换ReflectionUtil.getRawClass：原生反射获取原始类
                Class<?> rawClass = getRawClass(parameterizedType);
                if (rawClass != null && GirWebResult.class.isAssignableFrom(rawClass)) {
                    // 3. 构建默认响应
                    ApiResponses apiResponses = operation.getResponses();
                    if (apiResponses == null) {
                        apiResponses = new ApiResponses();
                        operation.setResponses(apiResponses);
                    }

                    // 4. 添加200响应，指定正确的泛型Schema
                    ApiResponse apiResponse = new ApiResponse();
                    Content content = new Content();
                    MediaType mediaType = new MediaType();

                    // 获取泛型参数名称（纯原生反射）
                    String genericName = getGenericClassName(parameterizedType);
                    Schema<?> schema = new Schema<>().$ref("#/components/schemas/" + genericName);
                    mediaType.schema(schema);
                    content.addMediaType("application/json", mediaType);

                    apiResponse.content(content).description("请求成功");
                    apiResponses.addApiResponse("200", apiResponse);
                }
            }
            return operation;
        };
    }

    /** 原生反射：获取ParameterizedType的原始类 */
    private Class<?> getRawClass(ParameterizedType parameterizedType) {
        Type rawType = parameterizedType.getRawType();
        if (rawType instanceof Class<?>) {
            return (Class<?>) rawType;
        }
        return null;
    }

    /** 原生反射：获取泛型参数的类名称（如GirWebResult<DemoVo1> -> DemoVo1） */
    private String getGenericClassName(ParameterizedType parameterizedType) {
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        if (actualTypeArguments.length > 0) {
            Type genericType = actualTypeArguments[0];
            // 解析泛型参数的原始类名称
            Class<?> genericClass = null;
            if (genericType instanceof Class<?>) {
                genericClass = (Class<?>) genericType;
            } else if (genericType instanceof ParameterizedType) {
                genericClass = getRawClass((ParameterizedType) genericType);
            }
            return genericClass != null ? genericClass.getSimpleName() : "Object";
        }
        return "Object";
    }
}
