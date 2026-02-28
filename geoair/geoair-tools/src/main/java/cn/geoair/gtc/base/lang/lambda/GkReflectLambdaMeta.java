package cn.geoair.gtc.base.lang.lambda;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Field;

import cn.geoair.gtc.base.util.GutilClass;
import cn.geoair.gtc.base.util.GutilReflection;

public class GkReflectLambdaMeta implements GkfLambdaMeta {

	private static final Field FIELD_CAPTURING_CLASS;

	static {
		Field fieldCapturingClass;
		try {
			Class<SerializedLambda> aClass = SerializedLambda.class;
			fieldCapturingClass = GutilReflection.setAccessible(aClass.getDeclaredField("capturingClass"));
		}
		catch (Throwable e) {
			// 解决高版本 jdk 的问题 gitee: https://gitee.com/baomidou/mybatis-plus/issues/I4A7I5
			e.printStackTrace();
			fieldCapturingClass = null;
		}
		FIELD_CAPTURING_CLASS = fieldCapturingClass;
	}

	private final SerializedLambda lambda;

	public GkReflectLambdaMeta(SerializedLambda lambda) {
		this.lambda = lambda;
	}

	@Override
	public String getImplMethodName() {

		return lambda.getImplMethodName();
	}

	public String getImplClass() {
		return lambda.getImplClass();
	}

	@Override
	public Class<?> getInstantiatedClass() {
		String instantiatedMethodType = lambda.getInstantiatedMethodType();
		String instantiatedType = instantiatedMethodType.substring(2, instantiatedMethodType.indexOf(';')).replace('/',
				'.');
		try {
			return GutilClass.forName(instantiatedType, getCapturingClassClassLoader());
		}
		catch (ClassNotFoundException | LinkageError e) {
			e.printStackTrace();
		}
		return null;
	}

	private ClassLoader getCapturingClassClassLoader() {
		// 如果反射失败，使用默认的 classloader
		if (FIELD_CAPTURING_CLASS == null) {
			return null;
		}
		try {
			return ((Class<?>) FIELD_CAPTURING_CLASS.get(lambda)).getClassLoader();
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Class<?> getCapturingClass() {
		try {
			return GutilClass.forName(lambda.getCapturingClass().replace('/', '.'), null);
		}
		catch (ClassNotFoundException | LinkageError e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public String getFunctionalInterfaceClass() {

		return lambda.getFunctionalInterfaceClass();
	}

	@Override
	public String getFunctionalInterfaceMethodName() {

		return lambda.getFunctionalInterfaceMethodName();
	}

	@Override
	public String getFunctionalInterfaceMethodSignature() {

		return lambda.getFunctionalInterfaceMethodSignature();
	}

	@Override
	public String getImplMethodSignature() {

		return lambda.getImplMethodSignature();
	}

	@Override
	public int getImplMethodKind() {

		return lambda.getImplMethodKind();
	}

	@Override
	public String getInstantiatedMethodType() {

		return lambda.getInstantiatedMethodType();
	}

	@Override
	public int getCapturedArgCount() {

		return lambda.getCapturedArgCount();
	}

	@Override
	public Object getCapturedArg(int i) {

		return lambda.getCapturedArg(i);
	}

}
