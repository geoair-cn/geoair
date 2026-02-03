package cn.geoair.gtc.base.bean;
import java.lang.reflect.Type;
import java.util.Map;

public interface GiBeanFactory {

	/**
	 * 根据name获取一个bean
	 * @param name 要查询的bean的名称<br>the name of the bean to retrieve
	 * @return 一个bean的实例<br>an instance of the bean
	 * @throws GirNoSuchBeanException 如果没有bean是这个name<br> if there is no bean with the specified name
	 * @throws GirBeanException 如果bean不能被获得<br> if the bean could not be obtained
	 */
	Object getBean(String name) throws GirBeanException;

	/**
	 * 根据name获取一个bean 并返回指定的类型
	 * @param <T> 泛型类型
	 * @param name 要查询的bean的名称<br>the name of the bean to retrieve
	 * @param requiredType 匹配的类或接口（不能为null）<br>the required type of the bean
	 * @return 相应的bean实例<br>an instance of the bean
	 * @throws GirNoSuchBeanException 找不到类型的bean抛出异常<br> if there is no such bean definition
	 * @throws GirBeanNotOfRequiredTypeException 如果bean和类型不匹配抛出异常<br> if the bean is not of the required type
	 * @throws GirBeanException 如果bean不能被创建<br> if the bean could not be created
	 */
	<T> T getBean(String name, Class<T> requiredType) throws GirBeanException;

	/**
	 * 返回指定bean的实例，该实例可以是共享的或独立的。允许指定显式构造函数参数/工厂方法参数，重写bean定义中指定的默认参数（如果有的话）。
	 * <br>Return an instance, which may be shared or independent, of the specified bean.
	 * <br>Allows for specifying explicit constructor arguments / factory method arguments,
	 * <br>overriding the specified default arguments (if any) in the bean definition.
	 * @param name 要查询的bean的名称<br>the name of the bean to retrieve
	 * @param args 指定显式构造函数参数/工厂方法参数<br>arguments to use when creating a bean instance using explicit arguments (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @return 相应的bean实例<br>an instance of the bean
	 * @throws GirNoSuchBeanException 找不到类型的bean抛出异常<br> if there is no such bean definition
	 * @throws GirBeanDefinitionStoreException 参数传递之后bean不是一个prototype类型时抛出异常<br> if arguments have been given but the affected bean isn't a prototype
	 * @throws GirBeanException bean不能被创建抛出异常<br> if the bean could not be created
	 */
	Object getBean(String name, Object... args) throws GirBeanException;

	/**
	 * 返回指定bean的实例，该实例可以是共享的或独立的。允许指定显式构造函数参数/工厂方法参数，重写bean定义中指定的默认参数（如果有的话）。
	 * <br>Return an instance, which may be shared or independent, of the specified bean.
	 * <br>Allows for specifying explicit constructor arguments / factory method arguments,
	 * <br>overriding the specified default arguments (if any) in the bean definition.
	 * @param <T> 泛型类型
	 * @param requiredType 匹配的类或接口（不能为null）<br>type the bean must match; can be an interface or superclass
	 * @param args 指定显式构造函数参数/工厂方法参数<br>arguments to use when creating a bean instance using explicit arguments (only applied when creating a new instance as opposed to retrieving an existing one)
	 * @return 相应的bean实例<br>an instance of the bean
	 * @throws GirNoSuchBeanException 找不到类型的bean抛出异常<br>if there is no such bean definition
	 * @throws GirBeanDefinitionStoreException 参数传递之后bean不是一个prototype类型时抛出异常<br>if arguments have been given but the affected bean isn't a prototype
	 * @throws GirBeanException bean不能被创建抛出异常<br>if the bean could not be created
	 */
	<T> T getBean(Class<T> requiredType, Object... args) throws GirBeanException;

