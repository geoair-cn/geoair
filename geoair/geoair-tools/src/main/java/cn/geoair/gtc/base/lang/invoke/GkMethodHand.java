package cn.geoair.gtc.base.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import cn.geoair.gtc.base.tool.GkConsole;
import cn.geoair.gtc.base.util.GutilClass;
import cn.geoair.gtc.base.util.GutilReflection;
import cn.geoair.gtc.base.util.GutilStr;

/**
 * @author Ray
 */
public class GkMethodHand {

	private static String formatMethodDesc(Method method) {
		StringBuffer sb = new StringBuffer().append(method.getDeclaringClass().getName()).append(".").append(method.getName()).append("(");
		for(Class<?> pt : method.getParameterTypes()) {
			sb.append(pt.getSimpleName()).append(",");
		}
		if(sb.lastIndexOf(",") == sb.length()-1) {
			return sb.replace(sb.length() - 1, sb.length(), ")").toString();
		}else {
			return sb.append(")").toString();
		}
	}
	private static String getMethodHandleId(Method method) {
		GaMethodHandDefine conf = method.getAnnotation(GaMethodHandDefine.class);
		if(conf != null && !GutilStr.isBlank(conf.id())) {
			return conf.id();
		}
		return formatMethodDesc(method);
	}


	private static String getCallerIdByStackTraceElement(StackTraceElement ste) {
		return new StringBuffer().append(ste.getClassName())
				.append(".").append(ste.getMethodName()).append("(").append(ste.getFileName())
				.append(":").append(ste.getLineNumber()).append(")").toString();
	}


	private static StackTraceElement getCallerStackTraceElement() {
		StackTraceElement[] stes = Thread.currentThread().getStackTrace();
		for(StackTraceElement ste:stes) {
			if(!ste.getClassName().equals(java.lang.Thread.class.getName()) && !ste.getClassName().equals(GkMethodHand.class.getName())) {
				return ste;
			}
		}
		return null;
	}
	private static String getCallerId() {
		return getCallerIdByStackTraceElement(getCallerStackTraceElement());
	}
	private static boolean matchType(Object[] args,Class<?>[] types) {
		int len = args.length;
		if(len == types.length) {
			for(int i=0;i<len;i++) {
				if(args[i] != null && !GutilClass.resolvePrimitiveIfNecessary(types[i]).isAssignableFrom(args[i].getClass())) {
					return false;
				}
			}
			return true;
		}
		return false;
	}



	//-------------------setup

	private static Map<String,GkMethodHand> setups = new ConcurrentHashMap<>();//

	private synchronized static GkMethodHand setup(String handleId) {
		if(setups.containsKey(handleId)) {
			return setups.get(handleId);
		}
		GkMethodHand hand = new GkMethodHand(handleId);
		setups.put(handleId, hand);
		return hand;
	}

	//-------------------define




	private class Define {

		private String defineId;

		private String expectClassName;

		private String expectMethodName;

		private Define(String defineId,String expectClassName,String expectMethodName) {
			this.defineId = defineId;
			this.expectClassName = expectClassName;
			this.expectMethodName = expectMethodName;
		}
		private Class<?> getExpectClass(){
			if(!GutilStr.isEmpty(expectClassName)) {
				try {
					return Class.forName(expectClassName,true,Thread.currentThread().getContextClassLoader());
				} catch (Exception e) {}
			}
			return null;
		}
		private Method getExpectMethod(Class<?>[] parameterTypes){
			if(!GutilStr.isEmpty(expectMethodName)) {
				Class<?> clz = this.getExpectClass();
				if(clz == null) {
					return null;
				}
				try {
					return clz.getDeclaredMethod(expectMethodName, parameterTypes);
				} catch (Exception e) {}
			}
			return null;
		}
		public void print() {
			GkConsole.log("    {");
			GkConsole.log("      defineId:at " + defineId);
			GkConsole.log("      expectClassName:" + expectClassName);
			GkConsole.log("      expectMethodName:" + expectMethodName);
			GkConsole.log("    }");
		}
	}

