package cn.geoair.gtc.base.data.support;

import cn.geoair.gtc.base.data.GiGroup;
import cn.geoair.gtc.base.data.GiGroupType;
import cn.geoair.gtc.base.data.model.annotation.GaModelField;

@SuppressWarnings("serial")
public class GirGroupKid implements GiGroup {

	public static GirGroupKid valueOf(GiGroupType groupType, String  gtcGroupId, String  gtcGroupName) {
		return new GirGroupKid( GirGroupTypeKid.valueFor(groupType. gtcTypeId()), gtcGroupId, gtcGroupName);
	}


	@GaModelField(isID=true)
	private String  gtcGroupId;

	@GaModelField(isDisplay = true)
	private String  gtcGroupName;


	@GaModelField()
	private GirGroupTypeKid gtcGroupType;

	@Override
	public String  gtcGroupId() {
		return  gtcGroupId;
	}

	public GirGroupKid() {}


	public GirGroupKid(GirGroupTypeKid groupType, String  gtcGroupId, String  gtcGroupName) {
		this. gtcGroupId =  gtcGroupId;
		this. gtcGroupName =  gtcGroupName;
		this. gtcGroupType = groupType;
	}

	public GirGroupTypeKid gtcType() {
		return  gtcGroupType;
	}

	@Override
	public String  gtcGroupName() {
		return  gtcGroupName;
	}

	public String getgtcGroupId() {
		return  gtcGroupId;
	}

	public GirGroupKid setgtcGroupId(String  gtcGroupId) {
		this. gtcGroupId =  gtcGroupId;
		return this;
	}

	public String getgtcGroupName() {
		return  gtcGroupName;
	}

	public GirGroupKid setgtcGroupName(String  gtcGroupName) {
		this. gtcGroupName =  gtcGroupName;
		return this;
	}

	public GirGroupTypeKid getgtcGroupType() {
		return  gtcGroupType;
	}

	public void setgtcGroupType( GirGroupTypeKid gtcGroupType) {
		this. gtcGroupType =  gtcGroupType;
	}


}
