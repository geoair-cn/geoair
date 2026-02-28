package cn.geoair.gtc.base.env.property;

import cn.geoair.gtc.base.def.GkOperater;

public interface GiPropertier extends GkOperater {

	/**
	 * 判断是否包含指定key的属性
	 * @param key 属性键
	 * @return 包含返回true，否则返回false
	 */
	boolean containsProperty(String key);

	/**
	 * 根据key获取属性值
	 * @param key 属性键
	 * @return 属性值，不存在则返回null
	 */
	String getProperty(String key);

	/**
	 * 根据key获取属性值，若不存在则返回默认值
	 * @param key 属性键
	 * @param defaultValue 默认值
	 * @return 属性值，不存在则返回defaultValue
	 */
	String getProperty(String key, String defaultValue);

	/**
	 * 根据key获取指定类型的属性值
	 * @param key 属性键
	 * @param targetType 目标类型
	 * @return 属性值，不存在则返回null
	 */
	<T> T getProperty(String key, Class<T> targetType);

	/**
	 * 根据key获取指定类型的属性值，若不存在则返回默认值
	 * @param key 属性键
	 * @param targetType 目标类型
	 * @param defaultValue 默认值
	 * @return 属性值，不存在则返回defaultValue
	 */
	<T> T getProperty(String key, Class<T> targetType, T defaultValue);

	/**
	 * 获取必需的属性值，不存在则抛出异常
	 * @param key 属性键
	 * @return 属性值
	 * @throws IllegalStateException 属性不存在时抛出
	 */
	String getRequiredProperty(String key) throws IllegalStateException;

	/**
	 * 获取必需的指定类型的属性值，不存在则抛出异常
	 * @param key 属性键
	 * @param targetType 目标类型
	 * @return 属性值
	 * @throws IllegalStateException 属性不存在时抛出
	 */
	<T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

	/**
	 * 解析文本中的占位符
	 * @param text 包含占位符的文本
	 * @return 解析后的文本
	 */
	String resolvePlaceholders(String text);

	/**
	 * 解析文本中的占位符，缺失属性时抛出异常
	 * @param text 包含占位符的文本
	 * @return 解析后的文本
	 * @throws IllegalArgumentException 属性不存在时抛出
	 */
	String resolveRequiredPlaceholders(String text) throws IllegalArgumentException;

}
