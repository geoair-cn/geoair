package cn.geoair.gtc.base.data.support;

import cn.geoair.gtc.base.data.GiGroup;
import cn.geoair.gtc.base.data.GiGroupType;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;

@SuppressWarnings("serial")
public class GtcGroupKid implements GiGroup {

	public static GtcGroupKid valueOf(GiGroupType groupType, String  gtcGroupId, String  gtcGroupName) {
		return new GtcGroupKid( GtcGroupTypeKid.valueFor(groupType. gtcTypeId()), gtcGroupId, gtcGroupName);
	}


	@GaModelField(isID=true)
	private String  gtcGroupId;

	@GaModelField(isDisplay = true)
	private String  gtcGroupName;


	@GaModelField()
	private GtcGroupTypeKid  gtcGroupType;

	@Override
	public String  gtcGroupId() {
		return  gtcGroupId;
	}

	public GtcGroupKid() {}


	public GtcGroupKid(GtcGroupTypeKid groupType, String  gtcGroupId, String  gtcGroupName) {
		this. gtcGroupId =  gtcGroupId;
		this. gtcGroupName =  gtcGroupName;
		this. gtcGroupType = groupType;
	}

	public GtcGroupTypeKid  gtcType() {
		return  gtcGroupType;
	}

	@Override
	public String  gtcGroupName() {
		return  gtcGroupName;
	}

	public String getgtcGroupId() {
		return  gtcGroupId;
	}

	public GtcGroupKid setgtcGroupId(String  gtcGroupId) {
		this. gtcGroupId =  gtcGroupId;
		return this;
	}

	public String getgtcGroupName() {
		return  gtcGroupName;
	}

	public GtcGroupKid setgtcGroupName(String  gtcGroupName) {
		this. gtcGroupName =  gtcGroupName;
		return this;
	}

	public GtcGroupTypeKid getgtcGroupType() {
		return  gtcGroupType;
	}

	public void setgtcGroupType( GtcGroupTypeKid  gtcGroupType) {
		this. gtcGroupType =  gtcGroupType;
	}


}
