package cn.geoair.gtc.base.lang.lambda;

import java.lang.invoke.MethodHandleInfo;

/**
 * Lambda 信息
 * <p>
 */
public interface GkfLambdaMeta {

	/**
	 * 获取 lambda 表达式实现方法的名称
	 * @return lambda 表达式对应的实现方法名称
	 */
	String getImplMethodName();

	/**
	 * 实例化该方法的类
	 * @return 返回对应的类名称
	 */
	Class<?> getInstantiatedClass();

	/**
	 * Get the class that captured this lambda.
	 * @return the class that captured this lambda
	 */
	public Class<?> getCapturingClass();

	/**
	 * Get the name of the invoked type to which this lambda has been converted
	 * @return the name of the functional interface class to which this lambda has been
	 * converted
	 */
	public String getFunctionalInterfaceClass();

	/**
	 * Get the name of the primary method for the functional interface to which this
	 * lambda has been converted.
	 * @return the name of the primary methods of the functional interface
	 */
	public String getFunctionalInterfaceMethodName();

	/**
	 * Get the signature of the primary method for the functional interface to which this
	 * lambda has been converted.
	 * @return the signature of the primary method of the functional interface
	 */
	public String getFunctionalInterfaceMethodSignature();

	/**
	 * Get the name of the class containing the implementation method.
	 * @return the name of the class containing the implementation method
	 */
	public String getImplClass();

	/**
	 * Get the name of the implementation method.
	 * @return the name of the implementation method
	 */
	// public String getImplMethodName();

	/**
	 * Get the signature of the implementation method.
	 * @return the signature of the implementation method
	 */
	public String getImplMethodSignature();

	/**
	 * Get the method handle kind (see {@link MethodHandleInfo}) of the implementation
	 * method.
	 * @return the method handle kind of the implementation method
	 */
	public int getImplMethodKind();

	/**
	 * Get the signature of the primary functional interface method after type variables
	 * are substituted with their instantiation from the capture site.
	 * @return the signature of the primary functional interface method after type
	 * variable processing
	 */
	public String getInstantiatedMethodType();

	/**
	 * Get the count of dynamic arguments to the lambda capture site.
	 * @return the count of dynamic arguments to the lambda capture site
	 */
	public int getCapturedArgCount();

	/**
	 * Get a dynamic argument to the lambda capture site.
	 * @param i the argument to capture
	 * @return a dynamic argument to the lambda capture site
	 */
	public Object getCapturedArg(int i);

}
