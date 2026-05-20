package cn.geoair.map.dynamic.adv.query;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 数据更新相关的基础操作接口
 *
 * <p>覆盖单条更新、批量更新、自定义SQL更新、条件更新、UPSERT等全场景
 *
 * @author 张逢吉
 */
public interface IAdvBaseUpdateOpt extends IAdvConfigOpt {

    /**
     * 设置数据源获取器
     *
     * @param dataSourceGetter 数据源获取器，用于获取数据库连接
     */
    void setDataSourceGetter(IDataSourceGetter dataSourceGetter);

    // ==================== 自定义SQL更新 ====================

    /**
     * 执行自定义更新SQL语句（无参数）
     *
     * <p>支持任意复杂的UPDATE SQL（含多表关联更新、子查询更新等）
     *
     * @param sqlStatement 自定义更新SQL语句
     *                     示例：UPDATE user SET age = age + 1 WHERE id = 1001
     * @return 受影响的行数
     */
    Integer bUpdateBySql(String sqlStatement);

    /**
     * 执行带参数的自定义更新SQL语句
     *
     * <p>解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param dynamicSql 自定义更新SQL语句（含参数占位符）
     *                   示例：UPDATE user SET name = #{name} WHERE id = #{id}
     * @param sqlParam   SQL参数映射（key为占位符名称，value为参数值）
     *                   示例：{ "name": "张三", "id": 1001 }
     * @return 受影响的行数
     */
    Integer bUpdateBySql(String dynamicSql, SqlParamMap sqlParam);

    /**
     * 执行带参数的自定义更新SQL语句
     *
     * <p>解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param sqlStatement 自定义更新SQL语句（含参数占位符）
     *                     示例：UPDATE user SET name = ？ WHERE id =？
     * @param sqlParam     SQL参数映射 示例：["张三",1001]
     * @return 受影响的行数
     */
    Integer bUpdateBySql(String sqlStatement, SqlParamList sqlParam);

    /**
     * 执行带参数的自定义更新SQL语句
     *
     * <p>解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param sqlStatement 自定义更新SQL语句（含参数占位符）
     *                     示例：UPDATE user SET name = ？ WHERE id =？或者  UPDATE user SET name = #{name} WHERE id = #{id}
     * @param sqlParam     根据GirSqlParam 具体类型自动识别
     * @return 受影响的行数
     */

    Integer bUpdateBySql(String sqlStatement, GirSqlParam sqlParam);

    // ==================== 单条数据更新（标准化） ====================

    /**
     * 根据主键更新单条数据（Map格式）
     *
     * <p>适用于按主键更新指定字段的场景，字段名与Map的key一一对应
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名（如：id）
     * @param id        主键值
     * @param rowData   待更新的字段-值映射（key=字段名，value=新值）
     *                  示例：{ "name": "张三", "age": 25 }
     * @return 受影响的行数（成功返回1，无匹配数据返回0）
     */
    Integer bUpdateByPK(
            String tableName, String idKey, Object id, Map<String, Object> rowData);

