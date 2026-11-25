package cn.geoair.map.dynamic.adv.query.wherequery.queryr;

import static cn.geoair.map.dynamic.adv.query.utils.LambdaUtils.getColumnName;

import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsKeyTran;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.enums.AdvNullHandling;
import cn.geoair.map.dynamic.adv.query.utils.LambdaUtils;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.SFunction;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import java.util.*;
import java.util.function.Consumer;
import lombok.Getter;

/**
 * QueryRequest 的 Lambda 风格 Builder 对象
 *
 * <p>支持两种构建模式：
 *
 * <ul>
 *   <li>模式一：对象组装SQL（通过表名、字段、条件等自动生成SQL）
 *   <li>模式二：自定义SQL（完全自定义SQL语句）
 * </ul>
 *
 * <p>使用示例：
 *
 * <pre>
 * // 模式一：Lambda风格对象组装
 * GirAdvQueryRequest request = QueryRequestBuilder.&lt;User&gt;create(User.class)
 *     .table("user")
 *     .fields(User::getId, User::getName, User::getAge)
 *     .where(w -> w.eq(User::getStatus, 1).ge(User::getAge, 18))
 *     .orderByDesc(User::getCreateTime)
 *     .page(1, 10)
 *     .build();
 *
 * // 模式二：自定义SQL
 * GirAdvQueryRequest request2 = QueryRequestBuilder.create()
 *     .customSql("SELECT * FROM user WHERE status = 1")
 *     .page(1, 10)
 *     .build();
 * </pre>
 *
 * @param <T> 实体类型
 * @author 张俊
 * @date Created in 2026/5/18 19:27
 */
public class QueryRequestBuilder<T> {

    // ==================== 模式一参数 ====================

    /** 实体类型 */
    @Getter private final Class<T> entityClass;

    /** 是否驼峰转下划线 */
    boolean isToUnderlineCase;

    /** 表名或一个完整带结果的SQL */
    @Getter private String tableOrSqlView;

    /** SQL视图别名 */
    @Getter private String sqlViewTableNameAlias;

    /** 查询字段名列表 */
    private List<String> columnNames;

    /**
     * 表达式字段名称 类似于这样的函数表达式 "COUNT", "SUM", "AVG", "MAX", "MIN", "CONCAT", "SUBSTR", "LENGTH", "NOW",
     * "DATE", "YEAR", "MONTH", "DAY", "TRIM"
     */
    private List<String> exprColumnNames;

    /** WHERE条件参数映射 */
    @Getter private GirAdvWhereFilter whereOption;

    /** NULL值处理策略（默认INCLUDE） */
    @Getter private AdvNullHandling nullHandling = AdvNullHandling.INCLUDE;

    /** 排序参数列表 */
    @Getter private final List<OrderApo> orders = new ArrayList<>();

    /** 页码 */
    @Getter private Integer pageNum;

    /** 每页条数 */
    @Getter private Integer pageSize;

    /** 页码起始规则（默认false，从1开始） */
    @Getter private Boolean pageNumStartZero = false;

    // ==================== 模式二参数 ====================

    /** 自定义SQL语句 */
    @Getter private String customSql;

    /** 空间操作规则 */
    @Getter private AdvEnumsGeomOpt advEnumsGeomOpt;

    /** 是否返回字段元数据（默认false） */
    @Getter private Boolean hasFieldsInfo = false;

    /** key的转换策略 */
    @Getter private AdvEnumsKeyTran advEnumsKeyTran = AdvEnumsKeyTran.不转换;

    /** 字段映射（数据库字段名 -> 返回字段名） */
    @Getter private Map<String, String> fieldMapping;

    /** 是否去重查询 */
    @Getter private Boolean distinct;

    /** GROUP BY字段列表 */
    @Getter private List<String> groupByFields;

    /** HAVING条件过滤器 */
    @Getter private GirAdvWhereFilter havingFilter;

    // ==================== 构造函数 ====================

    public QueryRequestBuilder(Class<T> entityClass) {
        this(entityClass, true);
    }

