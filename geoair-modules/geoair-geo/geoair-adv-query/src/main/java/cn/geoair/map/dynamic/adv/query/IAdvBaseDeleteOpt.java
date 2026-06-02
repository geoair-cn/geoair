package cn.geoair.map.dynamic.adv.query;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据删除相关的基础操作接口
 *
 * <p>覆盖单条删除、批量删除、自定义SQL删除、条件删除、逻辑删除、防误删保护等全场景，
 * 适配PostgreSQL/MySQL等主流数据库，兼顾性能与数据安全
 *
 * @author 张逢吉
 * @version 1.0
 */
public interface IAdvBaseDeleteOpt extends IAdvConfigOpt {

    /**
     * 设置数据源获取器
     *
     * <p>用于动态获取数据库连接，支持多数据源场景
     *
     * @param dataSourceGetter 数据源获取器，用于获取数据库连接
     */
    void setDataSourceGetter(IDataSourceGetter dataSourceGetter);

    // ==================== 自定义SQL删除（最灵活） ====================

    /**
     * 执行自定义删除SQL语句（无参数）
     *
     * <p>支持任意复杂的DELETE SQL，包括但不限于：
     * <ul>
     *     <li>多表关联删除：DELETE FROM user u USING dept d WHERE u.dept_id = d.id AND d.name = 'test'</li>
     *     <li>子查询删除：DELETE FROM user WHERE dept_id IN (SELECT id FROM dept WHERE status = 0)</li>
     *     <li>带函数条件的删除：DELETE FROM log WHERE create_time < NOW() - INTERVAL '30 days'</li>
     * </ul>
     *
     * <p><b>注意：</b>此方法不会添加任何额外的安全限制，请谨慎使用，避免误删数据
     *
     * @param sqlStatement 自定义删除SQL语句（支持MyBatis标签语法）
     *                     示例：DELETE FROM user WHERE create_time < '2024-01-01' AND status = 0
     * @return 受影响的行数
     * @throws cn.geoair.map.dynamic.adv.query.exception.SqlExecuteException SQL执行异常
     */
    Integer bDeleteBySql(String sqlStatement);

    /**
     * 执行带参数的自定义删除SQL语句
     *
     * <p>使用预编译参数绑定方式执行SQL，可有效防止SQL注入攻击。
     * 支持MyBatis风格的参数占位符（#{paramName}）
     *
     * <p><b>优势：</b>
     * <ul>
     *     <li>参数自动转义，避免SQL注入</li>
     *     <li>支持复杂参数类型（如集合、数组）</li>
     *     <li>可复用SQL模板，提升性能</li>
     * </ul>
     *
     * @param sqlStatement 自定义删除SQL语句（含参数占位符）
     *                     示例：DELETE FROM user WHERE dept_id = #{deptId} AND age > #{age}
     * @param sqlParam     SQL参数映射（key为占位符名称，value为参数值）
     *                     示例：SqlParamMap.create().put("deptId", 5).put("age", 60)
     * @return 受影响的行数
     * @throws cn.geoair.map.dynamic.adv.query.exception.SqlExecuteException SQL执行异常
     */
    Integer bDeleteBySql(String sqlStatement, SqlParamMap sqlParam);


    /**
     * 执行带参数的自定义删除SQL语句
     *
     * <p>解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param sqlStatement 自定义更新SQL语句（含参数占位符）
     *                     示例：DELETE FROM WHERE id =？
     * @param sqlParam     SQL参数映射 示例：[ 1001]
     * @return 受影响的行数
     */
    Integer bDeleteBySql(String sqlStatement, SqlParamList sqlParam);

    /**
     * 执行带参数的自定义删除SQL语句
     *
     * <p>解决纯SQL拼接的SQL注入问题，支持动态参数绑定
     *
     * @param sqlStatement 自定义删除SQL语句（含参数占位符）
     *                     示例：DELETE FROM WHERE id =？或者DELETE FROM WHERE id = #{id}
     * @param sqlParam     根据 GirSqlParam 具体类型自动识别
     * @return 受影响的行数
     */

    Integer bDeleteBySql(String sqlStatement, GirSqlParam sqlParam);