    /**
     * 根据主键更新单条实体对象（自动提取属性，默认忽略空值）
     *
     * <p>自动提取实体对象的非空属性作为更新字段，主键字段用于WHERE条件
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param entity    待更新的实体对象（需包含主键值，值为null的字段会被忽略）
     * @param <T>       实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPK(String tableName, String idKey, T entity);

    /**
     * 根据主键更新单条实体对象（完整参数版本）
     *
     * <p>适用于面向对象的单条数据更新，对象属性名需与表字段名匹配
     *
     * @param tableName         目标表名（如：user）
     * @param idKey             主键字段名
     * @param entity            待更新的Java对象（如User实体类）
     * @param isToUnderlineCase 是否将驼峰属性名转换为下划线字段名（true: userName -> user_name）
     * @param ignoreNullValue   是否忽略值为null的字段（true: null字段不参与更新）
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPK(String tableName, String idKey, T entity, boolean isToUnderlineCase, boolean ignoreNullValue);

    /**
     * 根据主键更新单条实体对象（简化参数版本，默认忽略null值）
     *
     * <p>适用于面向对象的单条数据更新，对象属性名需与表字段名匹配
     *
     * @param tableName         目标表名（如：user）
     * @param idKey             主键字段名
     * @param entity            待更新的Java对象（如User实体类）
     * @param isToUnderlineCase 是否将驼峰属性名转换为下划线字段名（true: userName -> user_name）
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPK(String tableName, String idKey, T entity, boolean isToUnderlineCase);

    /**
     * 根据主键更新单条实体对象（完整参数版本，支持指定忽略字段）
     *
     * <p>适用于面向对象的单条数据更新，对象属性名需与表字段名匹配
     *
     * @param tableName         目标表名（如：user）
     * @param idKey             主键字段名
     * @param entity            待更新的Java对象（如User实体类）
     * @param isToUnderlineCase 是否将驼峰属性名转换为下划线字段名
     * @param ignoreNullValue   是否忽略值为null的字段
     * @param ignoreFieldNames  需要忽略的字段名称列表（实体类中的属性名，非数据库字段名）
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPK(String tableName, String idKey, T entity, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames);

    /**
     * 根据主键更新单条实体对象（选择性更新，忽略null值字段）
     *
     * <p>自动忽略实体中值为null的字段，只更新非null字段
     *
     * @param tableName         目标表名（如：user）
     * @param idKey             主键字段名
     * @param entity            待更新的Java对象（如User实体类）
     * @param isToUnderlineCase 是否将驼峰属性名转换为下划线字段名
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity, boolean isToUnderlineCase);

    /**
     * 根据主键更新单条实体对象（选择性更新，忽略null值字段，默认驼峰转下划线）
     *
     * <p>自动忽略实体中值为null的字段，只更新非null字段，属性名自动转换为下划线格式
     *
     * @param tableName 目标表名（如：user）
     * @param idKey     主键字段名
     * @param entity    待更新的Java对象（如User实体类）
     * @param <T>       实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity);

    /**
     * 根据主键更新单条实体对象（选择性更新，支持指定忽略字段）
     *
     * <p>自动忽略实体中值为null的字段以及指定的字段，只更新非null且不在忽略列表中的字段
     *
     * @param tableName        目标表名（如：user）
     * @param idKey            主键字段名
     * @param entity           待更新的Java对象（如User实体类）
     * @param ignoreFieldNames 需要忽略的字段名称列表（实体类中的属性名，非数据库字段名）
     * @param <T>              实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity, List<String> ignoreFieldNames);

    /**
     * 条件更新数据（Map格式条件，所有条件均为等值匹配）
     *
     * <p>适用于按自定义条件更新，需确保条件能唯一定位到单行数据，避免全表更新风险。
     * <b>注意：whereMap中的所有条件均为等值匹配（=），不支持比较运算符（如 >、<、>= 等）</b>
     *
     * <p><b>使用示例：</b>
     * <pre>
     * Map&lt;String, Object&gt; rowData = new HashMap&lt;&gt;();
     * rowData.put("status", 1);
     * rowData.put("update_time", new Date());
     *
     * Map&lt;String, Object&gt; whereMap = new HashMap&lt;&gt;();
     * whereMap.put("dept_id", 5);
     * whereMap.put("name", "张三");
     * // 生成 SQL: UPDATE user SET status = 1, update_time = NOW() WHERE dept_id = 5 AND name = '张三'
     * Integer count = bUpdateByMap("user", rowData, whereMap);
     * </pre>
     *
     * <p><b>如需复杂条件（比较运算符、函数、OR逻辑等），请使用：</b>
     * <ul>
     *     <li>{@link #bUpdateByWhere(String, Map, GirAdvWhereFilter)} - 支持复杂条件构建</li>
     *     <li>{@link #bUpdateByWhere(String, Object, GirAdvWhereLambdaFilter)} - Lambda表达式版本</li>
     *     <li>{@link #bUpdateBySql(String, SqlParamMap)} - 自定义SQL</li>
     * </ul>
     *
     * @param tableName 目标表名
     * @param rowData   待更新的字段-值映射（key=字段名，value=新值）
     * @param whereMap  更新条件（key=字段名，value=等值匹配的值，多个条件拼接为AND）
     *                  示例：{ "dept_id": 5, "status": 0 }
     * @return 受影响的行数
     * @throws IllegalArgumentException 当whereMap为null或空时抛出（防止全表更新）
     */
    Integer bUpdateByMap(
            String tableName, Map<String, Object> rowData, Map<String, Object> whereMap);

    // ==================== 批量更新 ====================

