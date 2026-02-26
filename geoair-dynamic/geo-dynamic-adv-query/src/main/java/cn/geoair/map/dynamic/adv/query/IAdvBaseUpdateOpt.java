package cn.geoair.map.dynamic.adv.query;

import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据更新相关的基础操作接口
 * <p>
 * 覆盖单条更新、批量更新、自定义SQL更新、条件更新、乐观锁更新、UPSERT等全场景，
 * 适配PostgreSQL/MySQL等主流数据库，保持语义化命名和易用性
 *
 * @author 张逢吉
 */
public interface IAdvBaseUpdateOpt {
    /**
     * 设置数据源获取器
     *
     * @param dataSourceGetter 数据源获取器，用于获取数据库连接
     */
    void setDataSourceGetter(IDataSourceGetter dataSourceGetter);
    // ==================== 自定义SQL更新  ====================

    /**
     * 执行自定义更新SQL语句（无参数）
     * <p>
     * 支持任意复杂的UPDATE SQL（含多表关联更新、子查询更新等）
     *
     * @param sqlStatement 自定义更新SQL语句（支持MyBatis标签）
     *                     <br>示例：UPDATE user SET age = age + 1 WHERE id = 1001
     * @return Integer 受影响的行数
     */
    Integer bUpdateBySql(String sqlStatement);

    /**
     * 执行带参数的自定义更新SQL语句
     * <p>
     * 解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param sqlStatement 自定义更新SQL语句（含参数占位符）
     *                     <br>示例：UPDATE user SET name = #{name} WHERE id = #{id}
     *                     sqlParam       SQL参数映射（key为占位符名称，value为参数值）
     *                     <br>示例：{ "name": "张三", "id": 1001 }
     * @return Integer 受影响的行数
     */
    Integer bUpdateBySql(String sqlStatement, SqlParamMap sqlParam);

    // ==================== 单条数据更新（标准化） ====================

    /**
     * 根据主键更新单条数据（Map格式）
     * <p>
     * 适用于按主键更新指定字段的场景，字段名与Map的key一一对应
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名（如：id）
     * @param id        主键值
     * @param rowData   待更新的字段-值映射（key=字段名，value=新值）
     *                  <br>示例：{ "name": "张三", "age": 25 }
     * @return Integer 受影响的行数（成功返回1，无匹配数据返回0）
     */
    Integer bUpdateByPrimaryKey(String tableName, String idKey, Object id, Map<String, Object> rowData);

    /**
     * 根据主键更新单条实体对象
     * <p>
     * 自动提取实体对象的属性作为更新字段，主键字段用于WHERE条件
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param entity    待更新的实体对象（需包含主键值）
     * @param <T>       实体类泛型
     * @return Integer 受影响的行数
     */
    <T> Integer bUpdateByPrimaryKey(String tableName, String idKey, T entity);

    /**
     * 条件更新单条数据（非主键更新）
     * <p>
     * 适用于按自定义条件更新，避免全表更新风险（需确保条件能定位单行）
     *
     * @param tableName 目标表名
     * @param rowData   待更新的字段-值映射
     * @param whereMap  更新条件（key=字段名，value=条件值，拼接为AND条件）
     *                  <br>示例：{ "name": "张三", "dept_id": 5 }
     * @return Integer 受影响的行数
     */
    Integer bUpdateByCondition(String tableName, Map<String, Object> rowData, Map<String, Object> whereMap);

    // ==================== 批量更新 ====================

    /**
     * 批量更新数据（按主键批量更新）
     * <p>
     * 适用于批量更新不同主键的多条数据，每条数据的更新字段可不同
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param rowsData  批量更新数据（每个Map需包含主键字段和待更新字段）
     *                  <br>示例：[{ "id": 1, "age":25 }, { "id":2, "name":"李四" }]
     * @return Integer 受影响的总行数
     */
    Integer bUpdateBatchByPrimaryKey(String tableName, String idKey, List<Map<String, Object>> rowsData);

    /**
     * 分批次批量更新（避免单次批量更新数据量过大）
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param rowsData  批量更新数据
     * @param batchSize 每批次更新条数（建议1000-5000）
     * @return Integer 受影响的总行数
     */
    Integer bUpdateBatchWithBatchSize(String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize);

    /**
     * 批量更新实体对象列表（按主键）
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param entities  待更新的实体对象列表（需包含主键值）
     * @param <T>       实体类泛型
     * @return Integer 受影响的总行数
     */
    <T> Integer bUpdateBatchByPrimaryKey(String tableName, String idKey, Collection<T> entities);

    // ==================== 特殊场景更新 ====================

    /**
     * 乐观锁更新（基于版本号）
     * <p>
     * 解决并发更新问题，仅当版本号匹配时才更新，适用于高并发场景
     *
     * @param tableName  目标表名
     * @param idKey      主键字段名
     * @param id         主键值
     * @param rowData    待更新的字段-值映射
     * @param versionKey 版本号字段名（如：version）
     * @param version    当前版本号
     * @return Integer 受影响的行数（版本匹配返回1，不匹配返回0）
     */
    Integer bUpdateWithOptimisticLock(String tableName, String idKey, Object id, Map<String, Object> rowData, String versionKey, Integer version);

    /**
     * 更新或插入（UPSERT）
     * <p>
     * 先尝试更新，无匹配数据则插入，等价于INSERT ON CONFLICT UPDATE
     *
     * @param tableName    目标表名
     * @param rowData      待更新/插入的字段-值映射
     * @param conflictKeys 冲突判定字段（唯一索引/主键）
     * @return Integer 受影响的行数（更新返回1，插入返回1，无变化返回0）
     */
    Integer bUpdateOrInsert(String tableName, Map<String, Object> rowData, Set<String> conflictKeys);
}
