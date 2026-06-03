package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsKeyTran;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.enums.AdvNullHandling;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.wherequery.queryr.QueryRequestBuilder;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.*;

/**
 * 查询参数构建器
 * <p>支持两种查询模式：
 * <ul>
 *   <li>模式一：对象组装SQL - 通过表名、字段名、条件参数自动构建SQL</li>
 *   <li>模式二：自定义SQL - 直接传入SQL语句，优先级更高</li>
 * </ul>
 * </p>
 *
 * @author zhangjun
 */
@Getter
public class GirAdvQueryRequest {

    // ==================== 模式一：对象组装SQL ====================

    /**
     * 表名或一个sql查询语句。但是必须是完整的sql
     * -- GETTER --
     * 获取表名或视图名
     * table ：user
     * sqlView：select * from user
     * 不支持的写法 ( select * from user )  as rere
     *
     * @return 表名或视图名
     */
    private final String tableOrSqlView;

    /**
     * 如果tableOrSqlView是一个 SqlView，这里你可以指定对他的别名，如果你没有写别名，那么我就会随机生成别名
     */
    private final String sqlViewTableNameAlias;

    /**
     * 查询字段名列表（必填）
     * -- GETTER --
     * 获取查询字段名列表
     *
     * @return 字段名列表
     */
    private final List<String> fieldNames;

    /**
     * 表达式字段名称
     * 类似于这样的函数表达式
     * "COUNT", "SUM", "AVG", "MAX", "MIN", "CONCAT", "SUBSTR",
     * "LENGTH", "NOW", "DATE", "YEAR", "MONTH", "DAY", "TRIM"
     */
    private List<String> exprColumnNames;

    /**
     * WHERE条件参数映射（必填，可为空Map）
     */
    private GirAdvWhereFilter whereOption = GirAdvWhereFilter.of();

    /**
     * NULL值处理策略（可选，默认INCLUDE）
     */
    private final AdvNullHandling nullHandling;

    /**
     * 排序参数列表（可选）
     * -- GETTER --
     * 获取排序参数列表
     *
     * @return 排序参数列表
     */
    private final List<OrderApo> orders;

    /**
     * 页码（可选）
     */
    private final Integer pageNum;

    /**
     * 每页条数（可选）
     */
    private final Integer pageSize;

    /**
     * 页码起始规则（可选，默认false）
     * <p>true=页码从0开始，false=页码从1开始</p>
     * -- GETTER --
     * 获取页码起始规则
     *
     * @return true=从0开始，false=从1开始
     */
    private final Boolean pageNumStartZero;

    /**
     * 空间操作规则（可选）
     * <p>用于处理空间字段的转换，如转换为空字符串等</p>
     * -- GETTER --
     * 获取空间操作规则
     *
     * @return 空间操作规则枚举
     */
    private final AdvEnumsGeomOpt advEnumsGeomOpt;

    /**
     * 是否返回字段元数据（可选，默认false）
     * <p>true=返回字段元数据信息，false=仅返回数据</p>
     * -- GETTER --
     * 获取是否返回字段元数据
     *
     * @return true=返回元数据，false=仅返回数据
     */
    private final Boolean hasFieldsInfo;

    /**
     * key的转换策略
     */
    private AdvEnumsKeyTran advEnumsKeyTran = AdvEnumsKeyTran.不转换;

    // ==================== 新增：分组相关 ====================

    /**
     * 是否去重查询
     */
    private final Boolean distinct;

    /**
     * GROUP BY字段列表
     */
    private final List<String> groupByFields;

    /**
     * HAVING条件过滤器
     */
    private final GirAdvWhereFilter havingFilter;

    // ==================== 模式二：直接传SQL ====================

    /**
     * 自定义SQL语句（可选，优先级高于对象模式）
     * <p>当此字段不为空时，将忽略tableOrViewName、fieldNames、whereOption等参数</p>
     * -- GETTER --
     * 获取自定义SQL语句
     *
     * @return 自定义SQL
     */
    private final String customSql;

