package cn.geoair.gtc.base.data.model;

import cn.geoair.gtc.base.data.model.support.GirModelTypeKid;
import cn.geoair.gtc.base.util.GutilStr;
import cn.geoair.gtc.base.data.GiType;

public interface GiModelType extends GiType {

	public <T extends GiTypeModelable> Class<T> gtcTypeModelClass(Class<? super T> cls);

	default GirModelTypeKid toModelTypeKid() {

		if (GutilStr.isNotEmpty(this.gtcTypeId())) {
			GirModelTypeKid kid = GirModelTypeKid.valueFor(this.gtcTypeId());
			if (kid != null) {
				return kid;
			}
		}

		Class<? extends GiTypeModelable> typeModelClass = gtcTypeModelClass(GiTypeModelable.class);
		if (typeModelClass != null) {
			return GirModelTypeKid.valueFor(typeModelClass);
		}

		return null;
	}

}
