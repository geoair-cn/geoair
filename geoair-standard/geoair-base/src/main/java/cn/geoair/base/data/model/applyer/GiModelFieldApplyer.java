package cn.geoair.base.data.model.applyer;

import cn.geoair.base.def.annotation.GaParameter;

import java.lang.reflect.Field;

public interface GiModelFieldApplyer {

    /**
     * 应用字段处理逻辑
     *
     * @param type 处理类型标识
     * @param model 数据模型对象
     * @param field 源字段
     * @param tar 目标字段
     * @param tag 标签标识
     * @param cfg 参数配置数组
     * @throws Exception 处理异常，将阻断其它FieldApplyer执行
     */
    void apply(String type, Object model, Field field, Field tar, int tag, GaParameter[] cfg)
            throws Exception;
}