	/**
	 * 返回一个与给定对象类型的bean实例（包括子类）
	 * @param <T> 泛型类型
	 * @param requiredType 匹配的类或接口（不能为null）<br>the required type of the bean
	 * @return 相应的bean实例<br>an instance of the bean
	 * @throws GirNoSuchBeanException 找不到抛出异常<br>if no bean of the required type is found
	 * @throws GirNoUniqueBeanException 找到多个抛出异常<br>if more than one bean of the required type is found
	 * @throws GirBeanException bean创建异常<br>if the bean could not be created
	 */
	<T> T getBean(Class<T> requiredType) throws GirBeanException;

	/**
	 * 返回一个与给定对象类型+泛型匹配的bean实例（包括子类）
	 * @param <T> 泛型类型
	 * @param requiredType 匹配的类或接口（不能为null）<br>the required type of the bean
	 * @param genericType 泛型类型 手动创建  new Type[]{X.class,Y.class}<br>generic types for the bean
	 * @return 相应的bean实例<br>an instance of the bean
	 * @throws GirNoSuchBeanException 找不到抛出异常<br>if no bean of the required type is found
	 * @throws GirNoUniqueBeanException 找到多个抛出异常<br>if more than one bean of the required type is found
	 * @throws GirBeanException bean创建异常<br>if the bean could not be created
	 */
	<T> T getBean(Class<T> requiredType,Type[] genericType) throws GirBeanException;

	/**
	 * 返回与给定对象类型+泛型匹配的bean实例（包括子类）
	 * @param <T> 泛型类型
	 * @param clazz 匹配的类或接口，或者为所有具体bean键入｛@code null｝<br>the class or interface to match, or {@code null} for all concrete beans
	 * @param genericType 泛型类型 手动创建  new Type[]{X.class,Y.class}<br>generic types for the bean
	 * @return 带有匹配bean的Map，包含bean名称键和相应的bean实例作为值<br>a Map with the matching beans, containing the bean names as keys and the corresponding bean instances as values
	 * @throws GirBeanException 如果bean不能被创建<br>if a bean could not be created
	 */
	<T> Map<String, T> getBeans(Class<T> clazz,Type[] genericType) throws GirBeanException;

	/**
	 * 返回与给定对象类型匹配的bean实例（包括子类）
	 * <br>Return the bean instances that match the given object type (including subclasses)
	 * @param <T> 泛型类型
	 * @param clazz 匹配的类或接口，或者为所有具体bean键入｛@code null｝<br>type the class or interface to match, or {@code null} for all concrete beans
	 * @return 带有匹配bean的Map，包含bean名称键和相应的bean实例作为值<br>a Map with the matching beans, containing the bean names as keys and the corresponding bean instances as values
	 * @throws GirBeanException 如果bean不能被创建<br>if a bean could not be created
	 */
	<T> Map<String, T> getBeans(Class<T> clazz) throws GirBeanException;

	/**
	 * 这个bean工厂是否包含bean定义或外部注册的单例具有给定名称的实例？
	 * <br>Does this bean factory contain a bean definition or externally registered singleton instance with the given name?
	 * @param name 要查询的bean的名称<br>the name of the bean to query
	 * @return 是否存在具有给定名称的bean<br>whether a bean with the given name is present
	 */
	boolean containsBean(String name);

	/**
	 * 检查bean是否是个单例模式;注意：返回false并不表示一定每次都是新实例，如果确定是不是每次都是新实例，用isPrototype做检查
	 * <br>Is this bean a shared singleton? That is, will {@link #getBean} always return the same instance?
	 * <br> Note: This method returning {@code false} does not clearly indicate
	 * independent instances. It indicates non-singleton instances, which may correspond
	 * to a scoped bean as well. Use the {@link #isPrototype} operation to explicitly
	 * check for independent instances.
	 * @param name 要查询的bean的名称<br> the name of the bean to query
	 * @return 此bean是否对应于单例实例<br>whether this bean corresponds to a singleton instance
	 * @throws GirNoSuchBeanException 如果没有具有给定名称的bean<br>if there is no bean with the given name
	 */
	boolean isSingleton(String name) throws GirNoSuchBeanException;

