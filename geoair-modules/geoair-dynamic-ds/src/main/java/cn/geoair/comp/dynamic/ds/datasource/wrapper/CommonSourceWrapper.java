package cn.geoair.comp.dynamic.ds.datasource.wrapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

/**
 * 通用数据源包装器 基于反射适配所有未显式实现的数据源类型
 */
public class CommonSourceWrapper extends AbstractDataSourceWrapper {

	// 常见的数据源名称属性/方法名（按优先级排序）
	private static final List<String> NAME_KEYS = Arrays.asList("name", "dataSourceName", "poolName", "dsName", "id",
			"identifier");

	// 常见的JDBC URL属性/方法名（按优先级排序）
	private static final List<String> URL_KEYS = Arrays.asList("jdbcUrl", "url", "rawJdbcUrl", "jdbcUrl", "getUrl",
			"getJdbcUrl", "rawUrl");

	private static final List<String> CLOSE_KEYS = Arrays.asList("close");

	public CommonSourceWrapper(DataSource targetDataSource) {
		super(targetDataSource);
	}

	public static boolean canInit() {
		return true;
	}

	@Override
	public boolean close() {
		for (String closeKey : CLOSE_KEYS) {
			try {
				Method method = targetDataSource.getClass().getMethod(closeKey);
				method.invoke(targetDataSource);
			}
			catch (Exception e) {
				continue;
			}
		}
		return true;
	}

	@Override
	protected Class<? extends DataSource> getTargetDataSourceClass() {
		// 匹配所有DataSource类型（作为兜底包装器）
		return DataSource.class;
	}

	/**
	 * 反射获取数据源名称（无则返回null）
	 */
	@Override
	public String getSimpleDataSourceName() {
		return getValueByReflection(targetDataSource, NAME_KEYS);
	}

	/**
	 * 反射获取JDBC URL（无则返回null）
	 */
	@Override
	public String getJdbcUrl() {
		return getValueByReflection(targetDataSource, URL_KEYS);
	}

	/**
	 * 通用反射获取值的方法
	 * @param target 目标对象
	 * @param keys 要尝试的属性/方法名列表
	 * @return 找到的值（字符串），无则返回null
	 */
	private String getValueByReflection(Object target, List<String> keys) {
		if (target == null || keys == null || keys.isEmpty()) {
			return null;
		}

		Class<?> clazz = target.getClass();
		// 遍历所有候选key，尝试获取值
		for (String key : keys) {
			// 1. 先尝试通过getter方法获取（如 getName()、getJdbcUrl()）
			String getterMethodName = "get" + key.substring(0, 1).toUpperCase() + key.substring(1);
			try {
				Method method = clazz.getMethod(getterMethodName);
				Object result = method.invoke(target);
				if (result != null) {
					return result.toString();
				}
			}
			catch (Exception e) {
				// 方法不存在/调用失败，继续尝试下一个
				continue;
			}

			// 2. 尝试直接获取字段（如 name、jdbcUrl）
			try {
				Field field = getDeclaredField(clazz, key);
				if (field != null) {
					field.setAccessible(true);
					Object result = field.get(target);
					if (result != null) {
						return result.toString();
					}
				}
			}
			catch (Exception e) {
				// 字段不存在/访问失败，继续尝试下一个
				continue;
			}
		}

		// 所有方式都失败，返回null
		return null;
	}

	/**
	 * 递归获取声明的字段（包括父类）
	 */
	private Field getDeclaredField(Class<?> clazz, String fieldName) {
		try {
			return clazz.getDeclaredField(fieldName);
		}
		catch (NoSuchFieldException e) {
			Class<?> superClass = clazz.getSuperclass();
			if (superClass != null && superClass != Object.class) {
				return getDeclaredField(superClass, fieldName);
			}
			return null;
		}
	}

}
