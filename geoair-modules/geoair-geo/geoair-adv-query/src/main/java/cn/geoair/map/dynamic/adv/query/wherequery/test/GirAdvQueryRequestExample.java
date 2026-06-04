package cn.geoair.map.dynamic.adv.query.wherequery.test;

import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import cn.geoair.map.dynamic.adv.query.wherequery.*;
import cn.hutool.core.date.DateUtil;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * GirAdvQueryRequest 使用示例
 *
 * @author zhangjun
 */
public class GirAdvQueryRequestExample {
    @Data
    static class User {
        private Long id;
        private String name;
        private Integer age;
        private String email;
        private Integer status;
        private Long deptId;
        private Date createTime;

    }

    @Data
    static class Order {
        private Long id;
        private Long userId;
        private BigDecimal amount;
        private String status;
        private Date createTime;

    }

    private static GirAdvSqlComposer sqlBuilder = GirAdvSqlComposerMockProvider.getMockPostgresql();



    /**
     * 示例1：基础查询 - 简单条件
     */
    public static void example1_BasicQuery() {
        System.out.println("========== 示例1：基础查询 ==========");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .fields("id", "name", "age", "email", "create_time")
                .where(w -> w
                        .eq("status", 1)
                        .ge("age", 18)
                )
                .orderByAsc("name")
                .page(1, 10)
                .build();

        System.out.println("表名: " + query.getTableOrSqlView());
        System.out.println("字段: " + query.getFieldNames());
        System.out.println("是否分页: " + query.hasPagination());
        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例2：复杂条件查询 - AND/OR组合
     */
    public static void example2_ComplexCondition() {
        System.out.println("========== 示例2：复杂条件查询 ==========");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .fields("id", "name", "age", "dept_id")
                .where(w -> w
                        .eq("status", 1)
                        .and(
                        ).group(sub -> sub
                                .eq("dept_id", 100)
                                .or()
                                .eq("dept_id", 200)
                                .or()
                                .eq("dept_id", 300))
                        .gt("age", 18)
                        .like("name", "张")
                )
                .orderByDesc("create_time")
                .orderByAsc("name")
                .build();

        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例3：范围查询 - BETWEEN/IN
     */
    public static void example3_RangeQuery() {
        System.out.println("========== 示例3：范围查询 ==========");

        List<Integer> statusList = Arrays.asList(1, 2, 3);

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .fields("id", "name", "age", "status")
                .where(w -> w
                        .in("status", statusList)
                        .between("age", 18, 60)
                        .ge("create_time", DateUtil.beginOfDay(DateUtil.date()))
                )
                .orderByDesc("create_time")
                .build();

        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例4：空值判断 - IS NULL/IS NOT NULL
     */
    public static void example4_NullCheck() {
        System.out.println("========== 示例4：空值判断 ==========");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .fields("id", "name", "email")
                .where(w -> w
                        .isNotNull("email")
                        .eq("status", 1)
                        .or()
                        .isNull("phone")
                )
                .build();

        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例5：子查询 - EXISTS/NOT EXISTS
     */
    public static void example5_SubQuery() {
        System.out.println("========== 示例5：子查询 ==========");

        // 子查询：查询有订单的用户
        String subSql = "SELECT 1 FROM orders o WHERE o.user_id = u.id";

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .fields("id", "name")
                .where(w -> w
                        .eq("status", 1)
                        .exprExists(subSql)
                )
                .build();

        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例6：分组查询 - GROUP BY + HAVING
     */
    public static void example6_GroupByAndHaving() {
        System.out.println("========== 示例6：分组查询 ==========");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .fields("dept_id" ).fieldExpr("COUNT(*) as user_count", "AVG(age) as avg_age")
                .where(w -> w.eq("status", 1))
                .groupBy("dept_id")
                .having(h -> h
                        .expr("COUNT(*)", AdvOperatorEnums.大于, 10)
                        .expr("AVG(age)", AdvOperatorEnums.大于等于, 25)
                )
                .orderByDesc("user_count")
                .build();

        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
        System.out.println("COUNT SQL: " + sqlBuilder.buildCountSql(query));
    }

    /**
     * 示例7：去重查询 - DISTINCT
     */
    public static void example7_DistinctQuery() {
        System.out.println("========== 示例7：去重查询 ==========");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .distinct()
                .fields("dept_id", "status")
                .where(w -> w.eq("status", 1))
                .build();

        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例8：表达式查询 - SQL函数和计算
     */
    public static void example8_ExpressionQuery() {
        System.out.println("========== 示例8：表达式查询 ==========");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .fields("id", "name",
                        "DATE_FORMAT(create_time, '%Y-%m-%d') as create_date",
                        "CASE WHEN age >= 60 THEN '老年' WHEN age >= 18 THEN '成年' ELSE '未成年' END as age_group")
                .where(w -> w
                        .expr("YEAR(create_time)", AdvOperatorEnums.等于, 2024)
                        .expr("MONTH(create_time)", AdvOperatorEnums.大于等于, 6)
                        .expr("age * 2", AdvOperatorEnums.小于, 100)
                )
                .orderByDesc("create_time")
                .build();

        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例9：自定义SQL查询 - 复杂业务场景
     */
    public static void example9_CustomSql() {
        System.out.println("========== 示例9：自定义SQL ==========");

        String customSql = "SELECT u.id, u.name, u.age, " +
                "COALESCE(o.order_count, 0) as order_count, " +
                "COALESCE(o.total_amount, 0) as total_amount " +
                "FROM user u " +
                "LEFT JOIN (" +
                "  SELECT user_id, COUNT(*) as order_count, SUM(amount) as total_amount " +
                "  FROM orders " +
                "  WHERE status = 'COMPLETED' " +
                "  GROUP BY user_id" +
                ") o ON u.id = o.user_id " +
                "WHERE u.status = 1";

        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .customSql(customSql)
                .orderByDesc("total_amount")
                .page(1, 20)
                .build();

        System.out.println("自定义SQL: " + query.getCustomSql());
        System.out.println("分页SQL: " + sqlBuilder.buildPageSql(query));
    }

    /**
     * 示例10：分页查询 - 带总数统计
     */
    public static void example10_PaginationQuery() {
        System.out.println("========== 示例10：分页查询 ==========");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .fields("id", "name", "age", "create_time")
                .where(w -> w
                        .eq("status", 1)
                        .like("name", "测试")
                        .between("create_time",
                                DateUtil.beginOfYear(DateUtil.date()),
                                DateUtil.endOfYear(DateUtil.date()))
                )
                .orderByDesc("create_time")
                .page(2, 15)  // 第2页，每页15条
                .pageNumStartZero(false)  // 页码从1开始
                .build();

        System.out.println("当前页: " + query.getPageNum());
        System.out.println("每页条数: " + query.getPageSize());
        System.out.println("偏移量: " + query.getOffset());
        System.out.println("查询SQL: " + sqlBuilder.buildSelectSql(query));
        System.out.println("统计SQL: " + sqlBuilder.buildCountSql(query));
        System.out.println("分页SQL: " + sqlBuilder.buildPageSql(query));
    }

    /**
     * 示例11：Lambda表达式方式 - 类型安全（如果支持）
     */
    public static void example11_LambdaQuery() {
        System.out.println("========== 示例11：Lambda表达式方式 ==========");

        // 注意：这需要QueryRequestBuilder支持Lambda表达式
        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class, true)
                .fields(User::getId, User::getName, User::getAge)
                .whereLambda(w -> w
                        .eq(User::getStatus, 1)
                        .ge(User::getAge, 18)
                        .like(User::getName, "张")
                )
                .orderByAsc(User::getName)
                .groupBy(User::getDeptId)
                .having(h -> h
                        .expr("COUNT(*)", AdvOperatorEnums.大于, 5)
                )
                .page(1, 10)
                .build();

        System.out.println("表名: " + query.getTableOrSqlView());
        System.out.println("字段: " + query.getFieldNames());
        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例12：SQL视图查询 - 子查询作为表
     */
    public static void example12_SqlViewQuery() {
        System.out.println("========== 示例12：SQL视图查询 ==========");

        String sqlView = "SELECT u.id, u.name, u.age, d.dept_name " +
                "FROM user u " +
                "LEFT JOIN dept d ON u.dept_id = d.id " +
                "WHERE u.status = 1";

        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table(sqlView)
                .sqlViewTableNameAlias("user_view")
                .fields("id", "name", "age", "dept_name")
                .where(w -> w
                        .ge("age", 18)
                        .like("name", "张")
                )
                .orderByDesc("age")
                .page(1, 20)
                .build();

        System.out.println("SQL视图: " + query.getTableOrSqlView());
        System.out.println("别名: " + query.getSqlViewTableNameAlias());
        System.out.println("生成SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例13：多表关联查询 - 使用表达式条件
     */
    public static void example13_JoinQuery() {
        System.out.println("========== 示例13：多表关联查询 ==========");

        // 注意：这里使用自定义SQL方式处理多表关联
        String joinSql = "SELECT u.id, u.name, u.age, o.order_id, o.amount " +
                "FROM user u " +
                "INNER JOIN orders o ON u.id = o.user_id " +
                "WHERE u.status = 1";

        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .customSql(joinSql)
                .where(w -> w  // 这个where会追加到自定义SQL后面
                        .ge("o.amount", 100)
                        .eq("o.status", "COMPLETED")
                )
                .orderByDesc("o.create_time")
                .build();

        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例14：聚合函数查询 - 统计报表
     */
    public static void example14_AggregateQuery() {
        System.out.println("========== 示例14：聚合函数查询 ==========");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .fields("dept_id",
                        "COUNT(*) as total_count",
                        "SUM(age) as total_age",
                        "AVG(age) as avg_age",
                        "MAX(age) as max_age",
                        "MIN(age) as min_age")
                .where(w -> w.eq("status", 1))
                .groupBy("dept_id")
                .having(h -> h.expr("COUNT(*)", AdvOperatorEnums.大于, 5))
                .orderByDesc("total_count")
                .build();

        System.out.println("SQL: " + sqlBuilder.buildSelectSql(query));
    }

    /**
     * 示例15：复杂业务场景 - 组合所有特性
     */
    public static void example15_ComplexBusinessQuery() {
        System.out.println("========== 示例15：复杂业务场景 ==========");

        // 查询：每个部门中年龄大于18岁的用户，统计各部门人数和平均年龄，
        // 只显示人数大于5的部门，按人数倒序，分页显示

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .distinct()
                .fields("dept_id", "COUNT(*) as user_count", "AVG(age) as avg_age")
                .where(w -> w
                        .eq("status", 1)
                        .gt("age", 18)
                        .in("dept_id", Arrays.asList(1, 2, 3, 4, 5))
                        .between("create_time",
                                DateUtil.parse("2024-01-01"),
                                DateUtil.parse("2024-12-31"))
                )
                .groupBy("dept_id")
                .having(h -> h
                        .expr("COUNT(*)", AdvOperatorEnums.大于, 5)
                        .expr("AVG(age)", AdvOperatorEnums.大于等于, 25)
                )
                .orderByDesc("user_count")
                .orderByAsc("dept_id")
                .page(1, 10)
                .pageNumStartZero(false)
                .build();

        System.out.println("查询参数:");
        System.out.println("  - 表名: " + query.getTableOrSqlView());
        System.out.println("  - 是否去重: " + query.hasDistinct());
        System.out.println("  - 是否分组: " + query.hasGroupBy());
        System.out.println("  - GROUP BY字段: " + query.getGroupByFields());
        System.out.println("  - 是否有HAVING: " + query.hasHaving());
        System.out.println("  - 当前页: " + query.getPageNum());
        System.out.println("  - 偏移量: " + query.getOffset());
        System.out.println();

        System.out.println("查询SQL: " + sqlBuilder.buildSelectSql(query));
        System.out.println("统计SQL: " + sqlBuilder.buildCountSql(query));
        System.out.println("分页SQL: " + sqlBuilder.buildPageSql(query));
    }

    public static void main(String[] args) {
        runAllExamples();

    }

    /**
     * 运行所有示例
     */
    public static void runAllExamples() {
        example1_BasicQuery();
        System.out.println();

        example2_ComplexCondition();
        System.out.println();

        example3_RangeQuery();
        System.out.println();

        example4_NullCheck();
        System.out.println();

        example5_SubQuery();
        System.out.println();

        example6_GroupByAndHaving();
        System.out.println();

        example7_DistinctQuery();
        System.out.println();

        example8_ExpressionQuery();
        System.out.println();

        example9_CustomSql();
        System.out.println();

        example10_PaginationQuery();
        System.out.println();

        example12_SqlViewQuery();
        System.out.println();

        example13_JoinQuery();
        System.out.println();

        example14_AggregateQuery();
        System.out.println();

        example15_ComplexBusinessQuery();
        System.out.println();

        // Lambda示例需要特殊支持
        example11_LambdaQuery();
    }
}