	/**
	 * 对某个类进行方法句柄定义的注册
	 * @param clz
	 */
	public static void defineFromClass(Class<?> clz) {
		Method[] methods = clz.getDeclaredMethods();
		for(Method method: methods) {
			GaMethodHandDefine conf = method.getAnnotation(GaMethodHandDefine.class);
			if(conf != null) {
				defineFromMethod(method);
			}
		}
	}
	/**
	 * 对某个方法进行定义
	 * @param method
	 * @return
	 */
	public static GkMethodHand defineFromMethod(Method method) {
		GaMethodHandDefine conf = method.getAnnotation(GaMethodHandDefine.class);
		if(conf == null) {
			return GkMethodHand.define(getMethodHandleId(method),method.getReturnType(),method.getParameterTypes());
		}else {
			return GkMethodHand.define(getMethodHandleId(method),GutilStr.emptyToDefault(conf.expectClassName(), null),GutilStr.emptyToDefault(conf.expectMethodName(), method.getName()),method.getReturnType(),method.getParameterTypes());
		}
	}

	public static GkMethodHand define(String handleId,Class<?> returnType,Class<?>... parameterTypes) {
		return define(handleId,null,null,returnType,parameterTypes);
	}
	private static GkMethodHand define(String handleId,String expectClassName,String expectMethodName,Class<?> returnType,Class<?>... parameterTypes) {
		GkMethodHand rh = GkMethodHand.setup(handleId);
		rh.addDefine(MethodType.methodType(returnType, parameterTypes),expectClassName,expectMethodName);
		return rh;
	}


	//-------------------impl

	public static enum ImplStatus{
		empty,
		expected,
		appointed,
		appointed_expected,
		other
	}

	private class Impl {

		private String implId;

		private Method method;

		private MethodHandle methodHandle;

		private GaMethodHandImpl.ImplType implType;

		private boolean isInitExpect;

		private boolean isAppoint;

		public boolean isExpect() {
			if(isInitExpect)return true;
			for(Impl imp : impls.values()) {
				if(imp != this) {
					if(imp.isInitExpect) {
						if(Objects.equals(imp.method, this.method) || Objects.equals(imp.methodHandle, this.methodHandle)) {
							return true;
						}
					}
				}
			}
			return false;
		}

		private Impl(String implId, Method method, MethodHandle handle, GaMethodHandImpl.ImplType implType) {

			this.implId = implId;
			this.methodHandle = handle;
			this.implType = implType;
			if(method != null) {
				this.method = method;
				GutilReflection.makeAccessible(method);
				try {
					this.methodHandle = MethodHandles.lookup().unreflect(method);
				} catch (Exception e) {
					throw new IllegalStateException("GkMethodHand.Impl(Method)发生错误:"+getMethodHandleId(method),e);
				}
			}
		}

		public void print() {
			GkConsole.log("    {");
			GkConsole.log("      implId:at " + implId);
			GkConsole.log("      isInitExpect:" + isInitExpect);
			GkConsole.log("      isAppoint:" + isAppoint);
			GkConsole.log("      implType:" + implType);
			GkConsole.log("      method:" + method.getName());
			GkConsole.log("      methodHandle:" + methodHandle.toString());
			GkConsole.log("    }");
		}
	}

	public static void implFromClass(Class<?> clz) {
		Method[] methods = clz.getDeclaredMethods();
		for(Method method: methods) {
			if(method.getAnnotation(GaMethodHandImpl.class) != null)
			implFromMethod(method);
		}
	}
	public static void implFromMethod(Method method) {
		GaMethodHandImpl conf = method.getAnnotation(GaMethodHandImpl.class);
		if(conf != null){
			String implId = conf.id();
			if(GutilStr.isEmpty(implId)) {
				Class<?> implClass = conf.implClass();
				Method md = null;
				try {
					md = implClass.getDeclaredMethod(GutilStr.emptyToDefault(conf.implMethod(), method.getName()), method.getParameterTypes());
				} catch (Exception e) {
					throw new IllegalStateException("GkMethodHand.implFromMethod(Method)发生错误,所需要实现的类中没有找到方法:"+method.toString(),e);
				}
				implId = getMethodHandleId(md);
			}
			GkMethodHand.impl(implId, method,conf.type());
		}else {
			throw new IllegalStateException("GkMethodHand.implFromMethod(Method)发生错误,方法必须带有GaMethodHandImpl注解,"+method.toString());
		}
	}

