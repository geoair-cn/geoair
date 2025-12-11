package cn.geoair.gtc.base.data;

import java.io.Serializable;

public interface GiTypeable extends Serializable{

	/*
	default  gtcType  gtcType() {
		return  gtcType.valueOf( gtcDigestUtil.md5DigestAsHex((this.getClass().getName()).getBytes()), this.getClass().getName());
	}
	*/

	GiType  gtcType();

}
