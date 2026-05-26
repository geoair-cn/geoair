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
 * 数据更新基础操作接口
 *
 * <p>提供全场景的数据更新能力，包括：
 * <ul>
 *   <li>自定义SQL更新（防SQL注入）</li>
 *   <li>按主键单条更新（支持Map和实体）</li>
 *   <li>批量更新（支持分批次）</li>
 *   <li>UPSERT（更新或插入）</li>
 *   <li>复杂条件更新（Lambda表达式 + 传统Filter）</li>
 * </ul>
 *
 * <p><b>使用建议：</b>
 * <ul>
 *   <li>简单场景使用 {@link #bUpdateByPK} 或 {@link #bUpdateByMap}</li>
 *   <li>复杂条件使用 {@link #bUpdateByWhere} 系列（Lambda类型安全）</li>
 *   <li>高性能批量操作使用 {@link #bUpdateBatchByPK}</li>
 *   <li>存在则更新、不存在则插入使用 {@link #bUpsert} 系列</li>
 * </ul>
 *
 * @author 张逢吉
 */
public interface IAdvBaseUpdateOpt extends IAdvConfigOpt {

    /**
     * 设置数据源获取器
     *
     * <p>用于获取数据库连接，支持动态数据源切换场景
     *
     * @param dataSourceGetter 数据源获取器实现，可为空（使用默认数据源）
     */
    void setDataSourceGetter(IDataSourceGetter dataSourceGetter);

    // ==================== 1. 自定义SQL更新（最灵活） ====================

    /**
     * 执行无参数的自定义更新SQL语句
     *
     * <p>适用于不需要参数绑定的简单更新场景，如批量状态重置、定时任务等。
     * 支持多表关联更新、子查询更新等复杂SQL。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * // 全表年龄加1
     * Integer count = bUpdateBySql("UPDATE user SET age = age + 1");
     *
     * // 多表关联更新
     * Integer count = bUpdateBySql(
     *     "UPDATE orders o SET o.status = 'CLOSED' " +
     *     "WHERE o.create_time &lt; '2024-01-01' AND o.status = 'PENDING'"
     * );
     * </pre>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>SQL语句直接拼接存在注入风险，建议仅在无外部输入时使用</li>
     *   <li>对于带用户输入的更新，请使用带参数的重载方法</li>
     *   <li>注意事务边界，大事务可能影响性能</li>
     * </ul>
     *
     * @param sqlStatement 完整的更新SQL语句，不包含任何占位符
     * @return 受影响的行数（-1表示执行失败）
     */
    Integer bUpdateBySql(String sqlStatement);

    /**
     * 执行带位置占位符(?)的自定义更新SQL
     *
     * <p>使用JDBC标准的问号占位符，按顺序绑定参数，有效防止SQL注入。
     * 适用于参数数量固定、顺序明确的场景。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * // 按ID更新用户名
     * Integer count = bUpdateBySql(
     *     "UPDATE user SET name = ?, update_time = ? WHERE id = ?",
     *     SqlParamList.of("张三", new Date(), 1001)
     * );
     *
     * // 批量更新状态（IN条件）
     * Integer count = bUpdateBySql(
     *     "UPDATE order SET status = ? WHERE id IN (?, ?, ?)",
     *     SqlParamList.of("COMPLETED", 101, 102, 103)
     * );
     * </pre>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>占位符数量必须与参数数量完全一致，否则抛出异常</li>
     *   <li>参数顺序严格对应SQL中?的出现顺序</li>
     *   <li>复杂类型（如Date）会自动转换为数据库兼容格式</li>
     * </ul>
     *
     * @param sqlStatement 包含?占位符的更新SQL
     * @param sqlParam     参数列表（顺序对应占位符）
     * @return 受影响的行数
     */
    Integer bUpdateBySql(String sqlStatement, SqlParamList sqlParam);

    /**
     * 执行带命名占位符(#{name})的自定义更新SQL
     *
     * <p>使用MyBatis风格的命名占位符，通过Map绑定参数，可读性强且参数顺序无关。
     * 适用于参数较多、字段名清晰的场景。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * Map&lt;String, Object&gt; params = new HashMap&lt;&gt;();
     * params.put("userName", "张三");
     * params.put("userAge", 25);
     * params.put("userId", 1001);
     *
     * Integer count = bUpdateBySql(
     *     "UPDATE user SET name = #{userName}, age = #{userAge} WHERE id = #{userId}",
     *     SqlParamMap.of(params)
     * );
     *
     * // 使用构建器
     * Integer count = bUpdateBySql(
     *     "UPDATE product SET price = #{newPrice} WHERE category = #{category}",
     *     SqlParamMap.builder()
     *         .put("newPrice", 99.9)
     *         .put("category", "ELECTRONICS")
     *         .build()
     * );
     * </pre>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>占位符名称必须与Map中的key完全匹配（包括大小写）</li>
     *   <li>未在Map中定义的占位符会抛出异常</li>
     *   <li>支持嵌套属性访问（如#{user.name}），需要参数对象支持</li>
     * </ul>
     *
     * @param dynamicSql 包含#{name}命名占位符的更新SQL
     * @param sqlParam   参数名-值映射
     * @return 受影响的行数
     */
    Integer bUpdateBySql(String dynamicSql, SqlParamMap sqlParam);

    /**
     * 执行自动识别参数类型的自定义更新SQL
     *
     * <p>自动判断传入的GirSqlParam是列表类型还是Map类型，
     * 分别调用对应的重载方法，简化调用方代码。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * // 自动识别为位置占位符模式
     * GirSqlParam paramList = SqlParamList.of("张三", 1001);
     * bUpdateBySql("UPDATE user SET name = ? WHERE id = ?", paramList);
     *
     * // 自动识别为命名占位符模式
     * GirSqlParam paramMap = SqlParamMap.of("name", "张三", "id", 1001);
     * bUpdateBySql("UPDATE user SET name = #{name} WHERE id = #{id}", paramMap);
     * </pre>
     *
     * @param sqlStatement 支持?或#{name}占位符的更新SQL
     * @param sqlParam     参数对象（自动识别类型）
     * @return 受影响的行数
     */
    Integer bUpdateBySql(String sqlStatement, GirSqlParam sqlParam);

    // ==================== 2. 按主键单条更新 ====================

    /**
     * 根据主键更新单条数据（Map格式）
     *
     * <p>最基础的按主键更新方式，通过Map指定需要更新的字段。
     * 适用于动态字段更新场景，如前端只提交了部分字段。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * Map&lt;String, Object&gt; rowData = new HashMap&lt;&gt;();
     * rowData.put("name", "张三");
     * rowData.put("age", 25);
     * rowData.put("update_time", new Date());
     *
     * // 更新id=1001的用户信息
     * Integer count = bUpdateByPK("user", "id", 1001, rowData);
     * // 生成SQL: UPDATE user SET name=?, age=?, update_time=? WHERE id=?
     * </pre>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>rowData中的key必须与数据库字段名完全一致</li>
     *   <li>主键值(id)不能为空，否则无法定位记录</li>
     *   <li>如果rowData为空，方法会直接返回0（不执行SQL）</li>
     *   <li>返回0表示没有匹配的主键记录，不代表执行失败</li>
     * </ul>
     *
     * @param tableName 目标表名（如：user、order）
     * @param idKey     主键字段名（如：id、user_id）
     * @param id        主键值（支持String、Integer、Long等类型）
     * @param rowData   待更新的字段-值映射（key=字段名，value=新值）
     * @return 受影响的行数（1=更新成功，0=未找到对应记录）
     */
    Integer bUpdateByPK(String tableName, String idKey, Object id, Map<String, Object> rowData);

    /**
     * 根据主键更新单条实体对象（自动提取属性，默认忽略空值）
     *
     * <p>将实体对象的非空属性自动转换为更新字段，主键字段用于WHERE条件。
     * 适用于面向对象的更新操作，减少手动构建Map的代码量。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * User user = new User();
     * user.setId(1001);
     * user.setName("张三");
     * user.setAge(25);
     * // age=null的属性会被自动忽略
     *
     * Integer count = bUpdateByPK("user", "id", user);
     * // 生成SQL: UPDATE user SET name=? WHERE id=?
     * </pre>
     *
     * <p><b>默认行为：</b>
     * <ul>
     *   <li>自动将驼峰属性名转换为下划线字段名（userName → user_name）</li>
     *   <li>值为null的属性不参与更新（选择性更新）</li>
     *   <li>实体类需包含主键字段的getter方法</li>
     * </ul>
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param entity    待更新的实体对象（需包含主键值）
     * @param <T>       实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPK(String tableName, String idKey, T entity);

    /**
     * 根据主键更新单条实体对象（简化参数版本）
     *
     * <p>相比默认版本，允许控制是否进行驼峰转下划线。
     * 其他行为与默认版本一致（忽略null值）。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * // 不转换驼峰（属性名直接作为字段名）
     * Integer count = bUpdateByPK("user", "id", user, false);
     *
     * // 转换驼峰（userName → user_name）
     * Integer count = bUpdateByPK("user", "id", user, true);
     * </pre>
     *
     * @param tableName         目标表名
     * @param idKey             主键字段名
     * @param entity            待更新的实体对象
     * @param isToUnderlineCase true=驼峰转下划线，false=保持原样
     * @param <T>               实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPK(String tableName, String idKey, T entity, boolean isToUnderlineCase);

    /**
     * 根据主键更新单条实体对象（完整参数版本）
     *
     * <p>提供最完整的参数控制，可分别控制驼峰转换和空值忽略行为。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * // 不转换驼峰，且更新null值字段（将数据库字段设为null）
     * Integer count = bUpdateByPK("user", "id", user, false, false);
     *
     * // 转换驼峰，但忽略null值字段
     * Integer count = bUpdateByPK("user", "id", user, true, true);
     * </pre>
     *
     * <p><b>ignoreNullValue参数说明：</b>
     * <ul>
     *   <li>true（推荐）：忽略null值字段，只更新非null字段</li>
     *   <li>false：包含null值字段（会将数据库对应字段设为NULL）</li>
     * </ul>
     *
     * @param tableName         目标表名
     * @param idKey             主键字段名
     * @param entity            待更新的实体对象
     * @param isToUnderlineCase 是否驼峰转下划线
     * @param ignoreNullValue   是否忽略null值字段
     * @param <T>               实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPK(String tableName, String idKey, T entity,
                            boolean isToUnderlineCase, boolean ignoreNullValue);

    /**
     * 根据主键更新单条实体对象（支持指定忽略字段）
     *
     * <p>在完整参数基础上，额外支持指定需要忽略的字段列表。
     * 忽略的字段无论值是否为null，都不会参与更新。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * User user = new User();
     * user.setId(1001);
     * user.setName("张三");
     * user.setAge(25);
     * user.setPassword("newPassword");
     *
     * // 忽略password字段，即使有值也不更新
     * List&lt;String&gt; ignoreFields = Arrays.asList("password", "createTime");
     * Integer count = bUpdateByPK("user", "id", user, true, true, ignoreFields);
     * </pre>
     *
     * <p><b>忽略字段说明：</b>
     * <ul>
     *   <li>ignoreFieldNames是实体类中的属性名，不是数据库字段名</li>
     *   <li>即使ignoreNullValue=false，被忽略的字段也不会更新</li>
     *   <li>主键字段会自动排除，无需手动添加</li>
     * </ul>
     *
     * @param tableName         目标表名
     * @param idKey             主键字段名
     * @param entity            待更新的实体对象
     * @param isToUnderlineCase 是否驼峰转下划线
     * @param ignoreNullValue   是否忽略null值字段
     * @param ignoreFieldNames  需要忽略的实体属性名列表
     * @param <T>               实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPK(String tableName, String idKey, T entity,
                            boolean isToUnderlineCase, boolean ignoreNullValue,
                            List<String> ignoreFieldNames);

    // ==================== 3. 选择性更新（按主键，自动过滤null） ====================

    /**
     * 根据主键选择性更新实体（支持驼峰转换控制）
     *
     * <p>"选择性更新"语义明确：自动忽略实体中值为null的字段，只更新非null字段。
     * 相比{@link #bUpdateByPK(String, String, Object, Map)}，语义更清晰。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * User user = new User();
     * user.setId(1001);
     * user.setName("张三");  // 只更新name
     * // age为null，不会参与更新
     *
     * Integer count = bUpdateByPKSelective("user", "id", user, true);
     * </pre>
     *
     * @param tableName         目标表名
     * @param idKey             主键字段名
     * @param entity            待更新的实体对象
     * @param isToUnderlineCase 是否驼峰转下划线
     * @param <T>               实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity, boolean isToUnderlineCase);

    /**
     * 根据主键选择性更新实体（默认驼峰转下划线）
     *
     * <p>最简洁的选择性更新调用方式，自动完成驼峰转下划线并忽略null值。
     * 适用于大部分常规场景。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * User user = new User();
     * user.setId(1001);
     * user.setUserName("张三");  // 自动转换为 user_name
     * user.setAge(25);
     *
     * Integer count = bUpdateByPKSelective("user", "id", user);
     * </pre>
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param entity    待更新的实体对象
     * @param <T>       实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity);

    /**
     * 根据主键选择性更新实体（支持指定忽略字段）
     *
     * <p>在选择性更新的基础上，额外支持指定需要忽略的字段。
     * 被忽略的字段即使有值也不会参与更新。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * User user = new User();
     * user.setId(1001);
     * user.setName("张三");
     * user.setPassword("123456");
     * user.setUpdateTime(new Date());
     *
     * // 忽略password和updateTime，只更新name
     * List&lt;String&gt; ignoreFields = Arrays.asList("password", "updateTime");
     * Integer count = bUpdateByPKSelective("user", "id", user, ignoreFields);
     * </pre>
     *
     * @param tableName        目标表名
     * @param idKey            主键字段名
     * @param entity           待更新的实体对象
     * @param ignoreFieldNames 需要忽略的实体属性名列表
     * @param <T>              实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByPKSelective(String tableName, String idKey, T entity, List<String> ignoreFieldNames);

    // ==================== 4. 简单条件更新（仅等值匹配） ====================

    /**
     * 通过等值条件更新数据（Map格式）
     *
     * <p>最简单的条件更新方式，所有条件均为等值匹配（=），多个条件之间为AND关系。
     * 适用于条件简单、不需要复杂表达式的场景。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * // 待更新的数据
     * Map&lt;String, Object&gt; rowData = new HashMap&lt;&gt;();
     * rowData.put("status", 1);
     * rowData.put("update_time", new Date());
     *
     * // 更新条件（部门id=5 且 状态=0）
     * Map&lt;String, Object&gt; whereMap = new HashMap&lt;&gt;();
     * whereMap.put("dept_id", 5);
     * whereMap.put("status", 0);
     *
     * Integer count = bUpdateByMap("user", rowData, whereMap);
     * // 生成SQL: UPDATE user SET status=1, update_time=? WHERE dept_id=5 AND status=0
     * </pre>
     *
     * <p><b>⚠️ 重要安全限制：</b>
     * <ul>
     *   <li>whereMap不能为null或空，否则抛出{@link IllegalArgumentException}</li>
     *   <li>此限制是为了防止误操作导致全表更新</li>
     *   <li>如需无条件更新（全表），请使用自定义SQL方法</li>
     * </ul>
     *
     * <p><b>⚠️ 功能限制：</b>
     * <ul>
     *   <li>仅支持等值匹配(=)，不支持 >、&lt;、>=、LIKE、IN等操作</li>
     *   <li>如需复杂条件，请使用{@link #bUpdateByWhere(String, Map, GirAdvWhereFilter)}</li>
     *   <li>如需OR逻辑，请使用Lambda表达式版本</li>
     * </ul>
     *
     * @param tableName 目标表名
     * @param rowData   待更新的字段-值映射（key=字段名，value=新值）
     * @param whereMap  更新条件（key=字段名，value=等值匹配的值，多个条件AND拼接）
     * @return 受影响的行数
     * @throws IllegalArgumentException 当whereMap为null或空时抛出
     */
    Integer bUpdateByMap(String tableName, Map<String, Object> rowData, Map<String, Object> whereMap);



    /**
     * 批量按主键更新（每条数据可更新不同字段）
     * 一次发送多条update语句
     * <p>一次性更新多条记录，每条记录可以有完全不同的更新字段。
     * 框架会根据数据情况选择最优执行策略（CASE WHEN或逐条执行）。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * List&lt;Map&lt;String, Object&gt;&gt; rowsData = new ArrayList&lt;&gt;();
     *
     * Map&lt;String, Object&gt; row1 = new HashMap&lt;&gt;();
     * row1.put("id", 1);
     * row1.put("name", "张三");
     *
     * Map&lt;String, Object&gt; row2 = new HashMap&lt;&gt;();
     * row2.put("id", 2);
     * row2.put("age", 25);
     * row2.put("email", "lisi@example.com");
     *
     * rowsData.add(row1);
     * rowsData.add(row2);
     *
     * Integer totalCount = bUpdateBatchByPK("user", "id", rowsData);
     * </pre>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>每个Map中必须包含主键字段（idKey）</li>
     *   <li>数据量过大时（如超过5000条），建议使用分批次版本</li>
     *   <li>所有更新在同一个事务中执行</li>
     *   <li>返回值是所有记录影响行数的总和</li>
     * </ul>
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param rowsData  批量更新数据列表（每个Map包含主键和待更新字段）
     * @return 受影响的总行数
     */
    Integer bUpdateBatchByPK(String tableName, String idKey, List<Map<String, Object>> rowsData);

    /**
     * 分批次批量更新（避免单次数据量过大）
     * 一次发送多条update语句
     * <p>将大数据量分批执行，每批次独立提交（根据事务配置）。
     * 适用于更新大量数据（如数万条）的场景，可避免SQL过长和内存溢出。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * List&lt;Map&lt;String, Object&gt;&gt; rowsData = get10000Records();
     *
     * // 每批1000条
     * Integer totalCount = bUpdateBatchWithBatchSize("user", "id", rowsData, 1000);
     * </pre>
     *
     * <p><b>批次大小建议：</b>
     * <ul>
     *   <li>简单更新：2000-5000条/批</li>
     *   <li>复杂更新（含大字段）：500-1000条/批</li>
     *   <li>网络延迟较高时：适当减小批次大小</li>
     * </ul>
     *
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>如果外层开启了事务，所有批次在同一事务中</li>
     *   <li>如果无事务，每批次独立提交，部分失败时前序批次不会回滚</li>
     *   <li>建议根据实际场景调整batchSize</li>
     * </ul>
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param rowsData  批量更新数据列表
     * @param batchSize 每批次更新条数（建议1000-5000）
     * @return 受影响的总行数
     */
    Integer bUpdateBatchByPK(String tableName, String idKey,
                             List<Map<String, Object>> rowsData, int batchSize);

    /**
     * 批量更新实体对象列表（按主键）
     *
     * <p>面向对象的批量更新方式，自动提取实体属性并批量执行。
     * 内部会将实体列表转换为Map列表进行处理。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * List&lt;User&gt; userList = new ArrayList&lt;&gt;();
     *
     * User user1 = new User();
     * user1.setId(1);
     * user1.setName("张三");
     *
     * User user2 = new User();
     * user2.setId(2);
     * user2.setAge(25);
     *
     * userList.add(user1);
     * userList.add(user2);
     *
     * Integer totalCount = bUpdateBatchByPK("user", "id", userList);
     * </pre>
     *
     * <p><b>默认行为：</b>
     * <ul>
     *   <li>自动驼峰转下划线</li>
     *   <li>忽略null值字段</li>
     *   <li>自动过滤不包含主键的实体</li>
     * </ul>
     *
     * @param tableName 目标表名
     * @param idKey     主键字段名
     * @param entities  待更新的实体对象集合
     * @param <T>       实体类型
     * @return 受影响的总行数
     */
    <T> Integer bUpdateBatchByPK(String tableName, String idKey, Collection<T> entities);

    // ==================== 6. UPSERT（更新或插入） ====================

    /**
     * UPSERT操作（Map参数版本）
     *
     * <p>根据冲突键判断：如果记录存在则更新，不存在则插入。
     * 等价于数据库的 INSERT ... ON CONFLICT UPDATE 语法。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * Map&lt;String, Object&gt; rowData = new HashMap&lt;&gt;();
     * rowData.put("id", 1001);
     * rowData.put("name", "张三");
     * rowData.put("age", 25);
     * rowData.put("create_time", new Date());
     *
     * // 当id冲突时，更新name和age
     * List&lt;String&gt; conflictKeys = Arrays.asList("id");
     * Integer count = bUpsert("user", rowData, conflictKeys);
     * </pre>
     *
     * <p><b>冲突键说明：</b>
     * <ul>
     *   <li>通常是主键字段或唯一索引字段</li>
     *   <li>多个冲突键时，任一冲突即触发更新</li>
     *   <li>确保数据库表上有对应的唯一约束</li>
     * </ul>
     *
     * @param tableName    目标表名
     * @param rowData      待插入/更新的字段映射
     * @param conflictKeys 冲突判定字段列表（唯一索引或主键）
     * @return 受影响行数（插入返回1，更新返回1，无变化返回0）
     */
    Integer bUpsert(String tableName, Map<String, Object> rowData, List<String> conflictKeys);

    /**
     * UPSERT操作（实体版本，表名和主键从注解获取）
     *
     * <p>通过实体类上的注解自动获取表名和主键字段，
     * 冲突键自动使用主键字段。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * &#64;Table(name = "user")
     * public class User {
     *     &#64;Id
     *     private Long id;
     *     private String name;
     *     private Integer age;
     *     // getters/setters
     * }
     *
     * User user = new User();
     * user.setId(1001L);
     * user.setName("张三");
     * user.setAge(25);
     *
     * Integer count = bUpsert(user);
     * </pre>
     *
     * @param entity 待操作的实体对象（需包含表名和主键注解）
     * @param <T>    实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsert(T entity);

    /**
     * UPSERT操作（指定表名，主键从注解获取）
     *
     * <p>手动指定表名，但主键字段仍从实体注解中获取。
     * 适用于实体注解表名与实际需要不一致的场景。
     *
     * @param tableName 目标表名（覆盖实体注解中的表名）
     * @param entity    待操作的实体对象
     * @param <T>       实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsert(String tableName, T entity);

    /**
     * UPSERT操作（指定表名和冲突键）
     *
     * <p>完全手动控制表名和冲突键，实体属性自动映射。
     *
     * @param tableName    目标表名
     * @param entity       待操作的实体对象
     * @param conflictKeys 冲突判定字段列表
     * @param <T>          实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys);

    /**
     * UPSERT操作（支持驼峰转换控制）
     *
     * @param tableName         目标表名
     * @param entity            待操作的实体对象
     * @param conflictKeys      冲突判定字段列表
     * @param isToUnderlineCase 是否驼峰转下划线
     * @param <T>               实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase);

    /**
     * UPSERT操作（支持驼峰转换和空值控制）
     *
     * @param tableName         目标表名
     * @param entity            待操作的实体对象
     * @param conflictKeys      冲突判定字段列表
     * @param isToUnderlineCase 是否驼峰转下划线
     * @param ignoreNullValue   是否忽略null值字段（true=null字段不参与更新）
     * @param <T>               实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys,
                        boolean isToUnderlineCase, boolean ignoreNullValue);

    /**
     * UPSERT操作（支持指定忽略字段）
     *
     * @param tableName        目标表名
     * @param entity           待操作的实体对象
     * @param conflictKeys     冲突判定字段列表
     * @param ignoreFieldNames 需要忽略的实体属性名列表
     * @param <T>              实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys, List<String> ignoreFieldNames);

    /**
     * UPSERT操作（完整参数版本）
     *
     * @param tableName         目标表名
     * @param entity            待操作的实体对象
     * @param conflictKeys      冲突判定字段列表
     * @param isToUnderlineCase 是否驼峰转下划线
     * @param ignoreNullValue   是否忽略null值字段
     * @param ignoreFieldNames  需要忽略的实体属性名列表
     * @param <T>               实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsert(String tableName, T entity, List<String> conflictKeys,
                        boolean isToUnderlineCase, boolean ignoreNullValue,
                        List<String> ignoreFieldNames);

    // ----- 选择性UPSERT（自动过滤null）-----

    /**
     * 选择性UPSERT（仅实体，自动过滤null）
     *
     * <p>自动忽略实体中值为null的字段，只使用非null字段进行UPSERT操作。
     * 表名和主键从实体注解获取。
     *
     * @param entity 待操作的实体对象
     * @param <T>    实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsertSelective(T entity);

    /**
     * 选择性UPSERT（指定表名）
     *
     * @param tableName 目标表名
     * @param entity    待操作的实体对象
     * @param <T>       实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsertSelective(String tableName, T entity);

    /**
     * 选择性UPSERT（指定表名和冲突键）
     *
     * @param tableName    目标表名
     * @param entity       待操作的实体对象
     * @param conflictKeys 冲突判定字段列表
     * @param <T>          实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys);

    /**
     * 选择性UPSERT（支持驼峰转换控制）
     *
     * @param tableName         目标表名
     * @param entity            待操作的实体对象
     * @param conflictKeys      冲突判定字段列表
     * @param isToUnderlineCase 是否驼峰转下划线
     * @param <T>               实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys, boolean isToUnderlineCase);

    /**
     * 选择性UPSERT（支持指定忽略字段）
     *
     * @param tableName        目标表名
     * @param entity           待操作的实体对象
     * @param conflictKeys     冲突判定字段列表
     * @param ignoreFieldNames 需要忽略的实体属性名列表
     * @param <T>              实体类型
     * @return 受影响行数
     */
    <T> Integer bUpsertSelective(String tableName, T entity, List<String> conflictKeys, List<String> ignoreFieldNames);

    /**
     * @deprecated 已废弃，请使用 {@link #bUpsert(String, Map, List)} 替代
     */
    @Deprecated
    Integer bUpdateOrInsert(String tableName, Map<String, Object> rowData, List<String> conflictKeys);

    // ==================== 7. 复杂条件更新（Lambda表达式） ====================

    /**
     * 根据Lambda条件更新实体（类型安全）
     *
     * <p>使用Lambda表达式构建WHERE条件，编译期类型检查，避免字段名拼写错误。
     * 支持复杂条件：比较运算符、IN、LIKE、BETWEEN、函数、子查询等。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * User updateData = new User();
     * updateData.setStatus(1);
     * updateData.setUpdateTime(new Date());
     *
     * // 复杂条件：部门=5 AND (年龄>60 OR 入职年份<2020)
     * Integer count = bUpdateByWhere("user", updateData,
     *     GirAdvWhereLambdaFilter.of(User.class)
     *         .eq(User::getDeptId, 5)
     *         .and(sub -> sub
     *             .gt(User::getAge, 60)
     *             .or()
     *             .lt(User::getHireYear, 2020)
     *         )
     * );
     * </pre>
     *
     * <p><b>支持的操作符：</b>
     * <ul>
     *   <li>比较：eq、ne、gt、ge、lt、le</li>
     *   <li>范围：in、notIn、between、notBetween</li>
     *   <li>模糊：like、notLike、startsWith、endsWith、contains</li>
     *   <li>空值：isNull、isNotNull</li>
     *   <li>逻辑：and、or、not</li>
     *   <li>表达式：expr（支持SQL函数和运算）</li>
     * </ul>
     *
     * @param tableName   目标表名
     * @param entity      待更新的实体对象（非null字段参与更新）
     * @param whereFilter Lambda条件过滤器
     * @param <T>         实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter);

    /**
     * 根据Lambda条件更新实体（支持指定忽略字段）
     *
     * @param tableName        目标表名
     * @param entity           待更新的实体对象
     * @param whereFilter      Lambda条件过滤器
     * @param ignoreFieldNames 需要忽略的实体属性名列表
     * @param <T>              实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateByWhere(String tableName, T entity,
                               GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames);

    // ----- 选择性条件更新（自动过滤null）-----

    /**
     * 根据Lambda条件选择性更新实体（自动过滤null字段）
     *
     * <p>自动忽略实体中值为null的字段，只更新非null字段。
     * 适用于前端提交部分字段的场景。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * User user = new User();
     * user.setName("张三");  // 只更新name
     * // age为null，不会参与更新
     *
     * Integer count = bUpdateSelectiveByWhere(user,
     *     GirAdvWhereLambdaFilter.of(User.class)
     *         .eq(User::getDeptId, 5)
     *         .eq(User::getStatus, 0)
     * );
     * </pre>
     *
     * @param entity      待更新的实体对象
     * @param whereFilter Lambda条件过滤器
     * @param <T>         实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateSelectiveByWhere(T entity, GirAdvWhereLambdaFilter<T> whereFilter);

    /**
     * 根据Lambda条件选择性更新实体（指定表名）
     *
     * @param tableName   目标表名
     * @param entity      待更新的实体对象
     * @param whereFilter Lambda条件过滤器
     * @param <T>         实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateSelectiveByWhere(String tableName, T entity, GirAdvWhereLambdaFilter<T> whereFilter);

    /**
     * 根据Lambda条件选择性更新实体（支持指定忽略字段）
     *
     * @param entity           待更新的实体对象
     * @param whereFilter      Lambda条件过滤器
     * @param ignoreFieldNames 需要忽略的实体属性名列表
     * @param <T>              实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateSelectiveByWhere(T entity, GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames);

    /**
     * 根据Lambda条件选择性更新实体（完整参数版本）
     *
     * @param tableName        目标表名
     * @param entity           待更新的实体对象
     * @param whereFilter      Lambda条件过滤器
     * @param ignoreFieldNames 需要忽略的实体属性名列表
     * @param <T>              实体类型
     * @return 受影响的行数
     */
    <T> Integer bUpdateSelectiveByWhere(String tableName, T entity,
                                        GirAdvWhereLambdaFilter<T> whereFilter, List<String> ignoreFieldNames);

    // ==================== 8. 复杂条件更新（传统Filter） ====================

    /**
     * 使用传统Filter构建复杂条件更新（Map格式）
     *
     * <p>通过{@link GirAdvWhereFilter}构建WHERE条件，支持表达式、函数、子查询等。
     * 适用于无法使用Lambda表达式的场景（如动态条件构建）。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * Map&lt;String, Object&gt; rowData = new HashMap&lt;&gt;();
     * rowData.put("status", 1);
     * rowData.put("update_time", new Date());
     *
     * // 构建复杂条件
     * GirAdvWhereFilter filter = GirAdvWhereFilter.of()
     *     .expr("YEAR(create_time)", AdvOperatorEnums.等于, 2024)
     *     .expr("price * quantity", AdvOperatorEnums.大于, 1000)
     *     .in("category", Arrays.asList("A", "B", "C"))
     *     .like("name", "%测试%");
     *
     * Integer count = bUpdateByWhere("order", rowData, filter);
     * </pre>
     *
     * <p><b>适用场景：</b>
     * <ul>
     *   <li>需要SQL函数支持（如YEAR、DATE_FORMAT）</li>
     *   <li>需要字段运算（如 price * quantity > 1000）</li>
     *   <li>运行时动态构建条件</li>
     *   <li>无法使用Lambda表达式的遗留代码</li>
     * </ul>
     *
     * @param tableName   目标表名
     * @param rowData     待更新的字段-值映射
     * @param whereFilter WHERE条件过滤器
     * @param <T>         泛型参数
     * @return 受影响的行数
     */
    <T> Integer bUpdateByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter);

    /**
     * 使用传统Filter选择性更新（自动过滤rowData中的空值）
     *
     * <p>自动过滤rowData中value为null或空字符串的字段，只更新有效字段。
     * 适用于不确定哪些字段有值的动态更新场景。
     *
     * <p><b>使用示例：</b>
     * <pre>
     * Map&lt;String, Object&gt; rowData = new HashMap&lt;&gt;();
     * rowData.put("status", 1);
     * rowData.put("name", "张三");
     * rowData.put("remark", null);      // 会被自动过滤
     * rowData.put("description", "");    // 空字符串也会被过滤
     *
     * GirAdvWhereFilter filter = GirAdvWhereFilter.of()
     *     .eq("dept_id", 5)
     *     .in("level", Arrays.asList(1, 2, 3));
     *
     * Integer count = bUpdateSelectiveByWhere("user", rowData, filter);
     * // 实际更新: SET status=1, name='张三' WHERE dept_id=5 AND level IN (1,2,3)
     * </pre>
     *
     * <p><b>过滤规则：</b>
     * <ul>
     *   <li>value == null → 过滤</li>
     *   <li>value instanceof String && ((String)value).isEmpty() → 过滤</li>
     *   <li>其他情况 → 保留</li>
     * </ul>
     *
     * @param tableName   目标表名
     * @param rowData     待更新的字段-值映射（自动过滤空值）
     * @param whereFilter WHERE条件过滤器
     * @param <T>         泛型参数
     * @return 受影响的行数
     */
    <T> Integer bUpdateSelectiveByWhere(String tableName, Map<String, Object> rowData, GirAdvWhereFilter whereFilter);
}
