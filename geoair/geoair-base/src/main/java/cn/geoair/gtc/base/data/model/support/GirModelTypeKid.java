package cn.geoair.gtc.base.data.model.support;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import cn.geoair.gtc.base.data.model.annotation.GaModel;
import cn.geoair.gtc.base.data.support.GirTypeKid;
import cn.geoair.gtc.base.util.GutilDigest;
import cn.geoair.gtc.base.util.GutilObject;
import cn.geoair.gtc.base.data.model.GiModelType;
import cn.geoair.gtc.base.data.model.GiTypeModelable;

public class GirModelTypeKid extends GirTypeKid implements GiModelType {

	/**
	 *
	 */
	private static final long serialVersionUID = -2085317997278008680L;

	@SuppressWarnings("rawtypes")
	private static final Map<Class<? extends GiTypeModelable>, GirModelTypeKid> typeModelTypes = new HashMap<>();

	public static <TM extends GiTypeModelable<ID>, ID extends Serializable> GirModelTypeKid valueFor(
			Class<TM> typeModelClass) {

		GirModelTypeKid ut = typeModelTypes.get(typeModelClass);
		if (ut == null) {
			GaModel an = typeModelClass.getAnnotation(GaModel.class);
			String modelTypeId = null;
			String modelTypeName = null;
			if (an != null) {
				if (!GutilObject.equal(GaModel.NULL, an.name())) {
					modelTypeId = an.name();
					if (!GutilObject.equal(GaModel.NULL, an.text())) {
						modelTypeName = an.text();
					}
				}
			}
			if (modelTypeId == null) {
				modelTypeId = GutilDigest.md5DigestAsHex((typeModelClass.getName()).getBytes());
			}
			if (modelTypeName == null) {
				modelTypeName = typeModelClass.getName();
			}
			ut = new GirModelTypeKid(typeModelClass, modelTypeId, modelTypeName);
			typeModelTypes.put(typeModelClass, ut);
		}

		return ut;

	}

	@SuppressWarnings("rawtypes")
	public static GirModelTypeKid valueFor(String modelTypeId) {
		if (typeModelTypes.size() > 0) {
			Iterator<Entry<Class<? extends GiTypeModelable>, GirModelTypeKid>> it = typeModelTypes.entrySet()
					.iterator();
			while (it.hasNext()) {
				Entry<Class<? extends GiTypeModelable>, GirModelTypeKid> entry = it.next();
				if (GutilObject.equal(entry.getValue().getgtcTypeId(), modelTypeId)) {
					return entry.getValue();
				}
			}
		}
		return null;
	}

	public GirModelTypeKid() {

	}

	public GirModelTypeKid(Class<? extends GiTypeModelable> typeModelClass, String gtcTypeId, String gtcTypeName) {
		super(gtcTypeId, gtcTypeName);
		this.gtcTypeModelClass = typeModelClass;

	}

	private Class<? extends GiTypeModelable> gtcTypeModelClass;

	public Class<? extends GiTypeModelable> getgtcTypeModelClass() {
		return gtcTypeModelClass;
	}

	public void setgtcTypeModelClass(Class<? extends GiTypeModelable> gtcTypeModelClass) {
		this.gtcTypeModelClass = gtcTypeModelClass;
	}

	@Override
	public <T extends GiTypeModelable> Class<T> gtcTypeModelClass(Class<? super T> cls) {
		return (Class<T>) gtcTypeModelClass;
	}

}