    // ==================== 主键删除（最常用） ====================

    /**
     * 根据单个主键值删除数据
     *
     * <p>适用于按主键删除单行数据的场景，这是最高效的删除方式，会使用主键索引。
     *
     * <p><b>使用场景：</b>
     * <ul>
     *     <li>根据ID删除单条记录</li>
     *     <li>前端删除操作</li>
     *     <li>撤销操作</li>
     * </ul>
     *
     * <p><b>注意：</b>如果主键值不存在，返回0，不会抛出异常
     *
     * @param tableName 目标表名（如：user、order）
     * @param idKey     主键字段名（如：id、user_id）
     * @param id        主键值（支持String、Long、Integer等类型）
     * @return 受影响的行数（成功删除返回1，无匹配数据返回0）
     * @throws IllegalArgumentException 当tableName、idKey或id为null时抛出
     */
    Integer bDeleteByPK(String tableName, String idKey, Object id);

    /**
     * 根据主键集合批量删除数据
     *
     * <p>底层优化为IN查询：DELETE FROM table WHERE id IN (?, ?, ...)
     * 适用于批量删除少量数据。
     *
     * <p><b>性能建议：</b>
     * <ul>
     *     <li>单次删除建议不超过1000条记录</li>
     *     <li>超过1000条请使用 {@link #bDeleteByPKs(String, String, Set, int)} 分批删除</li>
     *     <li>大量数据删除建议使用条件删除 {@link #bDeleteByMap(String, Map, int)}</li>
     * </ul>
     *
     * <p><b>示例：</b>
     * <pre>
     * Set&lt;Object&gt; ids = Set.of(1, 2, 3, 4, 5);
     * Integer count = bDeleteByPKs("user", "id", ids);
     * </pre>
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param ids       主键值集合（不能为null或空集合）
     * @return 受影响的行数（实际删除的记录数）
     * @throws IllegalArgumentException 当ids为null或空集合时抛出
     */
    Integer bDeleteByPKs(String tableName, String idKey, Set<Object> ids);

    /**
     * 分批次批量删除主键数据（避免IN子句参数过多）
     *
     * <p>自动将主键集合按批次拆分，每批次执行一次DELETE操作，
     * 适用于删除大量数据（如1万+条），避免单次SQL参数过多或锁表时间过长。
     *
     * <p><b>分批策略：</b>
     * <ul>
     *     <li>每批次独立事务（如果外层无事务）</li>
     *     <li>某批次失败不会影响已删除的批次</li>
     *     <li>返回所有批次成功删除的总行数</li>
     * </ul>
     *
     * <p><b>batchSize建议：</b>
     * <ul>
     *     <li>常规场景：1000-2000</li>
     *     <li>高性能场景：2000-5000</li>
     *     <li>避免超过10000（可能导致内存压力）</li>
     * </ul>
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param ids       主键值集合（不能为null或空集合）
     * @param batchSize 每批次删除的主键数量（必须大于0）
     * @return 受影响的总行数（所有批次成功删除的记录数之和）
     * @throws IllegalArgumentException 当ids为null/空集合，或batchSize小于等于0时抛出
     */
    Integer bDeleteByPKs(
            String tableName, String idKey, Set<Object> ids, int batchSize);

    // ==================== 条件删除 ====================