	public static void impl(String handleId, Method method, GaMethodHandImpl.ImplType implType) {
		GkMethodHand.setup(handleId).addImpl(method,null,implType);
	}

	public static void appoint(String handleId,MethodHandle methodHandle) {
		GkMethodHand rmh = GkMethodHand.setup(handleId);
		Impl imp = rmh.addImpl(null,methodHandle, GaMethodHandImpl.ImplType.cover);
		imp.isAppoint = true;
		rmh.impl = imp;
	}


	/**
	 * 打印所有句柄到控制台
	 * @param flag 打印标记字符串
	 */
	public static void prints(String flag) {
		for(Entry<String, GkMethodHand> ent:setups.entrySet()) {
			GkConsole.log("GkMethodHand print --------------------------------------------------start-------------------------------------------------------"+flag);
			ent.getValue().print();
			GkConsole.log("GkMethodHand print ---------------------------------------------------end------------------------------------------------------"+flag);
		}
	}

	public static GkMethodHand get(String handleId) {
		return setups.get(handleId);
	}

	@SuppressWarnings("unchecked")
	public static<T> T invokeHandle(String handleId,Class<T> returnType,Object... args){
		return (T)GkMethodHand.get(handleId).invoke(args);
	}

	public static Object invokeHandle(String handleId,Object... args){
		return GkMethodHand.get(handleId).invoke(args);
	}

	private static Map<String,String> selfs = new ConcurrentHashMap<>();//


