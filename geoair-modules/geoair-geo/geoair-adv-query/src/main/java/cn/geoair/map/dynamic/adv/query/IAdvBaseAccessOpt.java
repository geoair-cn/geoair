package cn.geoair.map.dynamic.adv.query;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据插入相关的基础操作接口
 *
 * <p>覆盖单条插入、批量插入、自定义SQL插入、忽略重复插入、插入并返回主键等全场景
 */
public interface IAdvBaseAccessOpt {

    /**
     * 设置数据源获取器
     *
     * @param dataSourceGetter 数据源获取器，用于获取数据库连接
     */
    void setDataSourceGetter(IDataSourceGetter dataSourceGetter);
    // ==================== 自定义SQL插入 ====================

    /**
     * 执行自定义插入SQL语句
     *
     * <p>支持任意复杂的INSERT SQL（含多表插入、子查询插入、自定义字段映射等）， 适用于无法通过标准化方法实现的特殊插入场景
     *
     * @param sql 自定义插入SQL语句  <br>
     *            示例1：INSERT INTO user (id, name, age) VALUES (1, name, 21) <br>
     * @return Integer 受影响的行数（成功插入的记录数）
     */
    Integer bInsertBySql(String sql);

    /**
     * 执行带参数的自定义插入SQL语句
     *
     * <p>解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param dynamicSql  自定义插入SQL语句（含参数占位符） <br>
     *                    示例：INSERT INTO user (name, age) VALUES (#{name}, #{age}) sqlParam
     *                    SQL参数映射（key为占位符名称，value为参数值） <br>
     *                    示例：{ "name": "张三", "age": 25 }
     * @param sqlParamMap
     * @return Integer 受影响的行数
     */
    Integer bInsertBySql(String dynamicSql, SqlParamMap sqlParamMap);

    /**
     * 执行带参数的自定义插入SQL语句
     *
     * <p>解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param sqlStatement 自定义插入SQL语句（含参数占位符） <br>
     *                     示例：INSERT INTO user (name, age) VALUES (?, ?) sqlParam
     *                     SQL参数映射（ value为参数值） <br>
     *                     示例：【  "张三",  25 】
     * @return Integer 受影响的行数
     */
    Integer bInsertBySql(String sqlStatement, SqlParamList sqlParamList);


    /**
     * 兼容SqlParamList 与SqlParamMap 的参数传入的执行器
     *
     * @param sqlStatementOrDynamicSql
     * @param sqlParam
     * @return
     */
    Integer bInsertBySql(String sqlStatementOrDynamicSql, GirSqlParam sqlParam);

    // ==================== 单条数据插入（标准化） ====================

    /**
     * 插入单条Map格式的数据（自动匹配表字段）
     *
     * <p>适用于单条数据插入，Map的key对应表字段名，value对应字段值
     *
     * @param tableName 目标表名（如：user）
     * @param rowData   单行数据（key=字段名，value=字段值） <br>
     *                  示例：{ "name": "张三", "age": 25, "create_time": new Date() }
     * @return Integer 受影响的行数（成功返回1，失败返回0）
     */
    Integer bInsertOne(String tableName, Map<String, Object> rowData);

    /**
     * 插入单条Java对象数据 字段名称默认转下划线
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配
     *
     * @param tableName 目标表名（如：user）
     * @param entity    待插入的Java对象（如User实体类）
     * @param <T>       实体类泛型
     * @return Integer 受影响的行数
     */
    <T> Integer bInsertOne(String tableName, T entity);

