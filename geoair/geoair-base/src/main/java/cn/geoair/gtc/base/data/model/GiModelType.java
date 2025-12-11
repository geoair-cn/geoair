package cn.geoair.gtc.base.data.model;

import cn.geoair.gtc.base.data.model.support.GtcModelTypeKid;
import cn.geoair.gtc.base.util.GutilStr;
import cn.geoair.gtc.base.data.GiType;

public interface GiModelType extends GiType{


	public <T extends GiTypeModelable> Class<T>  gtcTypeModelClass(Class<? super T> cls);


	default GtcModelTypeKid toModelTypeKid() {

		if(GutilStr.isNotEmpty(this. gtcTypeId())) {
			 GtcModelTypeKid kid =  GtcModelTypeKid.valueFor(this. gtcTypeId());
			if(kid != null) {
				return kid;
			}
		}

		Class<? extends GiTypeModelable> typeModelClass =  gtcTypeModelClass(GiTypeModelable.class);
		if(typeModelClass != null) {
			return  GtcModelTypeKid.valueFor(typeModelClass);
		}

		return null;
	}

}