    /**
     * 按条件删除数据（Map格式条件，所有条件均为等值匹配）
     *
     * <p>根据传入的条件Map拼接WHERE子句，多个条件之间使用AND连接。
     * <b>注意：所有条件均为等值匹配（=），不支持比较运算符（如 >、<、>= 等）</b>
     *
     * <p><b>重要安全机制：</b>
     * <ul>
     *     <li><b>禁止传入空条件</b>：如果whereMap为null或空，会抛出异常，防止全表删除</li>
     *     <li>建议条件中至少包含一个索引字段，避免全表扫描</li>
     *     <li>生产环境建议配合逻辑删除使用，而非物理删除</li>
     * </ul>
     *
     * <p><b>使用示例：</b>
     * <pre>
     * Map&lt;String, Object&gt; whereMap = new HashMap&lt;&gt;();
     * whereMap.put("dept_id", 5);
     * whereMap.put("status", 0);
     * whereMap.put("name", "张三");
     * // 生成 SQL: DELETE FROM user WHERE dept_id = 5 AND status = 0 AND name = '张三'
     * Integer count = bDeleteByMap("user", whereMap);
     * </pre>
     *
     * <p><b>如需复杂条件（比较运算符、函数、OR逻辑等），请使用：</b>
     * <ul>
     *     <li>{@link #bDeleteByWhere(String, GirAdvWhereFilter)} - 支持复杂条件构建</li>
     *     <li>{@link #bDeleteByWhere(String, GirAdvWhereLambdaFilter)} - Lambda表达式版本</li>
     *     <li>{@link #bDeleteBySql(String, SqlParamMap)} - 自定义SQL</li>
     * </ul>
     *
     * @param tableName 目标表名
     * @param whereMap  删除条件（key=字段名，value=等值匹配的值）
     *                  示例：{ "dept_id": 5, "status": 0, "name": "张三" }
     * @return 受影响的行数（实际删除的记录数）
     * @throws IllegalArgumentException                                      当whereMap为null或空时抛出
     * @throws cn.geoair.map.dynamic.adv.query.exception.SqlExecuteException SQL执行异常
     */
    Integer bDeleteByMap(String tableName, Map<String, Object> whereMap);

    /**
     * 按条件批量删除（分批次执行，所有条件均为等值匹配）
     *
     * <p>适用于删除大量数据，将删除操作分批执行，避免单次删除锁表时间过长或事务过大。
     * <b>注意：所有条件均为等值匹配（=），不支持比较运算符（如 >、<、>= 等）</b>
     *
     * <p><b>执行机制：</b>
     * <ol>
     *     <li>先统计符合条件的总记录数：SELECT COUNT(*) FROM table WHERE ...</li>
     *     <li>按batchSize分批删除：DELETE FROM table WHERE ... LIMIT batchSize</li>
     *     <li>循环执行直到删除完毕</li>
     *     <li>返回所有批次删除的总行数</li>
     * </ol>
     *
     * <p><b>使用示例：</b>
     * <pre>
     * Map&lt;String, Object&gt; whereMap = new HashMap&lt;&gt;();
     * whereMap.put("status", 0);
     * whereMap.put("dept_id", 5);
     * // 分批删除状态为0且部门为5的用户，每批删除1000条
     * Integer count = bDeleteByMap("user", whereMap, 1000);
     * </pre>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *     <li>MySQL使用LIMIT，PostgreSQL需要不同的实现</li>
     *     <li>删除过程中新插入符合条件的数据可能会被遗漏</li>
     *     <li>建议配合索引字段使用，提高分页效率</li>
     *     <li>适合清理历史数据、过期数据等场景</li>
     *     <li>如需复杂条件，请使用自定义SQL方式</li>
     * </ul>
     *
     * @param tableName 目标表名
     * @param whereMap  删除条件（不能为null或空，所有条件均为等值匹配）
     * @param batchSize 每批次删除的行数（建议1000-5000）
     * @return 受影响的总行数（所有批次删除的记录数之和）
     * @throws IllegalArgumentException 当whereMap为null/空，或batchSize小于等于0时抛出
     */
    Integer bDeleteByMap(String tableName, Map<String, Object> whereMap, int batchSize);

    // ==================== Lambda条件删除（类型安全） ====================

