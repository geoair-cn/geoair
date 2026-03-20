package cn.geoair.comp.db.service.core.common;

import cn.geoair.base.data.result.GiResult;

/**
 * @program: api
 * @description:
 * @author: 武汉刘德华
 * @create: 2020-08-11 11:22
 */
public class ResponseDto<T> implements GiResult<T> {

	String msg;

	int code;

	boolean success;


	T data;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public Object getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public boolean getSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public static ResponseDto apiSuccess(Object data) {
		return successWithData(data);
	}

	public static ResponseDto successWithMsg(String msg) {
		ResponseDto dto = new ResponseDto();
		dto.setData(null);
		dto.setSuccess(true);
		dto.setMsg(msg);
		return dto;
	}

	public static ResponseDto successWithData(Object data) {
		ResponseDto dto = new ResponseDto();
		dto.setData(data);
		dto.setSuccess(true);
		return dto;
	}

	public static ResponseDto fail(String msg) {
		ResponseDto dto = new ResponseDto();
		dto.setSuccess(false);
		dto.setMsg(msg);
		return dto;
	}

	public static ResponseDto failWithData(String msg, Object data) {
		ResponseDto dto = new ResponseDto();
		dto.setSuccess(false);
		dto.setMsg(msg);
		dto.setData(data);
		return dto;
	}

	@Override
	public int code() {
		return code;
	}

	@Override
	public GiResult andCode(int code) {
		this.code = code;
		return this;
	}

	@Override
	public String alertMsg() {
		return msg;
	}

	@Override
	public T value() {
		return data;
	}

	@Override
	public GiResult andAlertMsg(String msg) {
		this.msg = msg;
		return this;
	}

	@Override
	public int alertType() {
		return 0;
	}

	@Override
	public GiResult andAlertType(int alertType) {
		return null;
	}

	@Override
	public GiResult andValue(T value) {
		this.data = value;
		return this;
	}

	@Override
	public GiResult forSuccess() {
		return successWithData("调用成功，无结果集");
	}

	@Override
	public GiResult forFailure() {
		this.success = false;

		return fail("系统异常");
	}

}
