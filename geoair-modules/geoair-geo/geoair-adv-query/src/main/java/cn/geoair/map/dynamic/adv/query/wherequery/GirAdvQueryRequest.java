package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsKeyTran;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.enums.AdvNullHandling;
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
     * 表名或视图名（必填）
     * -- GETTER --
     * 获取表名或视图名
     *
     * @return 表名或视图名
     */
    private final String tableOrViewName;

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
     * 私有构造器，使用Builder构建
     *
     * @param builder 构建器实例
     */
    private GirAdvQueryRequest(Builder builder) {
        // 模式一参数
        this.tableOrViewName = builder.tableOrViewName;
        this.fieldNames = builder.fieldNames;
        this.whereOption = builder.whereOption;
        this.nullHandling = builder.nullHandling;
        this.orders = Collections.unmodifiableList(new ArrayList<>(builder.orders));
        this.pageNum = builder.pageNum;
        this.pageSize = builder.pageSize;
        this.pageNumStartZero = builder.pageNumStartZero;

        // 模式二参数
        this.customSql = builder.customSql;
        this.advEnumsGeomOpt = builder.advEnumsGeomOpt;
        this.hasFieldsInfo = builder.hasFieldsInfo;
        this.advEnumsKeyTran = builder.advEnumsKeyTran;

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
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 查询参数构建器类
     * <p>使用链式调用方式构建SelectQueryParam对象</p>
     */
    public static class Builder {

        // ==================== 模式一参数 ====================

        /**
         * 表名或视图名
         */
        private String tableOrViewName;

        /**
         * 查询字段名列表
         */
        private List<String> fieldNames;

        /**
         * WHERE条件参数映射
         */
        private GirAdvWhereFilter whereOption;

        /**
         * NULL值处理策略（默认INCLUDE）
         */
        private AdvNullHandling nullHandling = AdvNullHandling.INCLUDE;

        /**
         * 排序参数列表
         */
        private final List<OrderApo> orders = new ArrayList<>();

        /**
         * 页码
         */
        private Integer pageNum;

        /**
         * 每页条数
         */
        private Integer pageSize;

        /**
         * 页码起始规则（默认false，从1开始）
         */
        private Boolean pageNumStartZero = false;

        // ==================== 模式二参数 ====================

        /**
         * 自定义SQL语句
         */
        private String customSql;

        /**
         * 空间操作规则
         */
        private AdvEnumsGeomOpt advEnumsGeomOpt;

        /**
         * 是否返回字段元数据（默认false）
         */
        private Boolean hasFieldsInfo = false;


        /**
         * key的转换策略
         */
        private AdvEnumsKeyTran advEnumsKeyTran = AdvEnumsKeyTran.不转换;


        // ==================== 模式一：对象组装SQL方法 ====================

        /**
         * 设置表名或视图名
         *
         * @param tableOrViewName 表名或视图名
         * @return Builder实例
         */
        public Builder table(String tableOrViewName) {
            this.tableOrViewName = tableOrViewName;
            return this;
        }

        /**
         * 设置查询字段名列表
         *
         * @param fieldNames 字段名列表
         * @return Builder实例
         */
        public Builder fields(List<String> fieldNames) {
            this.fieldNames = fieldNames;
            return this;
        }

        /**
         * 设置查询字段名（可变参数）
         *
         * @param fieldNames 字段名数组
         * @return Builder实例
         */
        public Builder fields(String... fieldNames) {
            this.fieldNames = Arrays.asList(fieldNames);
            return this;
        }

        /**
         * 设置WHERE条件参数映射
         *
         * @param whereOption 条件参数映射
         * @return Builder实例
         */
        public Builder where(GirAdvWhereFilter whereOption) {
            this.whereOption = whereOption;
            return this;
        }

        /**
         * 设置忽略NULL值
         * <p>当WHERE条件中的值为NULL时，自动忽略该条件</p>
         *
         * @return Builder实例
         */
        public Builder ignoreNull() {
            this.nullHandling = AdvNullHandling.IGNORE;
            return this;
        }

        /**
         * 设置NULL值处理策略
         *
         * @param nullHandling NULL处理策略枚举
         * @return Builder实例
         */
        public Builder nullHandling(AdvNullHandling nullHandling) {
            this.nullHandling = nullHandling;
            return this;
        }

        // ==================== 排序相关方法 ====================

        /**
         * 添加字段排序（升序）
         *
         * @param fieldName 字段名
         * @return Builder实例
         */
        public Builder orderByAsc(String fieldName) {
            this.orders.add(OrderApo.ofASCFieldName(fieldName));
            return this;
        }

        /**
         * 添加字段排序（降序）
         *
         * @param fieldName 字段名
         * @return Builder实例
         */
        public Builder orderByDesc(String fieldName) {
            this.orders.add(OrderApo.ofDescFieldName(fieldName));
            return this;
        }

        /**
         * 添加字段排序（自定义方向）
         *
         * @param fieldName 字段名
         * @param direction 排序方向枚举
         * @return Builder实例
         */
        public Builder orderByField(String fieldName, AdvEnumsOrder direction) {
            this.orders.add(OrderApo.ofFieldName(fieldName, direction));
            return this;
        }

        /**
         * 添加函数排序（升序）
         * <p>示例：orderByAscFunction("CAST(gtc_id AS numeric)")</p>
         *
         * @param function 排序函数表达式
         * @return Builder实例
         */
        public Builder orderByAscFunction(String function) {
            this.orders.add(OrderApo.ofASCFunction(function));
            return this;
        }

        /**
         * 添加函数排序（降序）
         * <p>示例：orderByDescFunction("LENGTH(name)")</p>
         *
         * @param function 排序函数表达式
         * @return Builder实例
         */
        public Builder orderByDescFunction(String function) {
            this.orders.add(OrderApo.ofDescFunction(function));
            return this;
        }

        /**
         * 添加函数排序（自定义方向）
         *
         * @param function  排序函数表达式
         * @param direction 排序方向枚举
         * @return Builder实例
         */
        public Builder orderByFunction(String function, AdvEnumsOrder direction) {
            this.orders.add(OrderApo.ofFunction(function, direction));
            return this;
        }

        /**
         * 直接添加OrderApo排序对象
         *
         * @param order OrderApo排序对象
         * @return Builder实例
         */
        public Builder order(OrderApo order) {
            this.orders.add(order);
            return this;
        }

        /**
         * 批量添加排序条件
         * <p>会清空之前设置的排序条件</p>
         *
         * @param orders 排序参数列表
         * @return Builder实例
         */
        public Builder orders(List<OrderApo> orders) {
            this.orders.clear();
            this.orders.addAll(orders);
            return this;
        }

        // ==================== 分页相关方法 ====================

        /**
         * 设置分页参数（页码从1开始）
         *
         * @param pageNum  页码（从1开始）
         * @param pageSize 每页条数
         * @return Builder实例
         */
        public Builder page(int pageNum, int pageSize) {
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.pageNumStartZero = false;
            return this;
        }

        /**
         * 设置分页参数（自定义起始页码）
         *
         * @param pageNum          页码
         * @param pageSize         每页条数
         * @param pageNumStartZero 页码起始规则（true=从0开始，false=从1开始）
         * @return Builder实例
         */
        public Builder page(int pageNum, int pageSize, boolean pageNumStartZero) {
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.pageNumStartZero = pageNumStartZero;
            return this;
        }

        // ==================== 模式二：自定义SQL方法 ====================

        /**
         * 设置自定义SQL语句
         * <p>当设置此参数后，将忽略tableOrViewName、fieldNames、whereOption等对象组装参数</p>
         *
         * @param customSql 自定义SQL语句
         * @return Builder实例
         */
        public Builder customSql(String customSql) {
            this.customSql = customSql;
            return this;
        }

        /**
         * 设置空间操作规则
         * <p>用于处理空间字段的转换逻辑</p>
         *
         * @param advEnumsGeomOpt 空间操作规则枚举
         * @return Builder实例
         */
        public Builder advEnumsGeomOpt(AdvEnumsGeomOpt advEnumsGeomOpt) {
            this.advEnumsGeomOpt = advEnumsGeomOpt;
            return this;
        }

        /**
         * 设置是否返回字段元数据
         *
         * @param hasFieldsInfo true=返回字段元数据，false=仅返回数据
         * @return Builder实例
         */
        public Builder hasFieldsInfo(boolean hasFieldsInfo) {
            this.hasFieldsInfo = hasFieldsInfo;
            return this;
        }

        /**
         * 设置key的转换策略
         *
         * @param advEnumsKeyTran
         * @return Builder实例
         */
        public Builder advEnumsKeyTran(AdvEnumsKeyTran advEnumsKeyTran) {
            this.advEnumsKeyTran = advEnumsKeyTran;
            return this;
        }




        /**
         * 完整的高级分页配置（自定义SQL模式）
         * <p>一次性设置自定义SQL模式下的所有分页参数</p>
         *
         * @param customSql        自定义SQL语句
         * @param pageNum          页码
         * @param pageSize         每页条数
         * @param pageNumStartZero 页码起始规则（true=从0开始，false=从1开始）
         * @param advEnumsGeomOpt  空间操作规则
         * @param hasFieldsInfo    是否返回字段元数据
         * @param orders           排序参数列表
         * @return Builder实例
         */
        public Builder advancedPage(String customSql,
                                    int pageNum,
                                    int pageSize,
                                    boolean pageNumStartZero,
                                    AdvEnumsGeomOpt advEnumsGeomOpt,
                                    boolean hasFieldsInfo,
                                    List<OrderApo> orders) {
            this.customSql = customSql;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
            this.pageNumStartZero = pageNumStartZero;
            this.advEnumsGeomOpt = advEnumsGeomOpt;
            this.hasFieldsInfo = hasFieldsInfo;
            if (orders != null) {
                this.orders.clear();
                this.orders.addAll(orders);
            }
            return this;
        }

        /**
         * 构建SelectQueryParam对象
         * <p>执行参数校验并返回不可变的查询参数对象</p>
         *
         * @return SelectQueryParam实例
         * @throws IllegalArgumentException 当参数校验失败时抛出
         */
        public GirAdvQueryRequest build() {
            // 校验：两种模式至少选一种
            boolean hasObjectMode = tableOrViewName != null && fieldNames != null && whereOption != null;
            boolean hasCustomSqlMode = customSql != null && !customSql.trim().isEmpty();

            if (!hasObjectMode && !hasCustomSqlMode) {
                throw new IllegalArgumentException(
                        "Either (table + fields + where) or customSql must be provided"
                );
            }

            // 对象模式校验
            if (hasObjectMode) {
                if (tableOrViewName.trim().isEmpty()) {
                    throw new IllegalArgumentException("tableOrViewName cannot be empty");
                }
                if (fieldNames.isEmpty()) {
                    throw new IllegalArgumentException("fieldNames cannot be empty");
                }
            }

            // 分页参数校验
            if (pageNum != null && pageSize != null && pageSize <= 0) {
                throw new IllegalArgumentException("pageSize must be greater than 0");
            }

            return new GirAdvQueryRequest(this);
        }
    }

    /**
     * 示例代码
     */
    public static void main(String[] args) {
        // 1. 字段排序示例
        GirAdvQueryRequest query1 = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "age")
                .where(GirAdvWhereFilter.of())
                .ignoreNull()
                .orderByAsc("name")           // 升序
                .orderByDesc("age")           // 降序
                .orderByField("status", AdvEnumsOrder.升序)
                .page(1, 20)
                .build();

        // 2. 函数排序示例
        GirAdvQueryRequest query2 = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "gtc_id")
                .where(GirAdvWhereFilter.of())
                .orderByAscFunction("CAST(gtc_id AS numeric)")     // 函数升序
                .orderByDescFunction("LENGTH(name)")               // 函数降序
                .orderByFunction("YEAR(create_time)", AdvEnumsOrder.降序)
                .build();

        // 3. 混合排序示例
        GirAdvQueryRequest query3 = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name")
                .where(GirAdvWhereFilter.of())
                .order(OrderApo.ofASCFieldName("name"))              // 字段升序
                .order(OrderApo.ofDescFunction("CAST(gtc_id AS numeric)")) // 函数降序
                .build();

        // 4. 批量添加排序示例
        List<OrderApo> orderList = Arrays.asList(
                OrderApo.ofDescFieldName("create_time"),
                OrderApo.ofASCFieldName("id")
        );
        GirAdvQueryRequest query4 = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name")
                .where(GirAdvWhereFilter.of())
                .orders(orderList)
                .build();

        // 5. 构建ORDER BY子句示例
        String orderByClause = query4.buildOrderByClause();
        // 输出：create_time DESC, id ASC
    }
}
