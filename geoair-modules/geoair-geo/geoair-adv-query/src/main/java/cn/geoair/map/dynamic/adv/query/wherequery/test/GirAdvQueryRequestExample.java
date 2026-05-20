package cn.geoair.map.dynamic.adv.query.wherequery.test;

import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQuerySqlBuilderExample;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * GirAdvQueryRequest 使用示例
 * <p>展示各种查询场景的使用方法</p>
 *
 * @author 张俊
 * @date Created in 2026/5/18 20:07
 */
public class GirAdvQueryRequestExample {

    public static void main(String[] args) {
        // 示例1：基础查询 - 传统字符串方式
        basicStringExample();

        // 示例2：Lambda风格查询
        lambdaStyleExample();

        // 示例3：复杂条件查询
        complexConditionExample();

        // 示例4：分页查询
        paginationExample();

        // 示例5：排序示例
        orderByExample();

        // 示例6：自定义SQL模式
        customSqlExample();

        // 示例7：字段别名和映射
        fieldAliasExample();

        // 示例8：业务场景 - 用户管理
        userManagementExample();

        // 示例9：业务场景 - 订单查询
        orderQueryExample();

        // 示例10：动态条件构建
        dynamicConditionExample();
    }

    /**
     * 示例1：基础查询 - 传统字符串方式
     */
    public static void basicStringExample() {
        System.out.println("========== 基础查询（字符串方式） ==========");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)

