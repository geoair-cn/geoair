package cn.geoair.gtc.base.data.result;

import cn.geoair.gtc.base.data.GiValuable;
import cn.geoair.gtc.base.sp.GtcSpHelper;

/**
 *  数据结果集接口，定义了统一的结果返回格式
 * @author Ray
 *
 * @param <T> 实际数据类型
 */
public interface GiResult<T> extends GiValuable<T> {

	/**
	 * 获取状态码
	 * @return 状态码
	 */
	public int code();
	
	/**
	 * 设置状态码
	 * @param code 状态码
	 * @return 当前GiResult实例
	 */
	public GiResult<T> andCode(int code);
	
	/**
	 * 设置状态码
	 * @param code GiResultCode枚举类型的状态码
	 * @return 当前GiResult实例
	 */
	default public GiResult<T> andCode(GiResultCode code){
		return andCode(code.value());
	}

	/**
	 * 获取提示消息
	 * @return 提示消息字符串
	 */
	public String alertMsg();

	/**
	 *  获取值
	 * @return 泛型T的数据值
	 */
	public T value();
	
	/**
	 * 设置提示信息
	 * @param msg 提示信息
	 * @return 当前GiResult实例
	 */
	public GiResult<T> andAlertMsg(String msg);

	/**
	 * 获取消息提示类型
	 * @return 消息提示类型
	 */
	public int alertType();

	/**
	 * 设置消息提示类型
	 * @param alertType 消息提示类型
	 * @return 当前GiResult实例
	 */
	public GiResult<T> andAlertType(int alertType);

	/**
	 * 设置消息提示类型
	 * @param alertTypeEnum GtcEmAlertType枚举类型的消息提示类型
	 * @return 当前GiResult实例
	 */
	default public GiResult<T> andAlertTypeEnum( GtcEmAlertType alertTypeEnum){
		return andAlertType(alertTypeEnum.value());
	}

	/**
	 * 设置结果数据
	 * @param value 结果数据
	 * @return 当前GiResult实例
	 */
	public GiResult<T> andValue(T value);

	/**
	 * 设置为成功状态
	 * @return 当前GiResult实例
	 */
	public GiResult<T> forSuccess();

	/**
	 * 设置为成功状态并设置提示消息
	 * @param msg 成功提示消息
	 * @return 当前GiResult实例
	 */
	default public GiResult<T> forSuccessAlertMsg(String msg){
		return this.forSuccess().andAlertMsg(msg);
	}

	/**
	 * 设置为失败状态
	 * @return 当前GiResult实例
	 */
	public GiResult<T> forFailure();

	/**
	 * 设置为失败状态并设置提示消息
	 * @param msg 失败提示消息
	 * @return 当前GiResult实例
	 */
	default public GiResult<T> forFailureAlertMsg(String msg){
		return this.forFailure().andAlertMsg(msg);
	}

	/**
	 * 设置为失败状态并设置状态码、提示消息和消息类型
	 * @param code 状态码
	 * @param msg 提示消息
	 * @param alertType 消息提示类型
	 * @return 当前GiResult实例
	 */
	default public GiResult<T> forFailureSetCodeAndMsgAndType(int code,String msg, GtcEmAlertType alertType){
		return this.forFailureAlertMsg(msg).andAlertTypeEnum(alertType).andCode(code);
	}


	//--------------------------------------------------------------------------------------------------------------------


	/**
	 * 创建成功状态的结果对象并设置值
	 * @param <K> 值的类型
	 * @param value 要设置的值
	 * @return GiResult<K> 成功状态的结果对象
	 */
	@SuppressWarnings("unchecked")
	public static<K> GiResult<K> successValue(K value){
		Class<K> cls = null;
		if(value != null) {
			cls = (Class<K>)value.getClass();
		}
		return getResult(cls).andValue(value);
	}
	
	/**
	 * 创建成功状态的结果对象
	 * @param <K> 值的类型
	 * @return GiResult<K> 成功状态的结果对象
	 */
	public static<K> GiResult<K> success(){
		return GiResult.<K>successValue(null).forSuccess();
	}

	/**
	 * 创建成功状态的结果对象并设置消息
	 * @param <K> 值的类型
	 * @param msg 成功消息
	 * @return GiResult<K> 成功状态的结果对象
	 */
	public static<K> GiResult<K> successMsg(String msg){
		return GiResult.<K>success().andAlertMsg(msg);
	}
	
	/**
	 * 创建失败状态的结果对象
	 * @param <K> 值的类型
	 * @return GiResult<K> 失败状态的结果对象
	 */
	public static<K> GiResult<K> failure(){
		return GiResult.<K>getResult(null).forFailure();
	}

	/**
	 * 创建失败状态的结果对象并设置消息
	 * @param <K> 值的类型
	 * @param msg 失败消息
	 * @return GiResult<K> 失败状态的结果对象
	 */
	public static<K> GiResult<K> failureMsg(String msg){
		return GiResult.<K>failure().andAlertMsg(msg);
	}

	/**
	 * 创建失败状态的结果对象并设置消息和消息类型
	 * @param <K> 值的类型
	 * @param msg 失败消息
	 * @param alertType 消息提示类型
	 * @return GiResult<K> 失败状态的结果对象
	 */
	public static<K> GiResult<K> failureSetMsgAndType(String msg, GtcEmAlertType alertType){
		return GiResult.<K>failureMsg(msg).andAlertTypeEnum(alertType);
	}

	/**
	 * 创建失败状态的结果对象并设置状态码、消息和消息类型
	 * @param <K> 值的类型
	 * @param code 状态码
	 * @param msg 失败消息
	 * @param alertType 消息提示类型
	 * @return GiResult<K> 失败状态的结果对象
	 */
	public static<K> GiResult<K> failureSetCodeAndMsgAndType(int code ,String msg, GtcEmAlertType alertType){
		return GiResult.<K>failureMsg(msg).andAlertTypeEnum(alertType).andCode(code);
	}

	/**
	 * 根据类型获取结果对象实例
	 * @param <K> 值的类型
	 * @param resultType 结果值的类类型
	 * @return GiResult<K> 结果对象实例
	 */
	public static<K> GiResult<K> getResult(Class<K> resultType){
		return  GtcSpHelper.load(GiResultConfig.class).getResult(resultType);
	}

}