    /**
     * 从Builder构造
     */
    public <T> GirAdvQueryRequest(QueryRequestBuilder<T> builder) {
        if (GutilObject.isEmpty(builder.getTableOrSqlView()) && GutilObject.isNotEmpty(builder.getEntityClass())) {
            String tableNameByAnnotation = GirAdvSqlUtils.getTableNameByAnnotation(builder.getEntityClass());
            if (tableNameByAnnotation != null) {
                tableOrSqlView = tableNameByAnnotation;
            } else {
                tableOrSqlView = StrUtil.lowerFirst(builder.getEntityClass().getSimpleName());
            }
        } else {
            this.tableOrSqlView = builder.getTableOrSqlView();
        }
        if (GutilObject.isEmpty(tableOrSqlView) && GutilObject.isEmpty(builder.getCustomSql())) {
            throw new IllegalArgumentException("tableOrSqlView is empty");
        }
        this.sqlViewTableNameAlias = builder.getSqlViewTableNameAlias();
        this.fieldNames = GutilObject.isNotEmpty(builder.getFieldNames()) ? new ArrayList<>(builder.getFieldNames()) : ListUtil.of("*");
        this.exprColumnNames = GutilObject.isNotEmpty(builder.getExprFieldNames()) ? new ArrayList<>(builder.getExprFieldNames()) : ListUtil.empty();
        this.whereOption = builder.getWhereOption();
        this.nullHandling = builder.getNullHandling();
        this.orders = builder.getOrders() != null ? new ArrayList<>(builder.getOrders()) : new ArrayList<>();
        this.pageNum = builder.getPageNum();
        this.pageSize = builder.getPageSize();
        this.pageNumStartZero = builder.getPageNumStartZero();

        this.customSql = builder.getCustomSql();
        this.advEnumsGeomOpt = builder.getAdvEnumsGeomOpt();
        this.hasFieldsInfo = builder.getHasFieldsInfo();
        this.advEnumsKeyTran = builder.getAdvEnumsKeyTran();

        // 新增字段赋值
        this.distinct = builder.getDistinct();
        this.groupByFields = builder.getGroupByFields() != null ? new ArrayList<>(builder.getGroupByFields()) : new ArrayList<>();
        this.havingFilter = builder.getHavingFilter();
    }

    /**
     * 判断是否为自定义SQL模式
     *
     * @return true=使用自定义SQL，false=使用对象组装模式
     */
    public boolean isCustomSqlMode() {
        return customSql != null && !customSql.trim().isEmpty();
    }

    /**
     * 判断是否需要进行分页
     *
     * @return true=需要分页，false=不分页
     */
    public boolean hasPagination() {
        return pageNum != null && pageSize != null && pageSize > 0;
    }

    /**
     * 判断是否有排序条件
     */
    public boolean hasOrders() {
        return orders != null && !orders.isEmpty();
    }

    /**
     * 判断是否需要去重
     */
    public boolean hasDistinct() {
        return distinct != null && distinct;
    }

    /**
     * 判断是否有GROUP BY
     */
    public boolean hasGroupBy() {
        return groupByFields != null && !groupByFields.isEmpty();
    }

    /**
     * 判断是否有HAVING条件
     */
    public boolean hasHaving() {
        return havingFilter != null;
    }

    /**
     * 计算分页偏移量
     * <p>根据页码、每页条数和页码起始规则计算LIMIT子句的偏移量</p>
     *
     * @return 偏移量
     */
    public int getOffset() {
        if (!hasPagination()) {
            return 0;
        }
        int startPage = (pageNumStartZero != null && pageNumStartZero) ? 0 : 1;
        return (pageNum - startPage) * pageSize;
    }

    /**
     * 获取实际页码（转换为从1开始的页码）
     *
     * @return 从1开始的实际页码
     */
    public int getActualPageNum() {
        if (pageNum == null) {
            return 1;
        }
        if (pageNumStartZero != null && pageNumStartZero) {
            return pageNum + 1;
        }
        return pageNum;
    }

    /**
     * 构建ORDER BY子句
     * <p>根据排序参数列表生成SQL的ORDER BY部分</p>
     *
     * @return ORDER BY子句字符串，如果没有排序条件则返回空字符串
     */
    public String buildOrderByClause() {
        if (!hasOrders()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (OrderApo order : orders) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            if (order.isFunction()) {
                // 函数排序示例：CAST(gtc_id AS numeric) ASC
                sb.append(order.getFunction()).append(" ").append(order.getAdvEnumsOrder().getValue());
            } else {
                // 字段排序示例：field_name ASC
                sb.append(order.getFieldName()).append(" ").append(order.getAdvEnumsOrder().getValue());
            }
        }
        return sb.toString();
    }

    /**
     * 构建GROUP BY子句
     */
    public String buildGroupByClause() {
        if (!hasGroupBy()) {
            return "";
        }
        return String.join(", ", groupByFields);
    }

    /**
     * 获取DISTINCT前缀
     */
    public String getDistinctPrefix() {
        return hasDistinct() ? "DISTINCT " : "";
    }

    /**
     * 创建Builder实例
     */
    public static <T> QueryRequestBuilder<T> builder() {
        return new QueryRequestBuilder<>(null);
    }

    /**
     * 创建Builder实例
     */
    public static <T> QueryRequestBuilder<T> builder(Class<T> entityClass) {
        return new QueryRequestBuilder<>(entityClass);
    }

    /**
     * 创建Builder实例
     */
    public static <T> QueryRequestBuilder<T> builder(Class<T> entityClass, boolean isToUnderlineCase) {
        return new QueryRequestBuilder<>(entityClass, isToUnderlineCase);
    }
}
