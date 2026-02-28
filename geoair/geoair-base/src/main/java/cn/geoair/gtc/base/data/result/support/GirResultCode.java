package cn.geoair.gtc.base.data.result.support;

import cn.geoair.gtc.base.data.model.annotation.GaModelField;
import cn.geoair.gtc.base.data.result.GiResultCode;

public enum GirResultCode implements GiResultCode {

	成功(200), 未注册(394), 未实名认证(398), 未登录(399), 权限异常(403), 验证异常(499), 业务异常(599), 系统异常(999),;

	@GaModelField(isID = true)
	private Integer code;

	GirResultCode(Integer code) {
		this.code = code;
	}

	public Integer getCode() {
		return this.code;
	}

	@Override
	public String display() {
		return this.name();
	}

	@Override
	public Integer value() {
		return this.code;
	}

}