    /**
     * 插入单条Java对象数据（字段名自动映射）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配（支持驼峰转下划线）
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待插入的Java对象（如User实体类）
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue   是否忽略值为空的字段
     * @param <T>               实体类泛型
     * @return Integer 受影响的行数
     */
    <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase, boolean ignoreNullValue);


    /**
     * 插入单条Java对象数据（字段名自动映射）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配（支持驼峰转下划线）
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待插入的Java对象（如User实体类）
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param <T>               实体类泛型
     * @return Integer 受影响的行数
     */
    <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase);

    /**
     * 插入单条Java对象数据（字段名自动映射）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配（支持驼峰转下划线）
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待插入的Java对象（如User实体类）
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue   是否忽略值为空的字段
     * @param ignoreFieldNames  插入的时候，忽略哪些字段，这里传你实体里面的字段名称
     * @param <T>               实体类泛型
     * @return Integer 受影响的行数
     */
    <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames);


    /**
     * 批量插入Map格式的数据
     *
     * <p>适用于批量插入同结构数据，底层优化为批量SQL（如INSERT INTO ... VALUES (...), (...), ...）， 相比循环单条插入性能提升显著
     *
     * @param tableName 目标表名
     * @param headers   字段名集合（指定插入的字段顺序，避免Map无序导致的问题） <br>
     *                  示例：Set.of("name", "age", "create_time")
     * @param rowsData  多行数据列表（每个Map的key需包含headers中的所有字段） <br>
     *                  示例：[{ "name": "张三", "age":25 }, { "name": "李四", "age":28 }]
     * @return Integer 成功插入的记录总数
     */
    Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData);


    /**
     * 分批次批量插入（避免单次批量插入数据量过大导致数据库压力）
     *
     * <p>自动将数据按批次拆分，分批插入，适用于超大数据量（如10万+条）插入场景
     *
     * @param tableName 目标表名
     * @param headers   字段名集合
     * @param rowsData  多行数据列表
     * @param batchSize 每批次插入的条数（建议设置为1000-5000）
     * @return Integer 成功插入的记录总数
     */
    Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData, int batchSize);

    /**
     * 批量插入Java对象数据
     *
     * <p>面向对象的批量插入，自动提取对象属性作为字段值
     *
     * @param tableName 目标表名
     * @param entities  待插入的对象列表
     * @param <T>       实体类泛型
     * @return Integer 成功插入的记录总数
     */
    <T> Integer bInsertBatch(String tableName, Collection<T> entities);

    /**
     * 分批次批量插入Java对象数据
     *
     * @param tableName 目标表名
     * @param entities  待插入的对象列表
     * @param batchSize 每批次插入的条数
     * @param <T>       实体类泛型
     * @return Integer 成功插入的记录总数
     */
    <T> Integer bInsertBatch(String tableName, Collection<T> entities, int batchSize);

    // ==================== 特殊场景插入 ====================

    /**
     * 插入或忽略（存在则跳过，不存在则插入）
     *
     * <p>基于数据库唯一索引/主键实现，避免重复插入，适用于无需更新仅需插入的场景 （PostgreSQL：ON CONFLICT DO NOTHING；MySQL：INSERT
     * IGNORE INTO）
     *
     * @param tableName    目标表名
     * @param rowData      单行数据
     * @param conflictKeys 冲突判定字段（唯一索引/主键）
     * @return Integer 成功插入的行数（存在则返回0，不存在则返回1）
     */
    Integer bInsertIgnore(String tableName, Map<String, Object> rowData, Set<String> conflictKeys);


    /**
     * 插入单条Java对象数据 字段名称默认转下划线
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配
     *
     * @param tableName    目标表名（如：user）
     * @param entity       待插入的Java对象（如User实体类）
     * @param conflictKeys 冲突判定字段（唯一索引/主键）
     * @param <T>          实体类泛型
     * @return Integer 受影响的行数
     */
    <T> Integer bInsertIgnore(String tableName, T entity, Set<String> conflictKeys);

    /**
     * 插入单条Java对象数据（字段名自动映射）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配（支持驼峰转下划线）
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待插入的Java对象（如User实体类）
     * @param conflictKeys      冲突判定字段（唯一索引/主键）
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue   是否忽略值为空的字段
     * @param <T>               实体类泛型
     * @return Integer 受影响的行数
     */
    <T> Integer bInsertIgnore(String tableName, T entity, Set<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue);


    /**
     * 插入单条Java对象数据（字段名自动映射）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配（支持驼峰转下划线）
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待插入的Java对象（如User实体类）
     * @param conflictKeys      冲突判定字段（唯一索引/主键）
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param <T>               实体类泛型
     * @return Integer 受影响的行数
     */
    <T> Integer bInsertIgnore(String tableName, T entity, Set<String> conflictKeys, boolean isToUnderlineCase);

    /**
     * 插入单条Java对象数据（字段名自动映射）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配（支持驼峰转下划线）
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待插入的Java对象（如User实体类）
     * @param conflictKeys      冲突判定字段（唯一索引/主键）
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue   是否忽略值为空的字段
     * @param ignoreFieldNames  插入的时候，忽略哪些字段，这里传你实体里面的字段名称
     * @param <T>               实体类泛型
     * @return Integer 受影响的行数
     */
    <T> Integer bInsertIgnore(String tableName, T entity, Set<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames);


    /**
     * 批量插入或忽略
     *
     * @param tableName    目标表名
     * @param headers      字段名集合
     * @param rowsData     多行数据列表
     * @param conflictKeys 冲突判定字段（唯一索引/主键）
     * @return Integer 成功插入的记录总数（已存在的记录不计入）
     */
    Integer bInsertIgnoreBatch(
            String tableName, Set<String> headers, List<Map<String, Object>> rowsData, Set<String> conflictKeys);

    /**
     * 插入或更新（存在则更新指定字段，不存在则插入）
     *
     * <p>即UPSERT操作（PostgreSQL：ON CONFLICT DO UPDATE；MySQL：ON DUPLICATE KEY UPDATE），
     * 适用于需要"插入-更新"一体化的场景
     *
     * @param tableName    目标表名
     * @param rowData      单行数据（包含插入/更新的字段）
     * @param updateFields 冲突时需要更新的字段集合（为空则更新所有字段） <br>
     *                     示例：Set.of("age", "update_time")
     * @return Integer 受影响的行数（插入返回1，更新返回2，无变化返回0）
     */
    Integer bInsertOrUpdate(
            String tableName, Map<String, Object> rowData, Set<String> updateFields);
}