                .fields("id", "name", "age", "email")
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
        System.out.println("偏移量: " + query.getOffset());
        System.out.println("  - ALL：" + GirAdvQuerySqlBuilderExample.getGirAdvQuerySqlBuilderPg().buildSelectSql(query));
    }

    /**
     * 示例2：Lambda风格查询（推荐）
     */
    public static void lambdaStyleExample() {
        System.out.println("\n========== Lambda风格查询 ==========");

        // 方式1：使用Lambda表达式指定字段
        GirAdvQueryRequest query1 = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getCreateTime, User::getAge, User::getEmail)
                .whereLambda(w -> w
                        .eq(User::getStatus, 1)
                        .ge(User::getAge, 18)
                        .like(User::getName, "张")
                )
                .orderByDesc(User::getCreateTime)
                .page(1, 20)
                .build();

        System.out.println("Lambda查询1构建成功");
        System.out.println("  - ALL：" + GirAdvQuerySqlBuilderExample.getGirAdvQuerySqlBuilderPg().buildSelectSql(query1));
        // 方式2：带驼峰转下划线
        GirAdvQueryRequest query2 = GirAdvQueryRequest.builder(User.class, true)
                .table("user")
                .fields(User::getId, User::getCreateTime)
                .whereLambda(w -> w.between(User::getAge, 18, 30))
                .build();

        System.out.println("Lambda查询2构建成功（自动驼峰转下划线）");
    }

    /**
     * 示例3：复杂条件查询
     */
    public static void complexConditionExample() {
        System.out.println("\n========== 复杂条件查询 ==========");

        // 查询：年龄>=18 并且 (状态=1 或者 角色='admin')，且未被删除
        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName, User::getAge, User::getRole)
                .whereLambda(w -> w
                        .ge(User::getAge, 18)
                        .and(sub -> sub
                                .eq(User::getStatus, 1)
                                .or()
                                .eq(User::getRole, "admin")
                        )
                        .isNull(User::getDeletedAt)
                )
                .orderByDesc(User::getCreateTime)
                .page(1, 15)
                .build();

        System.out.println("复杂条件查询构建成功");
        System.out.println("是否有WHERE条件: " + query.getWhereOption().hasExpression());
        System.out.println("  - ALL：" + GirAdvQuerySqlBuilderExample.getGirAdvQuerySqlBuilderPg().buildSelectSql(query));
    }

    /**
     * 示例4：分页查询
     */
    public static void paginationExample() {
        System.out.println("\n========== 分页查询 ==========");

        // 标准分页（页码从1开始）
        GirAdvQueryRequest query1 = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName)
                .whereLambda(w -> w.eq(User::getStatus, 1))
                .page(2, 20)  // 第2页，每页20条
                .build();

        System.out.println("标准分页:");
        System.out.println("  - 页码: " + query1.getPageNum());
        System.out.println("  - 每页条数: " + query1.getPageSize());
        System.out.println("  - 偏移量: " + query1.getOffset());
        System.out.println("  - 实际页码: " + query1.getActualPageNum());

        // 从0开始的分页
        GirAdvQueryRequest query2 = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName)
                .page(0, 20, true)  // 页码从0开始
                .build();

        System.out.println("\n从0开始的分页:");
        System.out.println("  - 偏移量: " + query2.getOffset());
        System.out.println("  - 实际页码: " + query2.getActualPageNum());

        // 不分页查询所有
        GirAdvQueryRequest query3 = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName)
                .noPage()
                .build();

        System.out.println("\n不分页: " + !query3.hasPagination());
    }

    /**
     * 示例5：排序示例
     */
    public static void orderByExample() {
        System.out.println("\n========== 排序示例 ==========");

        // 1. 字段排序
        GirAdvQueryRequest query1 = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName)
                .orderByAsc(User::getName)      // 名称升序
                .orderByDesc(User::getAge)      // 年龄降序
                .build();

        System.out.println("字段排序 ORDER BY: " + query1.buildOrderByClause());

        // 2. 函数排序
        GirAdvQueryRequest query2 = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName)
                .orderByAscFunction("CAST(id AS numeric)")
                .orderByDescFunction("LENGTH(name)")
                .build();

        System.out.println("函数排序 ORDER BY: " + query2.buildOrderByClause());

        // 3. 混合排序
        GirAdvQueryRequest query3 = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName)
                .order(OrderApo.ofASCFieldName("name"))
                .order(OrderApo.ofDescFunction("YEAR(create_time)"))
                .build();

        System.out.println("混合排序 ORDER BY: " + query3.buildOrderByClause());

        // 4. 批量添加排序
        List<OrderApo> orders = Arrays.asList(
                OrderApo.ofDescFieldName("create_time"),
                OrderApo.ofASCFieldName("id")
        );
        GirAdvQueryRequest query4 = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName)
                .orders(orders)
                .build();

        System.out.println("批量排序 ORDER BY: " + query4.buildOrderByClause());
    }

    /**
     * 示例6：自定义SQL模式
     */
    public static void customSqlExample() {
        System.out.println("\n========== 自定义SQL模式 ==========");

        // 完全自定义SQL
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .customSql("SELECT id, name, age FROM user WHERE status = 1")
                .page(1, 10)
                .build();

        System.out.println("是否为自定义SQL模式: " + query.isCustomSqlMode());
        System.out.println("自定义SQL: " + query.getCustomSql());

        // 带参数的自定义SQL（通过占位符）
        GirAdvQueryRequest query2 = GirAdvQueryRequest.builder()
                .customSql("SELECT * FROM user WHERE age > #{minAge} AND age < #{maxAge}")
                .page(1, 10)
                .build();

        System.out.println("带参数SQL: " + query2.getCustomSql());
    }

    /**
     * 示例7：字段别名和映射
     */
    public static void fieldAliasExample() {
        System.out.println("\n========== 字段别名示例 ==========");

        // 方式1：使用别名
        GirAdvQueryRequest query1 = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .field(User::getId, "userId")
                .field(User::getName, "userName")
                .field(User::getAge, "userAge")
                .whereLambda(w -> w.eq(User::getStatus, 1))
                .build();

        System.out.println("带别名字段: " + query1.getFieldNames());

        // 方式2：SQL表达式字段
        GirAdvQueryRequest query2 = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName)
                .fieldExpr("COUNT(*)", "total")
                .fieldExpr("YEAR(create_time)", "year")
                .build();

        System.out.println("表达式字段: " + query2.getFieldNames());
    }

    /**
     * 示例8：业务场景 - 用户管理
     */
    public static void userManagementExample() {
        System.out.println("\n========== 用户管理场景 ==========");

        // 查询活跃用户，年龄18-35岁，按注册时间倒序
        GirAdvQueryRequest activeUsers = GirAdvQueryRequest.builder(User.class, true)
                .table("sys_user")
                .fields(User::getId, User::getName, User::getAge, User::getEmail, User::getCreateTime)
                .whereLambda(w -> w
                        .eq(User::getStatus, 1)
                        .between(User::getAge, 18, 35)
                        .isNull(User::getDeletedAt)
                )
                .orderByDesc(User::getCreateTime)
                .page(1, 20)
                .build();

        System.out.println("活跃用户查询构建成功");

        // 搜索用户（动态条件）
        String keyword = "张";
        Integer minAge = 18;
        Integer maxAge = 60;

        GirAdvQueryRequest searchUsers = GirAdvQueryRequest.builder(User.class)
                .table("sys_user")
                .fields(User::getId, User::getName, User::getAge)
                .whereLambda(w -> {
                    w.isNull(User::getDeletedAt);
                    if (keyword != null && !keyword.isEmpty()) {
                        w.like(User::getName, keyword);
                    }
                    if (minAge != null) {
                        w.ge(User::getAge, minAge);
                    }
                    if (maxAge != null) {
                        w.le(User::getAge, maxAge);
                    }
                })
                .orderByAsc(User::getName)
                .page(1, 15)
                .build();

        System.out.println("用户搜索查询构建成功");
    }

    /**
     * 示例9：业务场景 - 订单查询
     */
    public static void orderQueryExample() {
        System.out.println("\n========== 订单查询场景 ==========");




        System.out.println("订单查询构建成功");

    }

    /**
     * 示例10：动态条件构建
     */
    public static void dynamicConditionExample() {
        System.out.println("\n========== 动态条件构建 ==========");

        // 模拟前端传入的参数
        String name = "张三";
        Integer minAge = null;  // 可能为空
        Integer maxAge = 30;
        Integer status = 1;
        List<String> roles = Arrays.asList("admin", "manager");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName, User::getAge, User::getRole)
                .whereLambda(w -> {
                    // 基础条件：未删除
                    w.isNull(User::getDeletedAt);

                    // 动态添加条件
                    if (name != null && !name.isEmpty()) {
                        w.like(User::getName, name);
                    }
                    if (minAge != null) {
                        w.ge(User::getAge, minAge);
                    }
                    if (maxAge != null) {
                        w.le(User::getAge, maxAge);
                    }
                    if (status != null) {
                        w.eq(User::getStatus, status);
                    }
                    if (roles != null && !roles.isEmpty()) {
                        w.in(User::getRole, roles);
                    }
                })
                .orderByDesc(User::getCreateTime)
                .page(1, 10)
                .build();

        System.out.println("动态条件查询构建成功");
        System.out.println("是否有条件: " + query.getWhereOption().hasExpression());

        // 演示NULL处理策略
        GirAdvQueryRequest queryWithIgnoreNull = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName)
                .whereLambda(w -> w.eq(User::getName, null))  // 值为null的条件
                .ignoreNull()  // 忽略null值条件
                .build();

        System.out.println("\n忽略NULL值后条件数量: " + queryWithIgnoreNull.getWhereOption().hasExpression());
    }

    /**
     * 示例11：工具方法使用
     */
    public static void utilityMethodsExample() {
        System.out.println("\n========== 工具方法示例 ==========");

        GirAdvQueryRequest query = GirAdvQueryRequest.builder(User.class)
                .table("user")
                .fields(User::getId, User::getName)
                .orderByDesc(User::getCreateTime)
                .orderByAsc(User::getName)
                .page(2, 20)
                .build();

        // 判断方法
        System.out.println("是否为自定义SQL模式: " + query.isCustomSqlMode());
        System.out.println("是否有分页: " + query.hasPagination());
        System.out.println("是否有排序: " + query.hasOrders());

        // 计算分页
        System.out.println("分页偏移量: " + query.getOffset());
        System.out.println("实际页码: " + query.getActualPageNum());

        // 构建ORDER BY
        System.out.println("ORDER BY子句: " + query.buildOrderByClause());
    }
}


