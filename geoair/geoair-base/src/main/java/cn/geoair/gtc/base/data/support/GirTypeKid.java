package cn.geoair.gtc.base.data.support;

import cn.geoair.gtc.base.data.GiType;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;

@SuppressWarnings("serial")
public class GirTypeKid implements GiType {


	public static GirTypeKid valueOf(String  gtcTypeId, String  gtcTypeName) {
		return new GirTypeKid( gtcTypeId, gtcTypeName);
	}


	@GaModelField(isID=true)
	private String  gtcTypeId;

	@GaModelField(isDisplay = true)
	private String  gtcTypeName;

	public GirTypeKid() {}


	public GirTypeKid(String  gtcTypeId, String  gtcTypeName) {
		this. gtcTypeId =  gtcTypeId;
		this. gtcTypeName =  gtcTypeName;
	}

	@Override
	public String  gtcTypeId() {
		return  gtcTypeId;
	}

	@Override
	public String  gtcTypeName() {
		return  gtcTypeName;
	}

	public String getgtcTypeId() {
		return  gtcTypeId;
	}

	public GirTypeKid setgtcTypeId(String  gtcTypeId) {
		this. gtcTypeId =  gtcTypeId;
		return this;
	}

	public String getgtcTypeName() {
		return  gtcTypeName;
	}

	public GirTypeKid setgtcTypeName(String  gtcTypeName) {
		this. gtcTypeName =  gtcTypeName;
		return this;
	}


}