    public QueryRequestBuilder(Class<T> entityClass, boolean isToUnderlineCase) {
        this.entityClass = entityClass;
        this.isToUnderlineCase = isToUnderlineCase;
    }

    // ==================== 模式一：对象组装SQL方法（Lambda风格） ====================

    /**
     * 设置表名
     *
     * @param tableName 表名
     * @return Builder实例
     */
    public QueryRequestBuilder<T> table(String tableName) {
        this.tableOrSqlView = tableName;
        return this;
    }

    public QueryRequestBuilder<T> sqlViewTableNameAlias(String sqlViewTableNameAlias) {
        this.sqlViewTableNameAlias = sqlViewTableNameAlias;
        return this;
    }

    /**
     * 设置表名或SQL视图（带别名）
     *
     * @param tableOrSqlView 表名或SQL视图
     * @param sqlViewTableNameAlias SQL视图别名
     * @return Builder实例
     */
    public QueryRequestBuilder<T> table(String tableOrSqlView, String sqlViewTableNameAlias) {
        this.tableOrSqlView = tableOrSqlView;
        this.sqlViewTableNameAlias = sqlViewTableNameAlias;
        return this;
    }

    /**
     * 设置查询字段（Lambda表达式）
     *
     * @param columns 字段Lambda表达式
     * @return Builder实例
     */
    @SafeVarargs
    public final QueryRequestBuilder<T> fields(SFunction<T, ?>... columns) {
        this.columnNames = new ArrayList<>();
        for (SFunction<T, ?> column : columns) {
            this.columnNames.add(getColumnName(column, isToUnderlineCase));
        }
        return this;
    }

    /**
     * 设置查询字段名列表
     *
     * @param columnNames 字段名列表
     * @return Builder实例
     */
    public QueryRequestBuilder<T> fields(List<String> columnNames) {
        this.columnNames = columnNames;
        return this;
    }

    /**
     * 设置查询字段名（可变参数）
     *
     * @param columnNames 字段名数组
     * @return Builder实例
     */
    public QueryRequestBuilder<T> fields(String... columnNames) {
        this.columnNames = ListUtil.toList(columnNames);
        return this;
    }

    /**
     * 添加查询字段
     *
     * @param columnNames 字段名数组
     * @return Builder实例
     */
    public QueryRequestBuilder<T> field(String... columnNames) {
        if (this.columnNames == null) {
            this.columnNames = new ArrayList<>();
        }
        this.columnNames.addAll(ListUtil.toList(columnNames));
        return this;
    }

    /**
     * 设置查询字段（带别名）
     *
     * @param column 字段Lambda表达式
     * @param alias 字段别名
     * @return Builder实例
     */
    public QueryRequestBuilder<T> field(SFunction<T, ?> column, String alias) {
        if (this.columnNames == null) {
            this.columnNames = new ArrayList<>();
        }
        String columnName = getColumnName(column, isToUnderlineCase);
        this.columnNames.add(columnName + " AS " + alias);

        // 记录字段映射
        if (this.fieldMapping == null) {
            this.fieldMapping = new HashMap<>();
        }
        this.fieldMapping.put(columnName, alias);
        return this;
    }

    /**
     * 批量添加查询字段
     *
     * @param columns 字段Lambda表达式数组
     * @return Builder实例
     */
    @SafeVarargs
    public final QueryRequestBuilder<T> addFields(SFunction<T, ?>... columns) {
        if (this.columnNames == null) {
            this.columnNames = new ArrayList<>();
        }
        for (SFunction<T, ?> column : columns) {
            this.columnNames.add(getColumnName(column, isToUnderlineCase));
        }
        return this;
    }

    /**
     * 添加SQL表达式字段
     *
     * @param sqlExpression SQL表达式（如 "COUNT(*)", "SUM(amount)"）
     * @param alias 别名
     * @return Builder实例
     */
    public QueryRequestBuilder<T> fieldExprAs(String sqlExpression, String alias) {
        if (this.exprColumnNames == null) {
            this.exprColumnNames = new ArrayList<>();
        }
        this.exprColumnNames.add(sqlExpression + " AS " + alias);
        return this;
    }

