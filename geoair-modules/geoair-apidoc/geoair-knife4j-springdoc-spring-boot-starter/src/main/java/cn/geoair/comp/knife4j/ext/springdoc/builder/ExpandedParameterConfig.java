// package cn.geoair.comp.knife4j.ext.springdoc.builder;
//
// import cn.geoair.base.data.model.annotation.GaModelField;
// import io.swagger.v3.oas.models.Operation;
// import io.swagger.v3.oas.models.media.Schema;
// import io.swagger.v3.oas.models.parameters.Parameter;
// import io.swagger.v3.oas.models.parameters.RequestBody;
// import io.swagger.v3.oas.models.parameters.RequestBodyBuilder;
// import org.springdoc.core.customizers.OperationCustomizer;
// import org.springdoc.core.customizers.ParameterCustomizer;
// import org.springdoc.core.providers.ObjectMapperProvider;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.core.annotation.AnnotationUtils;
// import org.springframework.web.method.HandlerMethod;
// import org.springframework.web.method.annotation.ParamMethodArgumentResolver;
//
// import java.lang.reflect.Field;
// import java.lang.reflect.Parameter;
// import java.util.*;
//
/// **
// * 自定义参数解析器：将对象入参字段展开为独立参数（模拟springfox ExpandedParameterBuilderPlugin）
// */
// @Configuration
// public class ExpandedParameterConfig {
//
//
// @Bean
// public ParameterCustomizer expandedParameterCustomizer(ObjectMapperProvider
// objectMapperProvider) {
// return (parameter, parameterContext) -> {
// // 1. 获取入参的原始类型和字段信息
//
// HandlerMethod handlerMethod = parameter.getHandlerMethod();
// Parameter[] methodParameters = handlerMethod.getMethod().getParameters();
// int paramIndex = parameterContext.getParameterIndex();
//
// if (paramIndex < methodParameters.length) {
// Parameter methodParam = methodParameters[paramIndex];
// Class<?> paramType = methodParam.getType();
//
// // 2. 只处理自定义对象类型（排除基本类型/字符串/集合等）
// if (isCustomObject(paramType)) {
// // 3. 解析对象字段，替换原参数为多个独立参数
// List<Parameter> expandedParams = buildExpandedParameters(paramType);
// // 这里通过parameterContext的扩展属性暂存展开的参数（后续在OperationCustomizer中替换）
// parameterContext.getExtensions().put("expandedParams", expandedParams);
// }
// }
//
// return parameter;
// };
// }
//
// // ========== 核心2：操作自定义器 - 替换RequestBody为展开的独立参数 ==========
// @Bean
// public OperationCustomizer expandedOperationCustomizer() {
// return (operation, handlerMethod) -> {
// // 1. 移除默认生成的RequestBody（因为对象被解析为JSON体）
// operation.setRequestBody(null);
//
// // 2. 遍历所有参数，替换为展开的字段参数
// List<Parameter> finalParams = new ArrayList<>();
// if (operation.getParameters() != null) {
// for (Parameter param : operation.getParameters()) {
// // 3. 获取暂存的展开参数，添加到最终参数列表
// List<Parameter> expandedParams = (List<Parameter>)
// param.getExtensions().get("expandedParams");
// if (expandedParams != null && !expandedParams.isEmpty()) {
// finalParams.addAll(expandedParams);
// } else {
// // 非对象参数直接保留
// finalParams.add(param);
// }
// }
// }
//
// // 4. 设置最终的独立参数列表
// operation.setParameters(finalParams);
// return operation;
// };
// }
//
// // ========== 工具方法1：判断是否为需要展开的自定义对象 ==========
// private boolean isCustomObject(Class<?> clazz) {
// // 排除基本类型、字符串、集合、数组、Map、MultipartFile等
// return !clazz.isPrimitive()
// && !clazz.equals(String.class)
// && !Collection.class.isAssignableFrom(clazz)
// && !Map.class.isAssignableFrom(clazz)
// && !clazz.isArray()
// && !clazz.getName().startsWith("org.springframework.web.multipart")
// && !clazz.getName().startsWith("java.")
// && !clazz.getName().startsWith("javax.");
// }
//
// // ========== 工具方法2：解析对象字段，构建独立Parameter ==========
// private List<Parameter> buildExpandedParameters(Class<?> objClass) {
// List<Parameter> parameters = new ArrayList<>();
//
// // 遍历对象的所有字段（可扩展：支持父类字段）
// Field[] fields = objClass.getDeclaredFields();
// for (Field field : fields) {
// field.setAccessible(true);
// Parameter param = new Parameter();
//
// // 1. 设置参数基本信息
// param.setName(field.getName());
// param.setIn("query"); // 可改为form/formData，根据你的参数传递方式
// param.setRequired(true); // 可通过注解控制是否必填（如@NotBlank）
//
// // 2. 设置参数类型（根据字段类型映射OpenAPI类型）
// Schema<?> schema = mapFieldTypeToSchema(field.getType());
// param.setSchema(schema);
//
// // 3. 解析@GaModelField注解，设置描述
// GaModelField gaModelField = AnnotationUtils.findAnnotation(field, GaModelField.class);
// if (gaModelField != null && gaModelField.text() != null) {
// param.setDescription(gaModelField.text());
// } else {
// param.setDescription("字段：" + field.getName());
// }
//
// parameters.add(param);
// }
//
// return parameters;
// }
//
// // ========== 工具方法3：字段类型映射为OpenAPI Schema ==========
// private Schema<?> mapFieldTypeToSchema(Class<?> fieldType) {
// Schema<?> schema = new Schema<>();
//
// if (fieldType.equals(String.class)) {
// schema.setType("string");
// } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
// schema.setType("integer");
// schema.setFormat("int32");
// } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
// schema.setType("integer");
// schema.setFormat("int64");
// } else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
// schema.setType("boolean");
// } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
// schema.setType("number");
// schema.setFormat("double");
// } else if (Collection.class.isAssignableFrom(fieldType)) {
// schema.setType("array");
// Schema<Object> objectSchema = new Schema<>();
// objectSchema.setType("string");
// schema.setItems(objectSchema); // 集合默认按字符串数组处理
// } else {
// // 嵌套对象递归展开（可选：这里简化为字符串）
// schema.setType("string");
// }
//
// return schema;
// }
// }
