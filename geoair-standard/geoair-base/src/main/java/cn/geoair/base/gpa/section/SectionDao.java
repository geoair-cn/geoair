package cn.geoair.base.gpa.section;

import cn.geoair.base.gpa.dao.GiDao;
import cn.geoair.base.util.GutilClass;
import cn.geoair.base.util.GutilGenericType;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

/**
 * 分表Dao, dao继承该类型实现分表功能
 *
 * @author Ray
 * @param <M> 模型
 * @param <F> 分表因子
 */
public abstract class SectionDao<M extends SectionModel<PK, F>, PK extends Serializable, F>
        implements GiDao<M, PK> {

    protected Set<String> tableNames = new HashSet<>();

    public abstract <T> List<T> queryForList(String sql, Class<T> elementType);

    public abstract List<Map<String, Object>> queryForList(String sql);

    public abstract void excuteSql(String sql);

    private Object lock = new Object();

    /** 同步已经存在的表到缓存 */
    public void freshTables() {
        synchronized (lock) {
            tableNames.clear();
            List<String> tbs = getExistTableNames();
            if (tbs != null) {
                tableNames.addAll(tbs);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Class<F> getFactorClass() {
        return (Class<F>)
                GutilGenericType.resolveTypeArguments(
                        GutilClass.getUserClass(this), SectionDao.class)[2];
    }

    /**
     * 获取已经存在的表列表
     *
     * @return
     */
    protected List<String> getExistTableNames() {
        if (isPostgresql()) {
            return queryForList(
                    "SELECT tablename FROM pg_tables WHERE tablename NOT LIKE 'pg%' AND tablename NOT LIKE 'sql_%' AND tablename LIKE '"
                            + getTableNamePrefix()
                            + "%'  ORDER  BY  tablename;",
                    String.class);

        } else if (isMysql()) {
            return queryForList(
                    "select table_name from information_schema.tables where table_name like '"
                            + getTableNamePrefix()
                            + "%' and table_type='base table'  ORDER BY table_name DESC ",
                    String.class);
        }
        return null;
    }

    /**
     * 是否有缓存的表
     *
     * @param tableName
     * @return
     */
    public boolean hasCacheTableName(String tableName) {
        synchronized (lock) {
            return tableNames.contains(tableName);
        }
    }

    /**
     * 根据表名获取表名，如果缓存没有，去创建
     *
     * @param tableName
     * @return
     */
    public String getExistTableName(String tableName) {

        if (!hasCacheTableName(tableName)) {
            String sql = getCreateTableSql(tableName);
            try {
                excuteSql(sql);
            } catch (Exception e) {
                // e.printStackTrace();
            }
            freshTables();
        }
        return tableName;
    }

    /**
     * 根据因子获取表名
     *
     * @param factor 分表因子
     * @param autoCreate 如果表不存在，是否创建表， true 创建
     * @return
     */
    public String getTableNameByFactor(F factor, boolean autoCreate) {
        String tableName = getTableNamePrefix() + getTableNameSuffix(factor);
        return autoCreate ? getExistTableName(tableName) : tableName;
    }

    /**
     * 获取表后缀（子类实现）
     *
     * @param factor
     * @return
     */
    protected abstract String getTableNameSuffix(F factor);

    /**
     * 获取表前缀（子类实现）
     *
     * @return
     */
    protected abstract String getTableNamePrefix();

    /**
     * 通过表名 获取创建表的 sql语句（子类实现）
     *
     * @param tableName
     * @return
     */
    protected abstract String getCreateTableSql(String tableName);

    /**
     * 获取数据源（子类实现）
     *
     * @return
     */
    protected abstract DataSource getDataSource();

    /**
     * 工具方法，获取是不是Mysql数据库
     *
     * @return true 是mysql
     */
    public boolean isMysql() {
        return getDriverNameUpperCase().indexOf("MYSQL") != -1;
    }

    /**
     * 工具方法，获取是不是Pg数据库
     *
     * @return true 是Pg数据库
     */
    public boolean isPostgresql() {
        return getDriverNameUpperCase().indexOf("POSTGRESQL") != -1;
    }

    private String driverName = null;

    protected String getDriverNameUpperCase() {
        if (driverName == null) {
            try {
                driverName =
                        getDataSource().getConnection().getMetaData().getDriverName().toUpperCase();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return driverName;
    }

    /**
     * 获取id列名
     *
     * @return
     */
    public abstract String getColumnId();

    /**
     * 获取已经存在的id
     *
     * @param ids
     * @param tableName
     * @return
     */
    public List<String> getExistingIds(Set<PK> ids, String tableName) {
        String columnId = this.getColumnId();
        StringBuilder sql =
                new StringBuilder("SELECT ")
                        .append(columnId)
                        .append(" FROM ")
                        .append(tableName)
                        .append(" WHERE ")
                        .append(columnId)
                        .append(" in (");

        Class<PK> pkCls = this.getPKClass();
        boolean isNumber = Number.class.isAssignableFrom(pkCls);
        for (PK id : ids) {
            if (isNumber) {
                sql.append(id).append(",");
            } else {
                sql.append("'").append(id).append("',");
            }
        }

        sql.deleteCharAt(sql.length() - 1);
        sql.append(")");
        return queryForList(sql.toString(), String.class);
    }

    /**
     * 批量更新或者插入数据
     *
     * @param list
     * @param tableName
     * @return
     */
    public int saveOrUpdateBatch(List<M> list, String tableName) {

        String tn = getExistTableName(tableName);
        Set<PK> ids = new HashSet<>();
        for (M m : list) {
            ids.add(m.id());
        }
        List<String> existingIds = getExistingIds(ids, tn);

        List<M> sectionUpdate = new ArrayList<>();
        List<M> sectionAdd = new ArrayList<>();

        for (M it : list) {
            if (existingIds.contains(it.id())) {
                sectionUpdate.add(it);
            } else {
                sectionAdd.add(it);
            }
        }
        if (sectionUpdate.size() > 0) {
            this.updateBatch(sectionUpdate, tableName);
        }

        int addSize = sectionAdd.size();
        if (addSize > 0) {
            this.saveBatch(sectionAdd, tableName);
        }
        return addSize;
    }

    /**
     * 批量更新数据，默认实现是删除全部，再新增。子类可以覆盖实现
     *
     * @param list
     * @param tableName
     */
    public void updateBatch(List<M> list, String tableName) {
        this.deleteBatch(list, tableName);
        this.saveBatch(list, tableName);
    }

    /**
     * 批量删除，用ID来删除
     *
     * @param list
     * @param tableName
     */
    public void deleteBatch(List<M> list, String tableName) {
        if (list == null || list.size() == 0) {
            return;
        }
        String columnId = this.getColumnId();
        StringBuilder sql =
                new StringBuilder("DELETE FROM ")
                        .append(tableName)
                        .append(" WHERE ")
                        .append(columnId)
                        .append(" in (");
        for (M item : list) {
            sql.append("'").append(item.id()).append("',");
        }
        sql.deleteCharAt(sql.length() - 1);
        sql.append(")");
        excuteSql(sql.toString());
    }

    /**
     * 批量新增 （子类实现）
     *
     * @param list
     * @param tableName
     */
    protected abstract void saveBatch(List<M> list, String tableName);

    protected abstract void saveBatchOneByOne(List<M> list, String tableName);
}
