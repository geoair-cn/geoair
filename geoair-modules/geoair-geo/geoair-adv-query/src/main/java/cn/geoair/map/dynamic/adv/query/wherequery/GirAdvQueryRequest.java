package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.base.util.GutilObject;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsKeyTran;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.enums.AdvNullHandling;
import cn.geoair.map.dynamic.adv.query.wherequery.queryr.QueryRequestBuilder;
import cn.hutool.core.collection.ListUtil;
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
     * WHERE条件参数映射（必填，可为空Map）
     * -- GETTER --
     * 获取WHERE条件参数映射
     *
     * @return 条件参数映射
     */
    private GirAdvWhereFilter whereOption = GirAdvWhereFilter.of();

    /**
     * NULL值处理策略（可选，默认INCLUDE）
     * -- GETTER --
     * 获取NULL值处理策略
     *
     * @return NULL处理策略枚举
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
     * -- GETTER --
     * 获取页码
     *
     * @return 页码
     */
    private final Integer pageNum;

    /**
     * 每页条数（可选）
     * -- GETTER --
     * 获取每页条数
     *
     * @return 每页条数
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
        this.tableOrSqlView = builder.getTableOrSqlView();
        this.sqlViewTableNameAlias = builder.getSqlViewTableNameAlias();
        this.fieldNames = builder.getFieldNames() != null ? new ArrayList<>(builder.getFieldNames()) : null;
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

    }


//    /**
//     * 私有构造器，使用Builder构建
//     *
//     * @param builder 构建器实例
//     */
//    public GirAdvQueryRequest(QueryRequestBuilder builder) {
//        // 模式一参数
//        this.tableOrSqlView = builder.tableOrSqlView;
//        this.sqlViewTableNameAlias = builder.sqlViewTableNameAlias;
//        if (GutilObject.isEmpty(builder.fieldNames)) {
//            this.fieldNames = ListUtil.of("*");
//        } else {
//            this.fieldNames = builder.fieldNames;
//        }
//        if (GutilObject.isEmpty(builder.whereOption)) {
//            this.whereOption = GirAdvWhereFilter.of();
//        } else {
//            this.whereOption = builder.whereOption;
//        }
//        this.nullHandling = builder.nullHandling;
//        this.orders = Collections.unmodifiableList(new ArrayList<>(builder.orders));
//        this.pageNum = builder.pageNum;
//        this.pageSize = builder.pageSize;
//        this.pageNumStartZero = builder.pageNumStartZero;
//
//        // 模式二参数
//        this.customSql = builder.customSql;
//        this.advEnumsGeomOpt = builder.advEnumsGeomOpt;
//        this.hasFieldsInfo = builder.hasFieldsInfo;
//        this.advEnumsKeyTran = builder.advEnumsKeyTran;
//
//    }


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
     *
     * @return true=有排序条件，false=无排序条件
     */
    public boolean hasOrders() {
        return orders != null && !orders.isEmpty();
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
            if (sb.length() > 0) {
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
     * 创建Builder实例
     *
     * @return Builder实例
     */
    public static <T> QueryRequestBuilder<T> builder() {
        return new QueryRequestBuilder<>(null);
    }

    /**
     * 创建Builder实例
     *
     * @return Builder实例
     */
    public static <T> QueryRequestBuilder<T> builder(Class<T> entityClass) {
        return new QueryRequestBuilder<>(entityClass);
    }
    /**
     * 创建Builder实例
     *
     * @return Builder实例
     */
    public static <T> QueryRequestBuilder<T> builder(Class<T> entityClass, boolean isToUnderlineCase) {
        return new QueryRequestBuilder<>(entityClass, isToUnderlineCase);
    }


}
