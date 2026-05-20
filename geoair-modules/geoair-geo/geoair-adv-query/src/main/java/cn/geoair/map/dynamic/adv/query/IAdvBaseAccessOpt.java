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
 *
 * @author 张逢吉
 */
public interface IAdvBaseAccessOpt extends IAdvConfigOpt {

    /**
     * 设置数据源获取器
     *
     * @param dataSourceGetter 数据源获取器，用于获取数据库连接
     */
    void setDataSourceGetter(IDataSourceGetter dataSourceGetter);

    // ==================== 1. 自定义SQL插入（最灵活） ====================

    /**
     * 执行自定义插入SQL语句
     *
     * <p>支持任意复杂的INSERT SQL（含多表插入、子查询插入、自定义字段映射等），
     * 适用于无法通过标准化方法实现的特殊插入场景
     *
     * @param sql 自定义插入SQL语句
     *            示例：INSERT INTO user (id, name, age) VALUES (1, '张三', 21)
     * @return 受影响的行数（成功插入的记录数）
     */
    Integer bInsertBySql(String sql);

    /**
     * 执行带命名占位符的自定义插入SQL语句
     *
     * <p>解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param dynamicSql  自定义插入SQL语句（含#{name}命名占位符）
     *                    示例：INSERT INTO user (name, age) VALUES (#{name}, #{age})
     * @param sqlParamMap SQL参数映射（key为占位符名称，value为参数值）
     *                    示例：{ "name": "张三", "age": 25 }
     * @return 受影响的行数
     */
    Integer bInsertBySql(String dynamicSql, SqlParamMap sqlParamMap);

    /**
     * 执行带位置占位符的自定义插入SQL语句
     *
     * <p>解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param sqlStatement  自定义插入SQL语句（含?位置占位符）
     *                      示例：INSERT INTO user (name, age) VALUES (?, ?)
     * @param sqlParamList SQL参数列表
     *                      示例：["张三", 25]
     * @return 受影响的行数
     */
    Integer bInsertBySql(String sqlStatement, SqlParamList sqlParamList);

    /**
     * 执行自动识别参数类型的自定义插入SQL语句
     *
     * <p>兼容SqlParamList与SqlParamMap的参数传入，自动识别参数类型
     *
     * @param sqlStatementOrDynamicSql 自定义插入SQL语句（支持?或#{name}占位符）
     * @param sqlParam                 SQL参数（自动识别为SqlParamList或SqlParamMap）
     * @return 受影响的行数
     */
    Integer bInsertBySql(String sqlStatementOrDynamicSql, GirSqlParam sqlParam);

    // ==================== 2. 单条数据插入（标准化） ====================

    /**
     * 插入单条Map格式的数据（自动匹配表字段）
     *
     * <p>适用于单条数据插入，Map的key对应表字段名，value对应字段值
     *
     * @param tableName 目标表名（如：user）
     * @param rowData   单行数据（key=字段名，value=字段值）
     *                  示例：{ "name": "张三", "age": 25, "create_time": new Date() }
     * @return 受影响的行数（成功返回1，失败返回0）
     */
    Integer bInsertOne(String tableName, Map<String, Object> rowData);