    /**
     * 添加SQL表达式字段
     *
     * @return Builder实例
     */
    public QueryRequestBuilder<T> fieldExpr(List<String> sqlExpressions) {
        this.exprColumnNames = sqlExpressions;
        return this;
    }

    /**
     * 添加SQL表达式字段
     *
     * @return Builder实例
     */
    public QueryRequestBuilder<T> fieldExpr(String... sqlExpressions) {
        this.exprColumnNames = ListUtil.toList(sqlExpressions);
        return this;
    }

    /**
     * 设置WHERE条件（Lambda风格）
     *
     * @param consumer Lambda条件构建器
     * @return Builder实例
     */
    public QueryRequestBuilder<T> whereLambda(Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        GirAdvWhereLambdaFilter<T> lambdaFilter =
                GirAdvWhereLambdaFilter.of(entityClass, isToUnderlineCase);
        consumer.accept(lambdaFilter);
        this.whereOption = lambdaFilter.toWhereFilter();
        return this;
    }

    /**
     * 设置WHERE条件
     *
     * @param consumer Lambda条件构建器
     * @return Builder实例
     */
    public QueryRequestBuilder<T> where(Consumer<GirAdvWhereFilter> consumer) {
        GirAdvWhereFilter whereFilter = GirAdvWhereFilter.of();
        consumer.accept(whereFilter);
        this.whereOption = whereFilter;
        return this;
    }

    /**
     * 设置WHERE条件（直接传入WhereFilter）
     *
     * @param whereOption WHERE条件过滤器
     * @return Builder实例
     */
    public QueryRequestBuilder<T> where(GirAdvWhereFilter whereOption) {
        this.whereOption = whereOption;
        return this;
    }

    /** 设置忽略NULL值 */
    public QueryRequestBuilder<T> ignoreNull() {
        this.nullHandling = AdvNullHandling.IGNORE;
        return this;
    }

    /** 设置NULL值处理策略 */
    public QueryRequestBuilder<T> nullHandling(AdvNullHandling nullHandling) {
        this.nullHandling = nullHandling;
        return this;
    }

    /**
     * 添加字段排序（升序）
     *
     * @param columnName 字段名
     * @return Builder实例
     */
    public QueryRequestBuilder<T> orderByAsc(String columnName) {
        this.orders.add(OrderApo.ofASCFieldName(columnName));
        return this;
    }

    /**
     * 添加字段排序（降序）
     *
     * @param columnName 字段名
     * @return Builder实例
     */
    public QueryRequestBuilder<T> orderByDesc(String columnName) {
        this.orders.add(OrderApo.ofDescFieldName(columnName));
        return this;
    }

    /**
     * 添加字段排序（自定义方向）
     *
     * @param columnName 字段名
     * @param direction 排序方向枚举
     * @return Builder实例
     */
    public QueryRequestBuilder<T> orderByField(String columnName, AdvEnumsOrder direction) {
        this.orders.add(OrderApo.ofFieldName(columnName, direction));
        return this;
    }

    /**
     * 直接添加OrderApo排序对象
     *
     * @param order OrderApo排序对象
     * @return Builder实例
     */
    public QueryRequestBuilder<T> order(OrderApo order) {
        this.orders.add(order);
        return this;
    }

    /**
     * 批量添加排序条件
     *
     * <p>会清空之前设置的排序条件
     *
     * @param orders 排序参数列表
     * @return Builder实例
     */
    public QueryRequestBuilder<T> orders(List<OrderApo> orders) {
        this.orders.clear();
        this.orders.addAll(orders);
        return this;
    }

    /**
     * 升序排序
     *
     * @param column 排序字段Lambda表达式
     * @return Builder实例
     */
    public QueryRequestBuilder<T> orderByAsc(SFunction<T, ?> column) {
        this.orders.add(OrderApo.ofASCFieldName(getColumnName(column, isToUnderlineCase)));
        return this;
    }