    /**
     * 批量更新数据（按主键批量更新）
     *
     * <p>适用于批量更新不同主键的多条数据，每条数据的更新字段可不同
     * 使用CASE WHEN或逐条执行方式实现
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param rowsData  批量更新数据（每个Map需包含主键字段和待更新字段）
     *                  示例：[{ "id": 1, "age": 25 }, { "id": 2, "name": "李四" }]
     * @return 受影响的总行数
     */
    Integer bUpdateBatchByPK(
            String tableName, String idKey, List<Map<String, Object>> rowsData);

    /**
     * 分批次批量更新（避免单次批量更新数据量过大）
     *
     * <p>将大数据量分批执行，防止SQL过长或内存溢出
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param rowsData  批量更新数据
     * @param batchSize 每批次更新条数（建议值：1000-5000）
     * @return 受影响的总行数
     */
    Integer bUpdateBatchWithBatchSize(
            String tableName, String idKey, List<Map<String, Object>> rowsData, int batchSize);

    /**
     * 批量更新实体对象列表（按主键）
     *
     * <p>自动提取实体对象的属性作为更新字段，批量执行更新操作
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param entities  待更新的实体对象列表（每个对象需包含主键值）
     * @param <T>       实体类泛型
     * @return 受影响的总行数
     */
    <T> Integer bUpdateBatchByPK(String tableName, String idKey, Collection<T> entities);

    // ==================== 特殊场景更新 ====================

    /**
     * 更新或插入（UPSERT）- 已废弃，请使用 bUpsert 方法
     *
     * <p>先尝试更新，无匹配数据则插入，等价于 INSERT ... ON CONFLICT UPDATE
     *
     * @param tableName    目标表名
     * @param rowData      待更新/插入的字段-值映射
     * @param conflictKeys 冲突判定字段（唯一索引字段或主键字段列表）
     * @return 受影响的行数（更新返回1，插入返回1，无变化返回0）
     * @deprecated 请使用 {@link #bUpsert(String, Map, List)} 替代
     */
    @Deprecated
    Integer bUpdateOrInsert(
            String tableName, Map<String, Object> rowData, List<String> conflictKeys);

    /**
     * 更新或插入（UPSERT）- Map参数版本
     *
     * <p>先尝试更新，无匹配数据则插入，等价于 INSERT ... ON CONFLICT UPDATE
     *
     * @param tableName    目标表名
     * @param rowData      待更新/插入的字段-值映射
     * @param conflictKeys 冲突判定字段（唯一索引字段或主键字段列表）
     * @return 受影响的行数（更新返回1，插入返回1，无变化返回0）
     */
    Integer bUpsert(String tableName, Map<String, Object> rowData, List<String> conflictKeys);

