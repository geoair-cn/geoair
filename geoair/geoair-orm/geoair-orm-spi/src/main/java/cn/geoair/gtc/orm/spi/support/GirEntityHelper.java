package cn.geoair.gtc.orm.spi.support;

/**
 * @author ：张俊
 * @date ：Created in 2022/6/30 15:02
 * @description： TODO
 */

import cn.geoair.gtc.base.log.GiLogger;
import cn.geoair.gtc.base.log.GirLogger;
import cn.geoair.gtc.orm.spi.GirEntityResolve;
import cn.geoair.gtc.orm.spi.entity.GtcEntityColumn;
import cn.geoair.gtc.orm.spi.entity.GtcEntityTable;
import cn.geoair.gtc.orm.spi.jpa.GtcJpaGirEntityResolve;


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

    private static GiLogger logger =  GirLogger.getLoger( GirEntityHelper.class);
    /**
     * 实体类 => 表对象
     */
    private static final Map<Class<?>, GtcEntityTable> entityTableMap = new ConcurrentHashMap<Class<?>, GtcEntityTable>();
    /**
     * 实体类解析器
     */
    private static GirEntityResolve resolve = new GtcJpaGirEntityResolve();


    /**
     * 获取表对象l
     *
     * @param entityClass
     * @return
     */
    public static GtcEntityTable getEntityTable(Class<?> entityClass) {
        initEntityNameMap(entityClass);
         GtcEntityTable gtcEntityTable = entityTableMap.get(entityClass);
        if ( gtcEntityTable == null) {
            throw new RuntimeException("无法获取实体类" + entityClass.getCanonicalName() + "对应的表名!");
        }
        return  gtcEntityTable;
    }

    /**
     * 获取表对象l
     *
     * @param entityClass
     * @return
     */
    public static GtcEntityTable getEntityTable(Class<?> entityClass, GirEntityResolve resolve) {
         GtcEntityTable gtcEntityTable = resolve.resolveEntity(entityClass);
        if ( gtcEntityTable == null) {
            throw new RuntimeException("无法通过该解析器" + resolve.getClass().getName() + "获取实体对象!");
        } else {
            entityTableMap.put(entityClass,  gtcEntityTable);
        }
        return  gtcEntityTable;
    }

    /**
     * 更新表对象
     * @param entityClass
     * @param resolve
     * @return
     */
    public static GtcEntityTable updateEntityTable(Class<?> entityClass, GirEntityResolve resolve) {
         GtcEntityTable gtcEntityTable = resolve.resolveEntity(entityClass);
        if ( gtcEntityTable == null) {
            throw new RuntimeException("无法通过该解析器" + resolve.getClass().getName() + "获取实体对象!");
        } else {
            entityTableMap.put(entityClass,  gtcEntityTable);
        }
        return  gtcEntityTable;
    }

    /**
     * 更新表对象
     * @param entityClass
     * @return
     */
    public static GtcEntityTable updateEntityTable(Class<?> entityClass) {
         GtcEntityTable gtcEntityTable = resolve.resolveEntity(entityClass);
        if ( gtcEntityTable == null) {
            throw new RuntimeException("无法通过该解析器" + resolve.getClass().getName() + "获取实体对象!");
        } else {
            entityTableMap.put(entityClass,  gtcEntityTable);
        }
        return  gtcEntityTable;
    }

    /**
     * 获取默认的orderby语句
     *
     * @param entityClass
     * @return
     */
    public static String getOrderByClause(Class<?> entityClass) {
         GtcEntityTable table = getEntityTable(entityClass);
        if (table.getOrderByClause() != null) {
            return table.getOrderByClause();
        }
        StringBuilder orderBy = new StringBuilder();
        for ( GtcEntityColumn column : table.getEntityClassColumns()) {
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
     *
     * @param entityClass
     * @return
     */
    public static Set<GtcEntityColumn> getColumns(Class<?> entityClass) {
        return getEntityTable(entityClass).getEntityClassColumns();
    }

    /**
     * 获取主键信息
     *
     * @param entityClass
     * @return
     */
    public static Set<GtcEntityColumn> getPKColumns(Class<?> entityClass) {
        return getEntityTable(entityClass).getEntityClassPKColumns();
    }

    /**
     * 获取查询的Select
     *
     * @param entityClass
     * @return
     */
    public static String getSelectColumns(Class<?> entityClass) {
         GtcEntityTable gtcEntityTable = getEntityTable(entityClass);
        if ( gtcEntityTable.getBaseSelect() != null) {
            return  gtcEntityTable.getBaseSelect();
        }
        Set<GtcEntityColumn> columnList = getColumns(entityClass);
        StringBuilder selectBuilder = new StringBuilder();
        boolean skipAlias = Map.class.isAssignableFrom(entityClass);
        for ( GtcEntityColumn gtcEntityColumn : columnList) {
            selectBuilder.append( gtcEntityColumn.getSelectColumn());
            if (!skipAlias && ! gtcEntityColumn.getSelectColumn().equalsIgnoreCase( gtcEntityColumn.getProperty())) {
                //不等的时候分几种情况，例如`DESC`
                if ( gtcEntityColumn.getSelectColumn().substring(1,  gtcEntityColumn.getSelectColumn().length() - 1).equalsIgnoreCase( gtcEntityColumn.getProperty())) {
                    selectBuilder.append(",");
                } else {
                    selectBuilder.append(" AS ").append( gtcEntityColumn.getProperty()).append(",");
                }
            } else {
                selectBuilder.append(",");
            }
        }
         gtcEntityTable.setBaseSelect(selectBuilder.substring(0, selectBuilder.length() - 1));
        return  gtcEntityTable.getBaseSelect();
    }

    /**
     * 初始化实体属性
     *
     * @param entityClass
     */
    public static synchronized void initEntityNameMap(Class<?> entityClass) {
        if (entityTableMap.get(entityClass) != null) {
            return;
        }
        //创建并缓存EntityTable
         GtcEntityTable gtcEntityTable = resolve.resolveEntity(entityClass);
        entityTableMap.put(entityClass,  gtcEntityTable);
    }

    /**
     * 设置实体类解析器
     *
     * @param resolve
     */
    public static void setResolve( GirEntityResolve resolve) {
         GirEntityHelper.resolve = resolve;
    }
}
