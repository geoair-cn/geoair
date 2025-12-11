package cn.geoair.gtc.base.data.support;

import cn.geoair.gtc.base.data.GiType;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;

@SuppressWarnings("serial")
public class GtcTypeKid implements GiType {


	public static GtcTypeKid valueOf(String  gtcTypeId, String  gtcTypeName) {
		return new GtcTypeKid( gtcTypeId, gtcTypeName);
	}


	@GaModelField(isID=true)
	private String  gtcTypeId;

	@GaModelField(isDisplay = true)
	private String  gtcTypeName;

	public GtcTypeKid() {}


	public GtcTypeKid(String  gtcTypeId, String  gtcTypeName) {
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

	public GtcTypeKid setgtcTypeId(String  gtcTypeId) {
		this. gtcTypeId =  gtcTypeId;
		return this;
	}

	public String getgtcTypeName() {
		return  gtcTypeName;
	}

	public GtcTypeKid setgtcTypeName(String  gtcTypeName) {
		this. gtcTypeName =  gtcTypeName;
		return this;
	}


}
