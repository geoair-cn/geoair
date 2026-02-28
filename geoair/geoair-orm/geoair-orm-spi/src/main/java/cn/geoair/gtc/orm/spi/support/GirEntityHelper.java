package cn.geoair.gtc.orm.spi.support;

/**
 * @author ：张俊
 * @date ：Created in 2022/6/30 15:02
 * @description： TODO
 */

import cn.geoair.gtc.base.log.GiLogger;
import cn.geoair.gtc.base.log.GirLogger;
import cn.geoair.gtc.orm.spi.GirEntityResolve;
import cn.geoair.gtc.orm.spi.entity.GirEntityColumn;
import cn.geoair.gtc.orm.spi.entity.GirEntityTable;
import cn.geoair.gtc.orm.spi.jpa.GirJpaGirEntityResolve;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体类工具类 - 处理实体和数据库表以及字段关键的一个类
 * <p/>
 *
 * @author zhangjun
 */
public class GirEntityHelper {

	private static GiLogger logger = GirLogger.getLoger(GirEntityHelper.class);

	/**
	 * 实体类 => 表对象
	 */
	private static final Map<Class<?>, GirEntityTable> entityTableMap = new ConcurrentHashMap<Class<?>, GirEntityTable>();

	/**
	 * 实体类解析器
	 */
	private static GirEntityResolve resolve = new GirJpaGirEntityResolve();

	/**
	 * 获取表对象l
	 * @param entityClass
	 * @return
	 */
	public static GirEntityTable getEntityTable(Class<?> entityClass) {
		initEntityNameMap(entityClass);
		GirEntityTable girEntityTable = entityTableMap.get(entityClass);
		if (girEntityTable == null) {
			throw new RuntimeException("无法获取实体类" + entityClass.getCanonicalName() + "对应的表名!");
		}
		return girEntityTable;
	}

	/**
	 * 获取表对象l
	 * @param entityClass
	 * @return
	 */
	public static GirEntityTable getEntityTable(Class<?> entityClass, GirEntityResolve resolve) {
		GirEntityTable girEntityTable = resolve.resolveEntity(entityClass);
		if (girEntityTable == null) {
			throw new RuntimeException("无法通过该解析器" + resolve.getClass().getName() + "获取实体对象!");
		}
		else {
			entityTableMap.put(entityClass, girEntityTable);
		}
		return girEntityTable;
	}

	/**
	 * 更新表对象
	 * @param entityClass
	 * @param resolve
	 * @return
	 */
	public static GirEntityTable updateEntityTable(Class<?> entityClass, GirEntityResolve resolve) {
		GirEntityTable girEntityTable = resolve.resolveEntity(entityClass);
		if (girEntityTable == null) {
			throw new RuntimeException("无法通过该解析器" + resolve.getClass().getName() + "获取实体对象!");
		}
		else {
			entityTableMap.put(entityClass, girEntityTable);
		}
		return girEntityTable;
	}

	/**
	 * 更新表对象
	 * @param entityClass
	 * @return
	 */
	public static GirEntityTable updateEntityTable(Class<?> entityClass) {
		GirEntityTable girEntityTable = resolve.resolveEntity(entityClass);
		if (girEntityTable == null) {
			throw new RuntimeException("无法通过该解析器" + resolve.getClass().getName() + "获取实体对象!");
		}
		else {
			entityTableMap.put(entityClass, girEntityTable);
		}
		return girEntityTable;
	}

	/**
	 * 获取默认的orderby语句
	 * @param entityClass
	 * @return
	 */
	public static String getOrderByClause(Class<?> entityClass) {
		GirEntityTable table = getEntityTable(entityClass);
		if (table.getOrderByClause() != null) {
			return table.getOrderByClause();
		}
		StringBuilder orderBy = new StringBuilder();
		for (GirEntityColumn column : table.getEntityClassColumns()) {
			if (column.getOrderBy() != null) {
				if (orderBy.length() != 0) {
					orderBy.append(",");
				}
				orderBy.append(column.getColumn()).append(" ").append(column.getOrderBy());
			}
		}
		table.setOrderByClause(orderBy.toString());
		return table.getOrderByClause();
	}

	/**
	 * 获取全部列
	 * @param entityClass
	 * @return
	 */
	public static Set<GirEntityColumn> getColumns(Class<?> entityClass) {
		return getEntityTable(entityClass).getEntityClassColumns();
	}

	/**
	 * 获取主键信息
	 * @param entityClass
	 * @return
	 */
	public static Set<GirEntityColumn> getPKColumns(Class<?> entityClass) {
		return getEntityTable(entityClass).getEntityClassPKColumns();
	}

	/**
	 * 获取查询的Select
	 * @param entityClass
	 * @return
	 */
	public static String getSelectColumns(Class<?> entityClass) {
		GirEntityTable girEntityTable = getEntityTable(entityClass);
		if (girEntityTable.getBaseSelect() != null) {
			return girEntityTable.getBaseSelect();
		}
		Set<GirEntityColumn> columnList = getColumns(entityClass);
		StringBuilder selectBuilder = new StringBuilder();
		boolean skipAlias = Map.class.isAssignableFrom(entityClass);
		for (GirEntityColumn girEntityColumn : columnList) {
			selectBuilder.append(girEntityColumn.getSelectColumn());
			if (!skipAlias && !girEntityColumn.getSelectColumn().equalsIgnoreCase(girEntityColumn.getProperty())) {
				// 不等的时候分几种情况，例如`DESC`
				if (girEntityColumn.getSelectColumn().substring(1, girEntityColumn.getSelectColumn().length() - 1)
						.equalsIgnoreCase(girEntityColumn.getProperty())) {
					selectBuilder.append(",");
				}
				else {
					selectBuilder.append(" AS ").append(girEntityColumn.getProperty()).append(",");
				}
			}
			else {
				selectBuilder.append(",");
			}
		}
		girEntityTable.setBaseSelect(selectBuilder.substring(0, selectBuilder.length() - 1));
		return girEntityTable.getBaseSelect();
	}

	/**
	 * 初始化实体属性
	 * @param entityClass
	 */
	public static synchronized void initEntityNameMap(Class<?> entityClass) {
		if (entityTableMap.get(entityClass) != null) {
			return;
		}
		// 创建并缓存EntityTable
		GirEntityTable girEntityTable = resolve.resolveEntity(entityClass);
		entityTableMap.put(entityClass, girEntityTable);
	}

	/**
	 * 设置实体类解析器
	 * @param resolve
	 */
	public static void setResolve(GirEntityResolve resolve) {
		GirEntityHelper.resolve = resolve;
	}

}