    /**
     * 按Lambda条件删除数据（类型安全，支持复杂条件）
     *
     * <p>使用Lambda表达式构建类型安全的WHERE条件，避免字段名拼写错误，
     * 提供更好的IDE支持和重构友好性。支持复杂条件构建，包括比较运算符、函数、子查询等。
     *
     * <p><b>优势：</b>
     * <ul>
     *     <li>编译期检查：字段名错误在编译时就能发现</li>
     *     <li>IDE智能提示：自动补全实体类字段</li>
     *     <li>重构友好：实体类字段改名会自动同步</li>
     *     <li>支持复杂表达式和函数计算</li>
     * </ul>
     *
     * <p><b>使用示例：</b>
     * <pre>
     * // 基础等值条件：删除状态为0的用户
     * Integer count = bDeleteByWhere("user",
     *     GirAdvWhereLambdaFilter.of(User.class)
     *         .eq(User::getStatus, 0)
     * );
     *
     * // 多个条件（AND连接）
     * Integer count2 = bDeleteByWhere("user",
     *     GirAdvWhereLambdaFilter.of(User.class)
     *         .eq(User::getStatus, 0)
     *         .eq(User::getDeptId, 5)
     * );
     *
     * // 复杂条件：按年份删除
     * Integer count3 = bDeleteByWhere("user",
     *     GirAdvWhereLambdaFilter.of(User.class)
     *         .exprEq("YEAR(create_time)", 2024)
     * );
     *
     * // 计算值比较
     * Integer count4 = bDeleteByWhere("order",
     *     GirAdvWhereLambdaFilter.of(Order.class)
     *         .exprGt("price * quantity", 1000)
     * );
     *
     * // 字符串拼接模糊查询
     * Integer count5 = bDeleteByWhere("employee",
     *     GirAdvWhereLambdaFilter.of(Employee.class)
     *         .exprLike("CONCAT(first_name, ' ', last_name)", "张%")
     * );
     * </pre>
     *
     * <p><b>注意：</b>此方法禁止传入空条件，会抛出异常防止全表删除
     *
     * @param tableName   目标表名
     * @param whereFilter Lambda条件过滤器（用于构建WHERE条件，不能为null且不能为空条件）
     * @param <T>         实体类泛型
     * @return 受影响的行数（实际删除的记录数）
     * @throws IllegalArgumentException 当whereFilter为null或条件为空时抛出
     * @see GirAdvWhereLambdaFilter 条件构建器详细用法
     */
    <T> Integer bDeleteByWhere(String tableName, GirAdvWhereLambdaFilter<T> whereFilter);

    /**
     * 表名从GirAdvWhereLambdaFilter的泛型中拿到
     *
     * @param whereFilter 过滤条件
     * @param <T>
     * @return
     */
    <T> Integer bDeleteByWhere(GirAdvWhereLambdaFilter<T> whereFilter);

    /**
     * 按条件删除数据（传统方式，支持复杂条件）
     *
     * <p>使用传统的GirAdvWhereFilter构建WHERE条件，适合动态条件构建场景。
     * 支持复杂条件构建，包括比较运算符、函数、OR/AND组合等。
     *
     * <p><b>使用场景：</b>
     * <ul>
     *     <li>条件字段动态不确定</li>
     *     <li>需要复杂的条件组合（嵌套AND/OR）</li>
     *     <li>与其他模块兼容</li>
     * </ul>
     *
     * <p><b>使用示例：</b>
     * <pre>
     * // 基础等值条件
     * GirAdvWhereFilter filter = GirAdvWhereFilter.of()
     *         .eq("name", "张三")
     *         .eq("status", 1);
     * Integer count = bDeleteByWhere("user", filter);
     *
     * // 复杂表达式条件
     * GirAdvWhereFilter filter2 = GirAdvWhereFilter.of()
     *         .expr("YEAR(create_time)", AdvOperatorEnums.等于, 2024)
     *         .expr("price * quantity", AdvOperatorEnums.大于, 1000)
     *         .eq("status", 1);
     * Integer count2 = bDeleteByWhere("order", filter2);
     * </pre>
     *
     * <p><b>注意：</b>此方法禁止传入空条件，会抛出异常防止全表删除
     *
     * @param tableName   目标表名
     * @param whereFilter WHERE条件过滤器（不能为null且不能为空条件）
     * @param <T>         泛型参数（用于类型推断）
     * @return 受影响的行数（实际删除的记录数）
     * @throws IllegalArgumentException 当whereFilter为null或条件为空时抛出
     * @see GirAdvWhereFilter 条件构建器详细用法
     */
    <T> Integer bDeleteByWhere(String tableName, GirAdvWhereFilter whereFilter);


    <T> Integer bDeleteByPK(T entity);

    <T> Integer bDeleteByPK(T entity, boolean isToUnderlineCase);

    <T> Integer bDeleteByPK(String tableName, T entity);

    <T> Integer bDeleteByPK(String tableName, T entity, boolean isToUnderlineCase);



}
