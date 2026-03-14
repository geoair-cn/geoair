package cn.geoair.base.lang.lambda;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.lang.invoke.MethodHandleInfo;

/**
 * 当前类是 {@link java.lang.invoke.SerializedLambda } 的一个镜像
 * <p>
 */
@SuppressWarnings("all")
public class GkSerializedLambda implements Serializable {

	private static final long serialVersionUID = 8025925345765570181L;

	/**
	 * capturingClass "捕获类"，当前的Lambda表达式出现的所在类 functionalInterfaceClass
	 * 名称，并且以"/"分隔，返回的Lambda对象的静态类型 functionalInterfaceMethodName 函数式接口方法名称
	 * functionalInterfaceMethodSignature 函数式接口方法签名（其实是参数类型和返回值类型，如果使用了泛型则是擦除后的类型）
	 * implClass 名称，并且以"/"分隔，持有该函数式接口方法的实现方法的类型（实现了函数式接口方法的实现类） implMethodName
	 * 函数式接口方法的实现方法名称 implMethodSignature 函数式接口方法的实现方法的方法签名（实是参数类型和返回值类型）
	 * instantiatedMethodType 用实例类型变量替换后的函数式接口类型 capturedArgs Lambda捕获的动态参数 implMethodKind
	 * 实现方法的MethodHandle类型
	 */
	private Class<?> capturingClass;

	private String functionalInterfaceClass;

	private String functionalInterfaceMethodName;

	private String functionalInterfaceMethodSignature;

	private String implClass;

	private String implMethodName;

	private String implMethodSignature;

	private int implMethodKind;

	private String instantiatedMethodType;

	private Object[] capturedArgs;

	public static GkSerializedLambda extract(Serializable serializable) {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(serializable);
			oos.flush();
			try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())) {
				@Override
				protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
					Class<?> clazz = super.resolveClass(desc);
					return clazz == java.lang.invoke.SerializedLambda.class ? GkSerializedLambda.class : clazz;
				}

			}) {
				return (GkSerializedLambda) ois.readObject();
			}
		}
		catch (IOException | ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Get the class that captured this lambda.
	 * @return the class that captured this lambda
	 */
	public Class<?> getCapturingClass() {
		return capturingClass;
	}

	/**
	 * Get the name of the class that captured this lambda.
	 * @return the name of the class that captured this lambda
	 */
	public String getCapturingClassName() {
		return capturingClass.getName().replace('.', '/');
	}

	/**
	 * Get the name of the invoked type to which this lambda has been converted
	 * @return the name of the functional interface class to which this lambda has been
	 * converted
	 */
	public String getFunctionalInterfaceClass() {
		return functionalInterfaceClass;
	}

	/**
	 * Get the name of the primary method for the functional interface to which this
	 * lambda has been converted.
	 * @return the name of the primary methods of the functional interface
	 */
	public String getFunctionalInterfaceMethodName() {
		return functionalInterfaceMethodName;
	}

	/**
	 * Get the signature of the primary method for the functional interface to which this
	 * lambda has been converted.
	 * @return the signature of the primary method of the functional interface
	 */
	public String getFunctionalInterfaceMethodSignature() {
		return functionalInterfaceMethodSignature;
	}

	/**
	 * Get the name of the class containing the implementation method.
	 * @return the name of the class containing the implementation method
	 */
	public String getImplClass() {
		return implClass;
	}

	/**
	 * Get the name of the implementation method.
	 * @return the name of the implementation method
	 */
	public String getImplMethodName() {
		return implMethodName;
	}

	/**
	 * Get the signature of the implementation method.
	 * @return the signature of the implementation method
	 */
	public String getImplMethodSignature() {
		return implMethodSignature;
	}

	/**
	 * Get the method handle kind (see {@link MethodHandleInfo}) of the implementation
	 * method.
	 * @return the method handle kind of the implementation method
	 */
	public int getImplMethodKind() {
		return implMethodKind;
	}

	/**
	 * Get the signature of the primary functional interface method after type variables
	 * are substituted with their instantiation from the capture site.
	 * @return the signature of the primary functional interface method after type
	 * variable processing
	 */
	public final String getInstantiatedMethodType() {
		return instantiatedMethodType;
	}

	/**
	 * Get the count of dynamic arguments to the lambda capture site.
	 * @return the count of dynamic arguments to the lambda capture site
	 */
	public int getCapturedArgCount() {
		return capturedArgs.length;
	}

	/**
	 * Get a dynamic argument to the lambda capture site.
	 * @param i the argument to capture
	 * @return a dynamic argument to the lambda capture site
	 */
	public Object getCapturedArg(int i) {
		return capturedArgs[i];
	}

}
