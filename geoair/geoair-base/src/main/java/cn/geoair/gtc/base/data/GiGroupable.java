package cn.geoair.gtc.base.data;

import java.io.Serializable;

public interface GiGroupable extends Serializable{


	GiGroup  gtcGroup();

	/*
	default  gtcGroup  gtcGroup() {
		return  gtcGroup.valueOf( gtcDigestUtil.md5DigestAsHex((this.getClass().getName()).getBytes()), this.getClass().getName());
	}
	*/
}
