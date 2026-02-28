package cn.geoair.gtc.orm.tkmapper.util;

import java.lang.reflect.Field;
import java.util.Map;
import cn.geoair.gtc.base.Gir;
import cn.geoair.gtc.base.tool.GkConcurrentReferenceHashMap;
import cn.geoair.gtc.base.util.GutilReflection;
import tk.mybatis.mapper.entity.EntityColumn;
import tk.mybatis.mapper.entity.EntityField;
import tk.mybatis.mapper.entity.EntityTable;
import tk.mybatis.mapper.mapperhelper.EntityHelper;

/**
 * 对 TkMapper 实体的工具类
 *
 * @author Ray
 *
 */
public class TkEntityHelper {

	private static final Map<String, EntityColumn> entityTableMap = new GkConcurrentReferenceHashMap<String, EntityColumn>();

	/**
	 * 通过方法名称(getter or setter) 获取 EntityColumn
	 * @param entityClass
	 * @param methodName
	 * @return
	 */
	public static EntityColumn getEntityColumnByMethodName(Class<?> entityClass, String methodName) {

		return entityTableMap.computeIfAbsent(methodName, key -> {

			EntityTable table = EntityHelper.getEntityTable(entityClass);
			for (EntityColumn column : table.getEntityClassColumns()) {
				EntityField ef = column.getEntityField();
				try {
					Field getter = ef.getClass().getDeclaredField("getter");
					GutilReflection.makeAccessible(getter);

					if (key.equalsIgnoreCase(getter.getName())) {
						return column;
					}
					else {
						Field setter = ef.getClass().getDeclaredField("setter");
						GutilReflection.makeAccessible(setter);

						if (key.equalsIgnoreCase(setter.getName())) {
							return column;
						}
					}

				}
				catch (NoSuchFieldException | SecurityException e) {
					Gir.log.error(e, "tk.mybatis.mapper.entity.EntityField类无法访问 getter setter属性");
				}
			}
			return null;
		});

	}

}