    /**
     * 降序排序
     *
     * @param column 排序字段Lambda表达式
     * @return Builder实例
     */
    public QueryRequestBuilder<T> orderByDesc(SFunction<T, ?> column) {
        this.orders.add(OrderApo.ofDescFieldName(getColumnName(column, isToUnderlineCase)));
        return this;
    }

    /**
     * 自定义排序
     *
     * @param column 排序字段Lambda表达式
     * @param direction 排序方向
     * @return Builder实例
     */
    public QueryRequestBuilder<T> orderBy(SFunction<T, ?> column, AdvEnumsOrder direction) {
        this.orders.add(OrderApo.ofFieldName(getColumnName(column, isToUnderlineCase), direction));
        return this;
    }

    /**
     * 添加函数排序（升序）
     *
     * <p>示例：orderByAscSFunction("CAST(gtc_id AS numeric)")
     *
     * @param function 排序函数表达式
     * @return Builder实例
     */
    public QueryRequestBuilder<T> orderByAscFunction(String function) {
        this.orders.add(OrderApo.ofASCFunction(function));
        return this;
    }

    /**
     * 添加函数排序（降序）
     *
     * @param function 排序函数表达式
     * @return Builder实例
     */
    public QueryRequestBuilder<T> orderByDescFunction(String function) {
        this.orders.add(OrderApo.ofDescFunction(function));
        return this;
    }

    /**
     * 添加函数排序（自定义方向）
     *
     * @param function 排序函数表达式
     * @param direction 排序方向
     * @return Builder实例
     */
    public QueryRequestBuilder<T> orderByFunction(String function, AdvEnumsOrder direction) {
        this.orders.add(OrderApo.ofFunction(function, direction));
        return this;
    }

    /** 清空排序条件 */
    public QueryRequestBuilder<T> clearOrders() {
        this.orders.clear();
        return this;
    }

    // ==================== 分页相关方法 ====================

    /**
     * 设置分页参数（页码从1开始）
     *
     * @param pageNum 页码（从1开始）
     * @param pageSize 每页条数
     * @return Builder实例
     */
    public QueryRequestBuilder<T> page(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pageNumStartZero = false;
        return this;
    }

    public QueryRequestBuilder<T> pageNumStartZero(boolean pageNumStartZero) {
        this.pageNumStartZero = pageNumStartZero;
        return this;
    }

