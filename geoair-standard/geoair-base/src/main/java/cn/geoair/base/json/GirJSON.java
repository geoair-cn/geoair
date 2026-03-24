package cn.geoair.base.json;

import java.io.Serializable;
import java.lang.reflect.Type;

import cn.geoair.base.lang.GkTypeReference;
import cn.geoair.base.lang.invoke.GaMethodHandDefine;
import cn.geoair.base.lang.invoke.GkMethodHand;

/**
 * 不依托于具体框架实现json的转换和查找
 *
 * @author Ray
 */
public interface GirJSON extends Cloneable, Serializable /* , Map<String, Object> */ {

	/**
	 * json字符串转 gtcJSON对象
	 * @param json
	 * @return
	 */
	@GaMethodHandDefine(expectClassName = "cn.geoair.spi.json.Json4Gir")
	public static GirJSON toJson(Object json) {
		Object o = GkMethodHand.invokeSelf(json);
		return (GirJSON) o;
	}

	/**
	 * 通过表达式获取JSON中嵌套的对象<br>
	 *
	 * <ol>
	 * <li>.表达式，可以获取Bean对象中的属性（字段）值或者Map中key对应的值
	 * <li>[]表达式，可以获取集合等对象中对应index的值
	 * </ol>
	 *
	 * <p>
	 * 表达式栗子：
	 *
	 * <pre>
	 * persion
	 * persion.name
	 * persons[3]
	 * person.friends[5].name
	 * </pre>
	 *
	 * <p>
	 * 获取表达式对应值后转换为对应类型的值
	 * @param <T> 返回值类型
	 * @param expression 表达式
	 * @param resultType 返回值类型
	 * @return 对象
	 * @see BeanPath#get(Object)
	 */
	<T> T getByPath(String expression, Class<T> resultType);

	/**
	 * 转为实体类对象，转换异常将被抛出
	 * @param <T> Bean类型
	 * @param clazz 实体类
	 * @return 实体类对象
	 */
	default <T> T toBean(Class<T> clazz) {
		return toBean((Type) clazz);
	}

	/**
	 * 获得json字符串
	 * @return
	 */
	String toJSONString();

	/**
	 * 转为实体类对象，转换异常将被抛出
	 * @param <T> Bean类型
	 * @param reference {@link GkTypeReference}类型参考子类，可以获取其泛型参数中的Type类型
	 * @return 实体类对象
	 */
	default <T> T toBean(GkTypeReference<T> reference) {
		return toBean(reference.getType());
	}

	/**
	 * 转为实体类对象
	 * @param <T> Bean类型
	 * @param type {@link Type}
	 * @return 实体类对象
	 */
	default <T> T toBean(Type type) {
		return toBean(type, false);
	}

	/**
	 * 转为实体类对象
	 * @param <T> Bean类型
	 * @param type {@link Type}
	 * @param ignoreError 是否忽略转换错误
	 * @return 实体类对象
	 */
	<T> T toBean(Type type, boolean ignoreError);

}
