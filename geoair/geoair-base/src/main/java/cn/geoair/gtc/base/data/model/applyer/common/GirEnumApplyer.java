package cn.geoair.gtc.base.data.model.applyer.common;

import java.lang.reflect.Field;

import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.def.annotation.GaParameter;
import cn.geoair.gtc.base.data.GiVisuable;
import cn.geoair.gtc.base.data.GiVisualValuable;
import cn.geoair.gtc.base.data.model.applyer.GiModelFieldApplyer;

public class GirEnumApplyer implements GiModelFieldApplyer {

	@Override
	public void apply(String type, Object model, Field field, Field tar, int tag, GaParameter[] cfg) throws Exception {

		if (type == "select") {
			GaModelField mf = tar.getAnnotation(GaModelField.class);
			if (mf == null) {
				throw new RuntimeException("枚举映射找不到对应的 gtcModelField注解");
			}
			Class<? extends Enum<?>> emCls = mf.em();

			if (GiVisuable.class.isAssignableFrom(emCls)) {
				Object tarV = tar.get(model);
				for (GiVisualValuable<?> obj : (GiVisualValuable[]) emCls.getEnumConstants()) {
					if (obj.value().equals(tarV)) {
						if (field.getType().isEnum()) {
							field.set(model, obj);
						}
						else {
							field.set(model, obj.display());
						}
						break;
					}
				}
			}
		}
	}

}
