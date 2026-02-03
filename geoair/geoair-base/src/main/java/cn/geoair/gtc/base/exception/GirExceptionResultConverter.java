package cn.geoair.gtc.base.exception;


import java.util.IdentityHashMap;
import java.util.Map;
import cn.geoair.gtc.base.data.GirValidateException;
import cn.geoair.gtc.base.data.result.GiResult;
import cn.geoair.gtc.base.data.result.GirEmAlertType;
//import  com.gtc.base.user. gtcNoLoginException;
//import  com.gtc.base.user.permission. gtcPermissionException;

public abstract class GirExceptionResultConverter {


	private static final Map<Class<? extends Exception>,Integer> primitiveExceptionCodeMap = new IdentityHashMap<Class<? extends Exception>,Integer>(8);


	static {
		primitiveExceptionCodeMap.put(Exception.class, 999);
		primitiveExceptionCodeMap.put( GirException.class, 998);
//		primitiveExceptionCodeMap.put( gtcNoLoginException.class, 399);
//		primitiveExceptionCodeMap.put( gtcPermissionException.class, 799);
		primitiveExceptionCodeMap.put( GirValidateException.class, 499);
	}



	public static void regExceptionCode(Class<? extends Exception> exCls,Integer code){
		primitiveExceptionCodeMap.put(exCls, code);
	}

	@SuppressWarnings("unchecked")
	public static Integer getExceptionCode(Class<? extends Exception> exCls){
		if(primitiveExceptionCodeMap.containsKey(exCls)) {
			return primitiveExceptionCodeMap.get(exCls);
		}else {
			return getExceptionCode((Class<? extends Exception>)exCls.getSuperclass());
		}
	}




	private static final Map<Class<? extends Exception>,Integer> primitiveExceptionAlertTypeMap = new IdentityHashMap<Class<? extends Exception>,Integer>(8);


	static {
		primitiveExceptionAlertTypeMap.put(Exception.class,  GirEmAlertType.不弹框0.value());
		primitiveExceptionAlertTypeMap.put( GirException.class,  GirEmAlertType.无需关闭的错误3.value());
//		primitiveExceptionAlertTypeMap.put( gtcNoLoginException.class,  GtcEmAlertType.无需关闭的错误3.value());
//		primitiveExceptionAlertTypeMap.put( gtcPermissionException.class,  GtcEmAlertType.无需关闭的提示1.value());
		primitiveExceptionAlertTypeMap.put( GirValidateException.class,  GirEmAlertType.无需关闭的提示1.value());
	}



	public static void regExceptionAlertType(Class<? extends Exception> exCls,Integer code){
		primitiveExceptionAlertTypeMap.put(exCls, code);
	}

	@SuppressWarnings("unchecked")
	public static Integer getExceptionAlertType(Class<? extends Exception> exCls){
		if(primitiveExceptionAlertTypeMap.containsKey(exCls)) {
			return primitiveExceptionAlertTypeMap.get(exCls);
		}else {
			return getExceptionAlertType((Class<? extends Exception>)exCls.getSuperclass());
		}
	}


	public static GiResult<Exception> convert(Exception ex){
		GiResult<Exception> res = GiResult.getResult(Exception.class);
		res.andAlertMsg(ex.getMessage());
		res.andValue(ex);
		res.andCode(getExceptionCode(ex.getClass()));
		res.andAlertType(getExceptionAlertType(ex.getClass()));
		return res;
	}

	/*
	public static GiResult<IllegalArgumentException> checkDataConvert(IllegalArgumentException ex){
		GiResult<IllegalArgumentException> res = GiResult.getResult(IllegalArgumentException.class);
		res.setAlertMsg(ex.getMessage());
		res.setValue(ex);
		res.setCode(899);
		res.setAlertType( gtcEmAlertType.无需关闭的提示1.value());
		return res;
	}
	*/

}