	/**
	 * 检查bean是不是每次都创建新实例，相对单态而言;注意，当返回false时，并不是表示它是一个单一实例；
	 * <br>Is this bean a prototype? That is, will {@link #getBean} always return independent instances?
	 * <br>Note: This method returning {@code false} does not clearly indicate
	 * a singleton object. It indicates non-independent instances, which may correspond
	 * to a scoped bean as well. Use the {@link #isSingleton} operation to explicitly
	 * check for a shared singleton instance.
	 * @param name 要查询的bean的名称<br>the name of the bean to query
	 * @return 此bean是否始终每次都创建新实例<br>whether this bean will always deliver independent instances
	 * @throws GirNoSuchBeanException 如果没有具有给定名称的bean<br>if there is no bean with the given name
	 */
	boolean isPrototype(String name) throws GirNoSuchBeanException;

	/**
	 * 检查具有给定名称的bean是否与指定类型匹配;
	 * <br>Check whether the bean with the given name matches the specified type.
	 * <br>更具体地说，检查是否为给定名称调用了{@link #getBean} <br>More specifically, check whether a {@link #getBean} call for the given name
	 * <br>将返回可分配给指定目标类型的对象<br> would return an object that is assignable to the specified target type.
	 * @param name 要查询的bean的名称<br> the name of the bean to query
	 * @param typeToMatch 要匹配的类型<br> the type to match against
	 * @return true 如果bean类型匹配<br> if the bean type matches
	 * <br>false 如果不匹配或无法确定<br> if it doesn't match or cannot be determined yet
	 * @throws GirNoSuchBeanException  如果没有具有给定名称的bean<br> if there is no bean with the given name
	 */
	boolean isTypeMatch(String name,Class<?> typeToMatch) throws GirNoSuchBeanException;

	/**
	 * 确定具有给定名称的bean的类型;
	 * <br>Determine the type of the bean with the given name. More specifically,
	 * <br>确定 {@link #getBean} 将为给定名称返回的对象类型
	 * <br>determine the type of object that {@link #getBean} would return for the given name.
	 * @param name 要查询的bean的名称<br> the name of the bean to query
	 * @return 返回bean的类型，如果无法确定，则返回 {@code null}<br> the type of the bean, or {@code null} if not determinable
	 * @throws GirNoSuchBeanException 如果没有具有给定名称的bean<br>if there is no bean with the given name
	 */
	Class<?> getType(String name) throws GirNoSuchBeanException;

	/**
	 * 返回给定bean名称的别名（如果有的话）;
	 * <br>Return the aliases for the given bean name, if any.
	 * <br>当在{@link #getBean} 调用中使用时，所有这些别名都指向同一个bean<br>All of those aliases point to the same bean when used in a {@link #getBean} call.
	 * <br>如果给定的名称是别名，则对应的原始bean名称<br>If the given name is an alias, the corresponding original bean name
	 * <br>和其他别名（如果有的话），将返回原始bean名称是阵列中的第一个元素<br>and other aliases (if any) will be returned, with the original bean name being the first element in the array.
	 * @param name 要检查别名的bean名称<br>the bean name to check for aliases
	 * @return 返回别名，如果没有，则返回空数组<br>the aliases, or an empty array if none
	 */
	String[] getAliases(String name);

	/**
	 * 注册一个bean的类型
	 * @param name 注册的名称<br>the name of the bean
	 * @param beanClass 注册的bean的类型<br>the class of the bean
	 * @throws GirBeanDefinitionStoreException 注册失败异常<br>if the registration fails
	 */
	void register(String name,Class<?> beanClass) throws GirBeanDefinitionStoreException;

	/**
	 * 注册一个bean的类型
	 * @param name 注册的名称<br>the name of the bean
	 * @param beanClass 注册的bean的类型<br>the class of the bean
	 * @param singleton 是否是单例<br>whether the bean should be a singleton
	 * @throws GirBeanDefinitionStoreException 注册失败异常<br>if the registration fails
	 */
	void register(String name, Class<?> beanClass,boolean singleton) throws GirBeanDefinitionStoreException;
}
