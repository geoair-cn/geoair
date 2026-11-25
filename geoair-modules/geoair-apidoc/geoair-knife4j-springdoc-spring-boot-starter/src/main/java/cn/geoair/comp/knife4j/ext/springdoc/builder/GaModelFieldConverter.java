package cn.geoair.comp.knife4j.ext.springdoc.builder;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.hutool.core.util.TypeUtil;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

import org.springframework.core.annotation.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Optional;

/**
 * GaModelFieldConverter class.
 *
 * @author Administrator
 * @version $Id: $Id
 */
public class GaModelFieldConverter implements ModelConverter {

    /** {@inheritDoc} */
    @Override
    public Schema<?> resolve(
            AnnotatedType annotatedType,
            ModelConverterContext context,
            Iterator<ModelConverter> chain) {

        // 先执行默认的转换器逻辑，获取基础Schema
        Schema<?> schema = chain.next().resolve(annotatedType, context, chain);
        if (schema == null) {
            return null;
        }

        Type type = annotatedType.getType();
        Class<?> clazz = TypeUtil.getClass(type); //  Hutool一键解析类型
        if (clazz != null) {
            GaModel gaModel = AnnotationUtils.findAnnotation(clazz, GaModel.class);
            if (gaModel != null) {
                schema.setDescription(gaModel.text());
                schema.setTitle(gaModel.text());
            }
        }

        // ========== 2. 处理字段级别@GaModelField注解 ==========
        Optional<Annotation[]> fieldAnnotations = getFieldAnnotations(annotatedType);
        if (fieldAnnotations.isPresent()) {
            for (Annotation ann : fieldAnnotations.get()) {
                if (ann instanceof GaModelField) {
                    GaModelField ann1 = (GaModelField) ann;
                    schema.setDescription(ann1.text());
                }
            }
        }

        return schema;
    }

    /** 从AnnotatedType中获取字段注解（兼容所有版本） */
    private Optional<Annotation[]> getFieldAnnotations(AnnotatedType annotatedType) {
        // 方式1：优先从ctxAnnotations获取（新版API标准）
        Annotation[] ctxAnnotations = annotatedType.getCtxAnnotations();
        if (ctxAnnotations != null && ctxAnnotations.length > 0) {
            return Optional.of(ctxAnnotations);
        }

        return Optional.empty();
    }
}
