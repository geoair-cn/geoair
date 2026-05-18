//package cn.geoair.map.dynamic.adv.query.wherequery.queryr;
//
//import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
//import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
//import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsKeyTran;
//import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
//import cn.geoair.map.dynamic.adv.query.enums.AdvNullHandling;
//import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
//import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
///**
// * @author ：张俊
// * @date ：Created in 2026/5/18 19:27
// * @description： QueryRequest 的bulider对象
// */
//public class QueryRequestBuilderBack {
//
//    // ==================== 模式一参数 ====================
//
//    /**
//     * 表名或一个一个完整带结果的SQL
//     * table ：user
//     * sqlView：select * from user
//     */
//    public String tableOrSqlView;
//
//    /**
//     * 如果tableOrSqlView是一个 SqlView，这里你可以指定对他的别名，如果你没有写别名，那么我就会随机生成别名
//     */
//    public String sqlViewTableNameAlias;
//
//    /**
//     * 查询字段名列表
//     */
//    public List<String> fieldNames;
//
//    /**
//     * WHERE条件参数映射
//     */
//    public GirAdvWhereFilter whereOption;
//
//    /**
//     * NULL值处理策略（默认INCLUDE）
//     */
//    public AdvNullHandling nullHandling = AdvNullHandling.INCLUDE;
//
//    /**
//     * 排序参数列表
//     */
//    public final List<OrderApo> orders = new ArrayList<>();
//
//    /**
//     * 页码
//     */
//    public Integer pageNum = 1;
//
//    /**
//     * 每页条数
//     */
//    public Integer pageSize = 25;
//
//    /**
//     * 页码起始规则（默认false，从1开始）
//     */
//    public Boolean pageNumStartZero = false;
//
//    // ==================== 模式二参数 ====================
//
//    /**
//     * 自定义SQL语句
//     */
//    public String customSql;
//
//    /**
//     * 空间操作规则
//     */
//    public AdvEnumsGeomOpt advEnumsGeomOpt;
//
//    /**
//     * 是否返回字段元数据（默认false）
//     */
//    public Boolean hasFieldsInfo = false;
//
//
//    /**
//     * key的转换策略
//     */
//    public AdvEnumsKeyTran advEnumsKeyTran = AdvEnumsKeyTran.不转换;
//
//
//    // ==================== 模式一：对象组装SQL方法 ====================
//
//    /**
//     * 表名或一个一个完整带结果的SQL
//     * table ：user
//     * sqlView：select * from user
//     * 不支持的写法 ( select * from user )  as alias
//     *
//     * @param tableOrSqlView 表名或
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack table(String tableOrSqlView) {
//        this.tableOrSqlView = tableOrSqlView;
//        return this;
//    }
//
//    /**
//     * 表名或一个一个完整带结果的SQL
//     * table ：user
//     * sqlView：select * from user
//     * 不支持的写法 ( select * from user )  as alias
//     *
//     * @param tableOrSqlView        表名或sql
//     * @param sqlViewTableNameAlias 如果tableOrSqlView是一个 SqlView，这里你可以指定对他的别名，如果你没有写别名，那么我就会随机生成别名
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack table(String tableOrSqlView, String sqlViewTableNameAlias) {
//        this.tableOrSqlView = tableOrSqlView;
//        this.sqlViewTableNameAlias = sqlViewTableNameAlias;
//        return this;
//    }
//
//    /**
//     * 设置查询字段名列表
//     *
//     * @param fieldNames 字段名列表
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack fields(List<String> fieldNames) {
//        this.fieldNames = fieldNames;
//        return this;
//    }
//
//    /**
//     * 设置查询字段名（可变参数）
//     *
//     * @param fieldNames 字段名数组
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack fields(String... fieldNames) {
//        this.fieldNames = Arrays.asList(fieldNames);
//        return this;
//    }
//
//    /**
//     * 设置WHERE条件参数映射
//     *
//     * @param whereOption 条件参数映射
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack where(GirAdvWhereFilter whereOption) {
//        this.whereOption = whereOption;
//        return this;
//    }
//
//    /**
//     * 设置忽略NULL值
//     * <p>当WHERE条件中的值为NULL时，自动忽略该条件</p>
//     *
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack ignoreNull() {
//        this.nullHandling = AdvNullHandling.IGNORE;
//        return this;
//    }
//
//    /**
//     * 设置NULL值处理策略
//     *
//     * @param nullHandling NULL处理策略枚举
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack nullHandling(AdvNullHandling nullHandling) {
//        this.nullHandling = nullHandling;
//        return this;
//    }
//
//    // ==================== 排序相关方法 ====================
//
//    /**
//     * 添加字段排序（升序）
//     *
//     * @param fieldName 字段名
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack orderByAsc(String fieldName) {
//        this.orders.add(OrderApo.ofASCFieldName(fieldName));
//        return this;
//    }
//
//    /**
//     * 添加字段排序（降序）
//     *
//     * @param fieldName 字段名
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack orderByDesc(String fieldName) {
//        this.orders.add(OrderApo.ofDescFieldName(fieldName));
//        return this;
//    }
//
//    /**
//     * 添加字段排序（自定义方向）
//     *
//     * @param fieldName 字段名
//     * @param direction 排序方向枚举
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack orderByField(String fieldName, AdvEnumsOrder direction) {
//        this.orders.add(OrderApo.ofFieldName(fieldName, direction));
//        return this;
//    }
//
//    /**
//     * 添加函数排序（升序）
//     * <p>示例：orderByAscFunction("CAST(gtc_id AS numeric)")</p>
//     *
//     * @param function 排序函数表达式
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack orderByAscFunction(String function) {
//        this.orders.add(OrderApo.ofASCFunction(function));
//        return this;
//    }
//
//    /**
//     * 添加函数排序（降序）
//     * <p>示例：orderByDescFunction("LENGTH(name)")</p>
//     *
//     * @param function 排序函数表达式
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack orderByDescFunction(String function) {
//        this.orders.add(OrderApo.ofDescFunction(function));
//        return this;
//    }
//
//    /**
//     * 添加函数排序（自定义方向）
//     *
//     * @param function  排序函数表达式
//     * @param direction 排序方向枚举
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack orderByFunction(String function, AdvEnumsOrder direction) {
//        this.orders.add(OrderApo.ofFunction(function, direction));
//        return this;
//    }
//
//    /**
//     * 直接添加OrderApo排序对象
//     *
//     * @param order OrderApo排序对象
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack order(OrderApo order) {
//        this.orders.add(order);
//        return this;
//    }
//
//    /**
//     * 批量添加排序条件
//     * <p>会清空之前设置的排序条件</p>
//     *
//     * @param orders 排序参数列表
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack orders(List<OrderApo> orders) {
//        this.orders.clear();
//        this.orders.addAll(orders);
//        return this;
//    }
//
//    // ==================== 分页相关方法 ====================
//
//    /**
//     * 设置分页参数（页码从1开始）
//     *
//     * @param pageNum  页码（从1开始）
//     * @param pageSize 每页条数
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack page(int pageNum, int pageSize) {
//        this.pageNum = pageNum;
//        this.pageSize = pageSize;
//        this.pageNumStartZero = false;
//        return this;
//    }
//
//    /**
//     * 设置分页参数（自定义起始页码）
//     *
//     * @param pageNum          页码
//     * @param pageSize         每页条数
//     * @param pageNumStartZero 页码起始规则（true=从0开始，false=从1开始）
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack page(int pageNum, int pageSize, boolean pageNumStartZero) {
//        this.pageNum = pageNum;
//        this.pageSize = pageSize;
//        this.pageNumStartZero = pageNumStartZero;
//        return this;
//    }
//
//    // ==================== 模式二：自定义SQL方法 ====================
//
//    /**
//     * 设置自定义SQL语句
//     * <p>当设置此参数后，将忽略tableOrViewName、fieldNames、whereOption等对象组装参数</p>
//     *
//     * @param customSql 自定义SQL语句
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack customSql(String customSql) {
//        this.customSql = customSql;
//        return this;
//    }
//
//    /**
//     * 设置空间操作规则
//     * <p>用于处理空间字段的转换逻辑</p>
//     *
//     * @param advEnumsGeomOpt 空间操作规则枚举
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack advEnumsGeomOpt(AdvEnumsGeomOpt advEnumsGeomOpt) {
//        this.advEnumsGeomOpt = advEnumsGeomOpt;
//        return this;
//    }
//
//    /**
//     * 设置是否返回字段元数据
//     *
//     * @param hasFieldsInfo true=返回字段元数据，false=仅返回数据
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack hasFieldsInfo(boolean hasFieldsInfo) {
//        this.hasFieldsInfo = hasFieldsInfo;
//        return this;
//    }
//
//    /**
//     * 设置key的转换策略
//     *
//     * @param advEnumsKeyTran
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack advEnumsKeyTran(AdvEnumsKeyTran advEnumsKeyTran) {
//        this.advEnumsKeyTran = advEnumsKeyTran;
//        return this;
//    }
//
//
//    /**
//     * 完整的高级分页配置（自定义SQL模式）
//     * <p>一次性设置自定义SQL模式下的所有分页参数</p>
//     *
//     * @param customSql        自定义SQL语句
//     * @param pageNum          页码
//     * @param pageSize         每页条数
//     * @param pageNumStartZero 页码起始规则（true=从0开始，false=从1开始）
//     * @param advEnumsGeomOpt  空间操作规则
//     * @param hasFieldsInfo    是否返回字段元数据
//     * @param orders           排序参数列表
//     * @return Builder实例
//     */
//    public QueryRequestBuilderBack advancedPage(String customSql,
//                                                int pageNum,
//                                                int pageSize,
//                                                boolean pageNumStartZero,
//                                                AdvEnumsGeomOpt advEnumsGeomOpt,
//                                                boolean hasFieldsInfo,
//                                                List<OrderApo> orders) {
//        this.customSql = customSql;
//        this.pageNum = pageNum;
//        this.pageSize = pageSize;
//        this.pageNumStartZero = pageNumStartZero;
//        this.advEnumsGeomOpt = advEnumsGeomOpt;
//        this.hasFieldsInfo = hasFieldsInfo;
//        if (orders != null) {
//            this.orders.clear();
//            this.orders.addAll(orders);
//        }
//        return this;
//    }
//
//    /**
//     * 构建SelectQueryParam对象
//     * <p>执行参数校验并返回不可变的查询参数对象</p>
//     *
//     * @return SelectQueryParam实例
//     * @throws IllegalArgumentException 当参数校验失败时抛出
//     */
//    public GirAdvQueryRequest build() {
//        // 校验：两种模式至少选一种
//        boolean hasObjectMode = tableOrSqlView != null;
//        boolean hasCustomSqlMode = customSql != null && !customSql.trim().isEmpty();
//
//        if (!hasObjectMode && !hasCustomSqlMode) {
//            throw new IllegalArgumentException(
//                    "Either (table + fields + where) or customSql must be provided"
//            );
//        }
//
//        // 对象模式校验
//        if (hasObjectMode) {
//            if (tableOrSqlView.trim().isEmpty()) {
//                throw new IllegalArgumentException("tableOrViewName cannot be empty");
//            }
//        }
//
//        // 分页参数校验
//        if (pageNum != null && pageSize != null && pageSize <= 0) {
//            throw new IllegalArgumentException("pageSize must be greater than 0");
//        }
//
//        return new GirAdvQueryRequest(this);
//    }
//}