    /**
     * 插入单条实体数据（自动提取属性，表名从注解获取）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名自动转换为下划线字段名
     *
     * @param entity 待插入的Java对象（如User实体类，需包含表名注解）
     * @param <T>    实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bInsertOne(T entity);

    /**
     * 插入单条实体数据（指定表名，默认驼峰转下划线）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配
     *
     * @param tableName 目标表名（如：user）
     * @param entity    待插入的Java对象（如User实体类）
     * @param <T>       实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bInsertOne(String tableName, T entity);

    /**
     * 插入单条实体数据（支持驼峰转换控制）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待插入的Java对象（如User实体类）
     * @param isToUnderlineCase 是否转换为下划线模式（true: userName → user_name）
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase);

    /**
     * 插入单条实体数据（支持驼峰转换和空值控制）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待插入的Java对象（如User实体类）
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue   是否忽略值为空的字段（true: null字段不参与插入）
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase, boolean ignoreNullValue);

    /**
     * 插入单条实体数据（支持指定忽略字段）
     *
     * <p>适用于面向对象的单条数据插入，对象属性名需与表字段名匹配
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待插入的Java对象（如User实体类）
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue   是否忽略值为空的字段
     * @param ignoreFieldNames  插入时忽略的字段名称列表（实体中的属性名）
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bInsertOne(String tableName, T entity, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames);

    /**
     * 插入单条实体数据
     *
     * @param entity            待插入的Java对象
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue   是否忽略值为空的字段
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bInsertOne(T entity, boolean isToUnderlineCase, boolean ignoreNullValue);

    // ==================== 3. 选择性插入（自动过滤null） ====================

    /**
     * 选择性插入单条实体数据（自动过滤null值字段，表名从注解获取）
     *
     * <p>只插入实体中非null的字段，适用于部分字段有值的场景
     *
     * @param entity 待插入的Java对象
     * @param <T>    实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bInsertSelectiveOne(T entity);

    /**
     * 选择性插入单条实体数据（支持驼峰转换控制）
     *
     * @param entity            待插入的Java对象
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bInsertSelectiveOne(T entity, boolean isToUnderlineCase);

    // ==================== 4. 批量插入 ====================

    /**
     * 批量插入Map格式的数据（指定字段顺序）
     *
     * <p>适用于批量插入同结构数据，底层优化为批量SQL（如INSERT INTO ... VALUES (...), (...), ...），
     * 相比循环单条插入性能提升显著
     *
     * @param tableName 目标表名
     * @param headers   字段名集合（指定插入的字段顺序，避免Map无序导致的问题）
     *                  示例：List.of("name", "age", "create_time")
     * @param rowsData  多行数据列表（每个Map的key需包含headers中的所有字段）
     *                  示例：[{ "name": "张三", "age":25 }, { "name": "李四", "age":28 }]
     * @return 成功插入的记录总数
     */
    Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData);

    /**
     * 分批次批量插入Map数据（避免单次批量插入数据量过大导致数据库压力）
     *
     * <p>自动将数据按批次拆分，分批插入，适用于超大数据量（如10万+条）插入场景
     *
     * @param tableName 目标表名
     * @param headers   字段名集合
     * @param rowsData  多行数据列表
     * @param batchSize 每批次插入的条数（建议设置为1000-5000）
     * @return 成功插入的记录总数
     */
    Integer bInsertBatch(String tableName, List<String> headers, List<Map<String, Object>> rowsData, int batchSize);

    /**
     * 批量插入实体数据（默认驼峰转下划线）
     *
     * <p>面向对象的批量插入，自动提取对象属性作为字段值
     *
     * @param tableName 目标表名
     * @param entities  待插入的对象列表
     * @param <T>       实体类泛型
     * @return 成功插入的记录总数
     */
    <T> Integer bInsertBatch(String tableName, Collection<T> entities);

    /**
     * 分批次批量插入实体数据
     *
     * <p>自动将数据按批次拆分，分批插入
     *
     * @param tableName 目标表名
     * @param entities  待插入的对象列表
     * @param batchSize 每批次插入的条数（建议设置为1000-5000）
     * @param <T>       实体类泛型
     * @return 成功插入的记录总数
     */
    <T> Integer bInsertBatch(String tableName, Collection<T> entities, int batchSize);

    // ==================== 5. 插入或忽略（单条） ====================

    /**
     * 插入或忽略单条Map数据（基于数据库唯一约束）
     *
     * <p>基于数据库唯一索引/主键实现，遇到冲突时自动跳过
     *
     * @param tableName 目标表名
     * @param rowData   单行数据
     * @return 成功插入的行数（存在则返回0，不存在则返回1）
     */
    Integer bInsertIgnore(String tableName, Map<String, Object> rowData);

    /**
     * 插入或忽略单条Map数据（指定冲突键）
     *
     * <p>基于指定的唯一索引/主键字段判断冲突，避免重复插入，
     * 适用于无需更新仅需插入的场景（PostgreSQL：ON CONFLICT DO NOTHING；MySQL：INSERT IGNORE INTO）
     *
     * @param tableName    目标表名
     * @param rowData      单行数据
     * @param conflictKeys 冲突判定字段（唯一索引/主键字段列表）
     * @return 成功插入的行数（存在则返回0，不存在则返回1）
     */
    Integer bInsertIgnore(String tableName, Map<String, Object> rowData, List<String> conflictKeys);

    /**
     * 插入或忽略单条实体数据（表名从注解获取，默认冲突键为主键）
     *
     * @param entity 待插入的实体对象
     * @param <T>    实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertIgnore(T entity);

    /**
     * 插入或忽略单条实体数据（指定冲突键）
     *
     * @param entity       待插入的实体对象
     * @param conflictKeys 冲突判定字段
     * @param <T>          实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertIgnore(T entity, List<String> conflictKeys);

    /**
     * 插入或忽略单条实体数据（指定表名和冲突键）
     *
     * @param tableName    目标表名
     * @param entity       待插入的实体对象
     * @param conflictKeys 冲突判定字段
     * @param <T>          实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertIgnore(String tableName, T entity, List<String> conflictKeys);

    /**
     * 插入或忽略单条实体数据（支持驼峰转换控制）
     *
     * @param entity            待插入的实体对象
     * @param conflictKeys      冲突判定字段
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param <T>               实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertIgnore(T entity, List<String> conflictKeys, boolean isToUnderlineCase);

    /**
     * 插入或忽略单条实体数据（指定表名，支持驼峰转换）
     *
     * @param tableName         目标表名
     * @param entity            待插入的实体对象
     * @param conflictKeys      冲突判定字段
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param <T>               实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertIgnore(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase);

    /**
     * 插入或忽略单条实体数据（支持驼峰转换和空值控制）
     *
     * @param tableName         目标表名
     * @param entity            待插入的实体对象
     * @param conflictKeys      冲突判定字段
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue   是否忽略值为空的字段
     * @param <T>               实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertIgnore(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue);

    /**
     * 插入或忽略单条实体数据（支持指定忽略字段）
     *
     * @param tableName        目标表名
     * @param entity           待插入的实体对象
     * @param conflictKeys     冲突判定字段
     * @param ignoreFieldNames 需要忽略的字段名称列表
     * @param <T>              实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertIgnore(String tableName, T entity, List<String> conflictKeys, List<String> ignoreFieldNames);

    /**
     * 插入或忽略单条实体数据（完整参数版本）
     *
     * @param tableName         目标表名
     * @param entity            待插入的实体对象
     * @param conflictKeys      冲突判定字段
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue   是否忽略值为空的字段
     * @param ignoreFieldNames  需要忽略的字段名称列表
     * @param <T>               实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertIgnore(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames);

    /**
     * 插入或忽略单条实体数据（简化版）
     *
     * @param entity            待插入的实体对象
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param <T>               实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertIgnore(T entity, boolean isToUnderlineCase);

    /**
     * 插入或忽略单条实体数据（简化版，带忽略字段）
     *
     * @param entity           待插入的实体对象
     * @param conflictKeys     冲突判定字段
     * @param ignoreFieldNames 需要忽略的字段名称列表
     * @param <T>              实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertIgnore(T entity, List<String> conflictKeys, List<String> ignoreFieldNames);

    // ==================== 6. 选择性插入或忽略（自动过滤null） ====================

    /**
     * 选择性插入或忽略实体（自动过滤null值字段，表名从注解获取）
     *
     * <p>自动忽略实体中值为null的字段，只使用非null字段进行插入判断
     *
     * @param entity 待插入的实体对象
     * @param <T>    实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertSelectiveIgnore(T entity);

    /**
     * 选择性插入或忽略实体（指定冲突键）
     *
     * @param entity       待插入的实体对象
     * @param conflictKeys 冲突判定字段
     * @param <T>          实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertSelectiveIgnore(T entity, List<String> conflictKeys);

    /**
     * 选择性插入或忽略实体（指定表名和冲突键）
     *
     * @param tableName    目标表名
     * @param entity       待插入的实体对象
     * @param conflictKeys 冲突判定字段
     * @param <T>          实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertSelectiveIgnore(String tableName, T entity, List<String> conflictKeys);

    /**
     * 选择性插入或忽略实体（支持驼峰转换）
     *
     * @param entity            待插入的实体对象
     * @param conflictKeys      冲突判定字段
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param <T>               实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertSelectiveIgnore(T entity, List<String> conflictKeys, boolean isToUnderlineCase);

    /**
     * 选择性插入或忽略实体（支持驼峰转换，指定表名）
     *
     * @param entity            待插入的实体对象
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param <T>               实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertSelectiveIgnore(T entity, boolean isToUnderlineCase);

    /**
     * 选择性插入或忽略实体（支持指定忽略字段）
     *
     * @param entity           待插入的实体对象
     * @param conflictKeys     冲突判定字段
     * @param ignoreFieldNames 需要忽略的字段名称列表
     * @param <T>              实体类泛型
     * @return 成功插入的行数
     */
    <T> Integer bInsertSelectiveIgnore(T entity, List<String> conflictKeys, List<String> ignoreFieldNames);

    // ==================== 7. 批量插入或忽略 ====================

    /**
     * 批量插入或忽略Map数据
     *
     * <p>基于数据库唯一索引/主键实现批量插入，遇到冲突时自动跳过
     *
     * @param tableName    目标表名
     * @param headers      字段名集合
     * @param rowsData     多行数据列表
     * @param conflictKeys 冲突判定字段（唯一索引/主键字段列表）
     * @return 成功插入的记录总数（已存在的记录不计入）
     */
    Integer bInsertIgnoreBatch(String tableName, Set<String> headers, List<Map<String, Object>> rowsData, List<String> conflictKeys);
}
