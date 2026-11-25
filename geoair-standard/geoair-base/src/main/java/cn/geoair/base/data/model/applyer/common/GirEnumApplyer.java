package cn.geoair.base.data.model.applyer.common;

import cn.geoair.base.data.GiVisuable;
import cn.geoair.base.data.GiVisualValuable;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.data.model.applyer.GiModelFieldApplyer;
import cn.geoair.base.def.annotation.GaParameter;

import java.lang.reflect.Field;

public class GirEnumApplyer implements GiModelFieldApplyer {

    @Override
    public void apply(String type, Object model, Field field, Field tar, int tag, GaParameter[] cfg)
            throws Exception {

        if (type == "select") {
            GaModelField mf = tar.getAnnotation(GaModelField.class);
            if (mf == null) {
                throw new RuntimeException("枚举映射找不到对应的 GaModelField注解");
            }
            Class<? extends Enum<?>> emCls = mf.em();

            if (GiVisuable.class.isAssignableFrom(emCls)) {
                Object tarV = tar.get(model);
                for (GiVisualValuable<?> obj : (GiVisualValuable[]) emCls.getEnumConstants()) {
                    if (obj.value().equals(tarV)) {
                        if (field.getType().isEnum()) {
                            field.set(model, obj);
                        } else {
                            field.set(model, obj.display());
                        }
                        break;
                    }
                }
            }
        }
    }
}