    /**
     * 设置分页参数（自定义起始页码）
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param pageNumStartZero 页码起始规则（true=从0开始，false=从1开始）
     * @return Builder实例
     */
    public QueryRequestBuilder<T> page(int pageNum, int pageSize, boolean pageNumStartZero) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pageNumStartZero = pageNumStartZero;
        return this;
    }

    /** 设置每页条数 */
    public QueryRequestBuilder<T> pageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /** 设置页码 */
    public QueryRequestBuilder<T> pageNum(int pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    /** 不进行分页（查询所有） */
    public QueryRequestBuilder<T> noPage() {
        this.pageNum = null;
        this.pageSize = null;
        return this;
    }

    // ==================== 模式二：自定义SQL方法 ====================

    /** 设置自定义SQL语句 */
    public QueryRequestBuilder<T> customSql(String customSql) {
        this.customSql = customSql;
        return this;
    }

    /** 设置空间操作规则 */
    public QueryRequestBuilder<T> geomOpt(AdvEnumsGeomOpt advEnumsGeomOpt) {
        this.advEnumsGeomOpt = advEnumsGeomOpt;
        return this;
    }

    /** 设置空间操作规则 */
    @Deprecated
    public QueryRequestBuilder<T> advEnumsGeomOpt(AdvEnumsGeomOpt advEnumsGeomOpt) {
        this.advEnumsGeomOpt = advEnumsGeomOpt;
        return this;
    }

    /** 设置是否返回字段元数据 */
    public QueryRequestBuilder<T> hasFieldsInfo(boolean hasFieldsInfo) {
        this.hasFieldsInfo = hasFieldsInfo;
        return this;
    }

    /** 设置key的转换策略 */
    public QueryRequestBuilder<T> keyTran(AdvEnumsKeyTran advEnumsKeyTran) {
        this.advEnumsKeyTran = advEnumsKeyTran;
        return this;
    }

    /** 设置key的转换策略 */
    @Deprecated
    public QueryRequestBuilder<T> advEnumsKeyTran(AdvEnumsKeyTran advEnumsKeyTran) {
        this.advEnumsKeyTran = advEnumsKeyTran;
        return this;
    }

    /** 设置字段映射 */
    public QueryRequestBuilder<T> fieldMapping(Map<String, String> fieldMapping) {
        this.fieldMapping = fieldMapping;
        return this;
    }

    /** 添加字段映射 */
    public QueryRequestBuilder<T> addFieldMapping(String dbField, String returnField) {
        if (this.fieldMapping == null) {
            this.fieldMapping = new HashMap<>();
        }
        this.fieldMapping.put(dbField, returnField);
        return this;
    }

    /**
     * 设置去重查询
     *
     * @param distinct true=去重查询，false=不去重
     * @return 当前Builder实例
     */
    public QueryRequestBuilder<T> distinct(boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    /** 启用去重查询 */
    public QueryRequestBuilder<T> distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * 设置GROUP BY字段
     *
     * @param groupByFields 分组字段列表
     * @return 当前Builder实例
     */
    public QueryRequestBuilder<T> groupBy(String... groupByFields) {
        if (this.groupByFields == null) {
            this.groupByFields = new ArrayList<>();
        }
        this.groupByFields.addAll(ListUtil.toList(groupByFields));
        return this;
    }

    /**
     * 设置GROUP BY字段（Lambda表达式版本，需要配合实体类使用）
     *
     * @param groupByFunctions 分组字段的Lambda表达式
     * @return 当前Builder实例
     */
    @SafeVarargs
    public final QueryRequestBuilder<T> groupBy(SFunction<T, ?>... groupByFunctions) {
        if (this.groupByFields == null) {
            this.groupByFields = new ArrayList<>();
        }
        for (SFunction<T, ?> function : groupByFunctions) {
            String fieldName = LambdaUtils.getColumnName(function);
            if (isToUnderlineCase && fieldName != null) {
                fieldName = StrUtil.toUnderlineCase(fieldName);
            }
            this.groupByFields.add(fieldName);
        }
        return this;
    }

    /**
     * 添加GROUP BY字段
     *
     * @param groupByField 分组字段
     * @return 当前Builder实例
     */
    public QueryRequestBuilder<T> addGroupBy(String groupByField) {
        if (this.groupByFields == null) {
            this.groupByFields = new ArrayList<>();
        }
        this.groupByFields.add(groupByField);
        return this;
    }

    /**
     * 设置HAVING条件（通过Consumer方式）
     *
     * @param consumer HAVING条件构建器
     * @return 当前Builder实例
     */
    public QueryRequestBuilder<T> having(Consumer<GirAdvWhereFilter> consumer) {
        this.havingFilter = GirAdvWhereFilter.of();
        consumer.accept(this.havingFilter);
        return this;
    }

    /**
     * 设置HAVING条件（直接传入Filter）
     *
     * @param havingFilter HAVING过滤器
     * @return 当前Builder实例
     */
    public QueryRequestBuilder<T> having(GirAdvWhereFilter havingFilter) {
        this.havingFilter = havingFilter;
        return this;
    }

    /** 构建GirAdvQueryRequest对象 */
    public GirAdvQueryRequest build() {
        // 校验：两种模式至少选一种
        boolean hasObjectMode = tableOrSqlView != null || entityClass != null;
        boolean hasCustomSqlMode = customSql != null && !customSql.trim().isEmpty();

        if (!hasObjectMode && !hasCustomSqlMode) {
            throw new IllegalArgumentException(
                    "Either (table + fields + where) or customSql must be provided");
        }

        // 分页参数校验
        if (pageNum != null && pageSize != null && pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be greater than 0");
        }

        return new GirAdvQueryRequest(this);
    }

    public List<String> getFieldNames() {
        return columnNames;
    }

    public List<String> getExprFieldNames() {
        return exprColumnNames;
    }
}