    /**
     * 更新或插入（UPSERT）
     * ID字段从entity的注解中获取，表名从注解中获取
     * <p>适用于面向对象的UPSERT操作，对象属性名需与表字段名匹配
     *
     * @param entity 待操作的Java对象（如User实体类）
     * @param <T>    实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsert(T entity);

    /**
     * 更新或插入（UPSERT）
     * ID字段从entity的注解中获取
     * <p>适用于面向对象的UPSERT操作，对象属性名需与表字段名匹配
     *
     * @param tableName 目标表名（如：user）
     * @param entity    待操作的Java对象（如User实体类）
     * @param <T>       实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsert(String tableName, T entity);

    /**
     * 更新或插入（UPSERT）
     *
     * <p>适用于面向对象的UPSERT操作，对象属性名需与表字段名匹配
     *
     * @param tableName    目标表名（如：user）
     * @param entity       待操作的Java对象（如User实体类）
     * @param conflictKeys 冲突判定字段（唯一索引字段或主键字段列表）
     * @param <T>          实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys);

    /**
     * 更新或插入（UPSERT）
     *
     * <p>适用于面向对象的UPSERT操作，支持驼峰转换和空值过滤
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待操作的Java对象（如User实体类）
     * @param conflictKeys      冲突判定字段（唯一索引字段或主键字段列表）
     * @param isToUnderlineCase 是否将驼峰属性名转换为下划线字段名
     * @param ignoreNullValue   是否忽略值为null的字段
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue);

    /**
     * 更新或插入（UPSERT）- 简化参数版本（默认忽略null值）
     *
     * <p>适用于面向对象的UPSERT操作，支持驼峰转换，默认忽略null值字段
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待操作的Java对象（如User实体类）
     * @param conflictKeys      冲突判定字段（唯一索引字段或主键字段列表）
     * @param isToUnderlineCase 是否将驼峰属性名转换为下划线字段名
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase);

    /**
     * 更新或插入（UPSERT）
     *
     * <p>适用于面向对象的UPSERT操作，支持驼峰转换、空值过滤和字段忽略
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待操作的Java对象（如User实体类）
     * @param conflictKeys      冲突判定字段（唯一索引字段或主键字段列表）
     * @param isToUnderlineCase 是否将驼峰属性名转换为下划线字段名
     * @param ignoreNullValue   是否忽略值为null的字段
     * @param ignoreFieldNames  需要忽略的字段名称列表（实体类中的属性名，非数据库字段名）
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames);

    /**
     * 更新或插入（UPSERT）
     *
     * <p>适用于面向对象的UPSERT操作，支持驼峰转换、空值过滤和字段忽略
     *
     * @param tableName        目标表名（如：user）
     * @param entity           待操作的Java对象（如User实体类）
     * @param conflictKeys     冲突判定字段（唯一索引字段或主键字段列表）
     * @param ignoreFieldNames 需要忽略的字段名称列表（实体类中的属性名，非数据库字段名）
     * @param <T>              实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, List<String> ignoreFieldNames);


    /**
     * 更新或插入（UPSERT）- 选择性更新版本（忽略null值字段）
     *
     * <p>自动忽略实体中值为null的字段，只使用非null字段进行UPSERT操作
     *
     * @param tableName         目标表名（如：user）
     * @param entity            待操作的Java对象（如User实体类）
     * @param conflictKeys      冲突判定字段（唯一索引字段或主键字段列表）
     * @param isToUnderlineCase 是否将驼峰属性名转换为下划线字段名
     * @param <T>               实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase);

    /**
     * 更新或插入（UPSERT）- 选择性更新版本（默认驼峰转下划线）
     *
     * <p>自动忽略实体中值为null的字段，只使用非null字段进行UPSERT操作
     *
     * @param tableName    目标表名（如：user）
     * @param entity       待操作的Java对象（如User实体类）
     * @param conflictKeys 冲突判定字段（唯一索引字段或主键字段列表）
     * @param <T>          实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys);

    /**
     * 更新或插入（UPSERT）- 选择性更新版本（默认驼峰转下划线）
     *
     * <p>自动忽略实体中值为null的字段，只使用非null字段进行UPSERT操作
     *
     * @param tableName 目标表名（如：user）
     * @param entity    待操作的Java对象（如User实体类）
     * @param <T>       实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsertSelective(String tableName, T entity);

    /**
     * 更新或插入（UPSERT）- 选择性更新版本（默认驼峰转下划线）
     *
     * <p>自动忽略实体中值为null的字段，只使用非null字段进行UPSERT操作
     *
     * @param entity 待操作的Java对象（如User实体类）
     * @param <T>    实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsertSelective(T entity);

    /**
     * 更新或插入（UPSERT）- 选择性更新版本（支持指定忽略字段）
     *
     * <p>自动忽略实体中值为null的字段以及指定的字段，使用剩余字段进行UPSERT操作
     *
     * @param tableName        目标表名（如：user）
     * @param entity           待操作的Java对象（如User实体类）
     * @param conflictKeys     冲突判定字段（唯一索引字段或主键字段列表）
     * @param ignoreFieldNames 需要忽略的字段名称列表（实体类中的属性名，非数据库字段名）
     * @param <T>              实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys, List<String> ignoreFieldNames);

    // ==================== 条件更新（Lambda表达式版本） ====================

    /**
     * 根据Lambda条件更新实体（支持指定忽略字段）
     *
     * <p>使用Lambda表达式构建类型安全的WHERE条件，避免字段名拼写错误。
     * 支持复杂条件构建，包括比较运算符、函数、子查询等。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * // 更新用户状态
     * User user = new User();
     * user.setStatus(1);
     * user.setUpdateTime(new Date());
     *
     * Integer count = bUpdateByWhere("user", user,
     *     GirAdvWhereLambdaFilter.of(User.class)
     *         .eq(User::getDeptId, 5)
     *         .gt(User::getAge, 60)
     * );
     * </pre>
     *
     * @param tableName        目标表名
     * @param entity           待更新的实体对象（非null字段参与更新）
     * @param whereFilter      Lambda条件过滤器（用于构建WHERE条件）
     * @param ignoreFieldNames 需要忽略的字段名称列表
     * @param <T>              实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames);

    /**
     * 根据Lambda条件更新实体（默认不忽略任何字段）
     *
     * <p>使用Lambda表达式构建类型安全的WHERE条件，避免字段名拼写错误。
     * 支持复杂条件构建，包括比较运算符、函数、子查询等。
     *
     * @param tableName   目标表名
     * @param entity      待更新的实体对象
     * @param whereFilter Lambda条件过滤器（用于构建WHERE条件）
     * @param <T>         实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter);

    /**
     * 根据Map条件更新（传统方式，支持复杂条件）
     *
     * <p>使用GirAdvWhereFilter构建复杂的WHERE条件，支持比较运算符、函数、OR/AND组合等。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * Map&lt;String, Object&gt; rowData = new HashMap&lt;&gt;();
     * rowData.put("status", 1);
     * rowData.put("update_time", new Date());
     *
     * // 复杂条件：按年份和计算值过滤
     * GirAdvWhereFilter filter = GirAdvWhereFilter.of()
     *         .expr("YEAR(create_time)", AdvOperatorEnums.等于, 2024)
     *         .expr("price * quantity", AdvOperatorEnums.大于, 1000)
     *         .eq("status", 1);
     *
     * Integer count = bUpdateByWhere("order", rowData, filter);
     * </pre>
     *
     * @param tableName   目标表名
     * @param rowData     待更新的字段-值映射
     * @param whereFilter WHERE条件过滤器（支持复杂条件构建）
     * @param <T>         泛型参数
     * @return 受影响的行数
     */
    <T> Integer bUpdateByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter);

    /**
     * 根据Lambda条件选择性更新实体（忽略null值字段，支持指定忽略字段）
     *
     * <p>自动忽略实体中值为null的字段以及指定的字段，只更新非null且不在忽略列表中的字段。
     * 使用Lambda表达式构建类型安全的WHERE条件。
     *
     * @param tableName        目标表名
     * @param entity           待更新的实体对象
     * @param whereFilter      Lambda条件过滤器
     * @param ignoreFieldNames 需要忽略的字段名称列表
     * @param <T>              实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateSelectiveByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames);
    /**
     * 根据Lambda条件选择性更新实体（忽略null值字段）
     *
     * <p>自动忽略实体中值为null的字段，只更新非null字段。
     * 使用Lambda表达式构建类型安全的WHERE条件。
     *

     * @param entity      待更新的实体对象
     * @param whereFilter Lambda条件过滤器
     * @param <T>         实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateSelectiveByWhere(T entity, GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames);

    /**
     * 根据Lambda条件选择性更新实体（忽略null值字段）
     *
     * <p>自动忽略实体中值为null的字段，只更新非null字段。
     * 使用Lambda表达式构建类型安全的WHERE条件。
     *
     * @param tableName   目标表名
     * @param entity      待更新的实体对象
     * @param whereFilter Lambda条件过滤器
     * @param <T>         实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateSelectiveByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter);

    /**
     * 根据Lambda条件选择性更新实体（忽略null值字段）
     *
     * <p>自动忽略实体中值为null的字段，只更新非null字段。
     * 使用Lambda表达式构建类型安全的WHERE条件。
     *

     * @param entity      待更新的实体对象
     * @param whereFilter Lambda条件过滤器
     * @param <T>         实体类泛型
     * @return 受影响的行数
     */
    <T> Integer bUpdateSelectiveByWhere(T entity, GirAdvWhereLambdaFilter<T> whereFilter);

    /**
     * 根据Map条件选择性更新（过滤value为空的字段，支持复杂条件）
     *
     * <p>自动过滤rowData中value为null或空字符串的字段，只更新有效字段。
     * 使用GirAdvWhereFilter构建复杂的WHERE条件。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * Map&lt;String, Object&gt; rowData = new HashMap&lt;&gt;();
     * rowData.put("status", 1);
     * rowData.put("name", "张三");
     * rowData.put("remark", null);  // 会被自动过滤
     *
     * // 复杂条件
     * GirAdvWhereFilter filter = GirAdvWhereFilter.of()
     *         .expr("YEAR(create_time)", AdvOperatorEnums.等于, 2024)
     *         .eq("dept_id", 5);
     *
     * Integer count = bUpdateSelectiveByWhere("user", rowData, filter);
     * </pre>
     *
     * @param tableName   目标表名
     * @param rowData     待更新的字段-值映射（会自动过滤空值）
     * @param whereFilter WHERE条件过滤器（支持复杂条件构建）
     * @param <T>         泛型参数
     * @return 受影响的行数
     */
    <T> Integer bUpdateSelectiveByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter);
}