	/**
	 * 执行静态方法句柄，用在静态方法
	 * @param args
	 * @return
	 */
	public static Object invokeSelf(Object... args){
		StackTraceElement ste = getCallerStackTraceElement();
		String selfId = getCallerIdByStackTraceElement(ste);
		if(selfs.containsKey(selfId)){
			String handleId = selfs.get(selfId);
			return setups.get(handleId).invoke(args);
		}
        Class<?> clazz = null;
		try {
			clazz = Class.forName(ste.getClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
        Method[] methods = clazz.getDeclaredMethods();
        for(Method md : methods) {
        	if(!md.getName().equals(ste.getMethodName())){
        		continue;
        	}
        	if(matchType(args,md.getParameterTypes())){
        		String testId = getMethodHandleId(md);
        		GkMethodHand mh = null;
        		if(setups.containsKey(testId)) {
        			mh = defineFromMethod(md);
        			mh = setups.get(testId);
        		}else {
        			mh = defineFromMethod(md);
        		}
        		selfs.put(selfId, testId);
				Object rtn = mh.invoke(args);

    			return rtn;
        	}
        }
		throw new IllegalStateException(GutilStr.format("GkMethodHand.invokeSelf(Object...)执行失败,可能的原因是方法参数个数与声明方法参数个数不一致 at {}",selfId));
	}


	private String id;

	private MethodType methodType;

	private Map<String,Define> defines = new HashMap<>();

	private Map<String,Impl> impls = new HashMap<>();

	private Impl impl = null;

	private String setupCallerId;

	public void print() {
		GkConsole.log("GkMethodHand{");
		GkConsole.log("  id:" + id);
		GkConsole.log("  setupCallerId: at " + setupCallerId);
		GkConsole.log("  methodType:" + methodType.toString());
		GkConsole.log("  impl:" + (impl==null?null:impl.implId));
		GkConsole.log("  defines:[");
		for(Define df : defines.values()) {
			df.print();
		}
		GkConsole.log("  ]");

		GkConsole.log("  impls:[");
		for(Impl ip : impls.values()) {
			ip.print();
		}
		GkConsole.log("  ]");
		GkConsole.log("}");
	}


	private GkMethodHand(String id) {
		this.id = id;
		this.setupCallerId = getCallerId();
	}
	private String getMyCallerId() {
		return getCallerId() + this.getId();
	}
	public String getId() {
		return id;
	}
	public ImplStatus getImplStatus() {
		if(impl == null) {
			return ImplStatus.empty;
		}
		if(impl.isExpect()) {
			if(impl.isAppoint) {
				return ImplStatus.appointed_expected;
			}
			return ImplStatus.expected;
		}else {
			if(impl.isAppoint) {
				return ImplStatus.appointed;
			}
		}
		return ImplStatus.other;
	}
	public MethodType getMethodType() {
		return methodType;
	}

	public Object invoke(Object... args){
		MethodHandle mh = this.getMethodHandle();

		if(mh == null) {
			throw new IllegalStateException("GkMethodHand("+this.getId()+").invoke(Object...)没有对应的实现 at "+getCallerId());
		}

		if(mh.isVarargsCollector()) {
			Object[] lastArray = (Object[])args[args.length - 1];
			if(lastArray != null && lastArray.length > 0) {
				Object[] argsVal = new Object[args.length + lastArray.length - 1];
				System.arraycopy(args,0,argsVal,0,args.length-1);
				System.arraycopy(lastArray,0,argsVal,args.length-1,lastArray.length);
				try {
					return mh.invokeWithArguments(argsVal);
				} catch (Throwable e) {
					throw new IllegalStateException("GkMethodHand("+this.getId()+").invoke(Object...)发生错误 at "+getCallerId(),e);
				}
			}
		}
		try {
			int len = args.length;
			if(len == 0) {
				return mh.invoke();
			}else {
				return mh.invokeWithArguments(args);
			}
		} catch (Throwable e) {
			throw new IllegalStateException("GkMethodHand("+this.getId()+").invoke(Object...)发生错误 at "+getCallerId(),e);
		}

		/*
		try {
			Object res = null;
			int len = args.length;
			switch(len) {
				case 0: res = mh.invoke();break;
				case 1: res = mh.invoke(args[0]);break;
				case 2: res = mh.invoke(args[0],args[1]);break;
				case 3: res = mh.invoke(args[0],args[1],args[2]);break;
				case 4: res = mh.invoke(args[0],args[1],args[2],args[3]);break;
				case 5: res = mh.invoke(args[0],args[1],args[2],args[3],args[4]);break;
				case 6: res = mh.invoke(args[0],args[1],args[2],args[3],args[4],args[5]);break;
				case 7: res = mh.invoke(args[0],args[1],args[2],args[3],args[4],args[5],args[6]);break;
				default:res = mh.invoke(args);
			}
			return res;

		} catch (Throwable e) {
			throw new  gtcException(" gtcMethodHand("+this.getId()+").invoke(Object...)发生错误 at "+getCallerId(),e);
		}
		*/
	}


	private Impl addImpl(Method method, MethodHandle methodHandle, GaMethodHandImpl.ImplType implType) {
		String key = getMyCallerId();
		if(!impls.containsKey(key)) {
			Impl imp = new Impl(key,method,methodHandle,implType);
			impls.put(key, imp);
			setMethodType(imp.methodHandle.type());
			if(impl == null) {
				impl = imp;
			}else if(!impl.isAppoint) {
				if(implType == GaMethodHandImpl.ImplType.cover) {
					impl = imp;
				}else if(implType == GaMethodHandImpl.ImplType.expectfirst){
					if(!impl.isExpect()) {
						impl = imp;
					}
				}
			}
			return imp;
		}
		return impls.get(key);
	}


	private boolean addExpect() {
		for(Define df : defines.values()) {
			Method exM = df.getExpectMethod(methodType.parameterArray());
			if(exM != null) {
				Impl imp = addImpl(exM,null, GaMethodHandImpl.ImplType.expectfirst);
				imp.isInitExpect = true;
				return true;
			}
		}
		return false;
	}

	private boolean addDefine(MethodType methodType,String expectClassName,String expectMethodName) {
		String key = getMyCallerId();
		if(!defines.containsKey(key)) {
			Define define = new Define(key,expectClassName,expectMethodName);
			defines.put(key, define);
			setMethodType(methodType);
			return true;
		}
		return false;
	}
	private GkMethodHand setMethodType(MethodType methodType) {
		if(this.methodType == null) {
			this.methodType = methodType;
		}else if(!this.methodType.equals(methodType)) {
			throw new IllegalStateException("GkMethodHand.setMethodType(MethodType)设置了不同的methodType at "+getCallerId());
		}
		return this;
	}

	public MethodHandle getMethodHandle(){
		if(impl == null ||!impl.isInitExpect) {
			addExpect();
		}
		if(impl == null) {
			return null;
		}
		return impl.methodHandle;
	}
}



