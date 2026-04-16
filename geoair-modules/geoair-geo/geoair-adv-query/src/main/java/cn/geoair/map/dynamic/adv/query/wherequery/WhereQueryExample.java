package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvNullHandling;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class WhereQueryExample {

    public static void main(String[] args) {

        // ==================== 示例1：简单等值查询 ====================
        example1();

        // ==================== 示例2：比较运算符查询 ====================
        example2();

        // ==================== 示例3：IN和BETWEEN查询 ====================
        example3();

        // ==================== 示例4：模糊查询 ====================
        example4();

        // ==================== 示例5：NULL判断查询 ====================
        example5();

        // ==================== 示例6：AND条件组 ====================
        example6();

        // ==================== 示例7：OR条件组 ====================
        example7();

        // ==================== 示例8：复杂嵌套查询 ====================
        example8();

        // ==================== 示例9：带排序的分页查询 ====================
        example9();

        // ==================== 示例10：自定义SQL模式 ====================
        example10();

        // ==================== 示例11：忽略NULL值查询 ====================
        example11();

        // ==================== 示例12：使用OrderApo排序 ====================
        example12();

        // ==================== 示例13：多层嵌套复杂场景 ====================
        example13();

        // ==================== 示例14：NOT条件组 ====================
        example14();

        // ==================== 示例15：组合所有特性 ====================
        example15();
    }

    /**
     * 示例1：简单等值查询
     * SQL: SELECT * FROM user WHERE name = ? AND status = ?
     */
    static void example1() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "status")
                .where(GirAdvQueryFilter.of()
                        .eq("name", "张三")
                        .eq("status", 1)
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例1】简单等值查询");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例2：比较运算符查询
     * SQL: SELECT * FROM user WHERE age > ? AND score >= ? AND create_time < ?
     */
    static void example2() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "age", "score", "create_time")
                .where(GirAdvQueryFilter.of()
                        .gt("age", 18)
                        .ge("score", 60)
                        .lt("create_time", new Date())
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例2】比较运算符查询");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例3：IN和BETWEEN查询
     * SQL: SELECT * FROM user WHERE id IN (?, ?, ?) AND age BETWEEN ? AND ?
     */
    static void example3() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "age")
                .where(GirAdvQueryFilter.of()
                        .in("id", Arrays.asList(1, 2, 3, 4, 5))
                        .between("age", 18, 30)
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例3】IN和BETWEEN查询");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例4：模糊查询
     * SQL: SELECT * FROM user WHERE name LIKE ? AND email LIKE ?
     */
    static void example4() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "email")
                .where(GirAdvQueryFilter.of()
                        .like("name", "张")
                        .likeLeft("email", "admin")
                        .likeRight("phone", "1234")
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例4】模糊查询");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例5：NULL判断查询
     * SQL: SELECT * FROM user WHERE deleted_at IS NULL AND email IS NOT NULL
     */
    static void example5() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "email")
                .where(GirAdvQueryFilter.of()
                        .isNull("deleted_at")
                        .isNotNull("email")
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例5】NULL判断查询");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例6：AND条件组
     * SQL: SELECT * FROM user WHERE status = ? AND (age > ? AND score > ?)
     */
    static void example6() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "status", "age", "score")
                .where(GirAdvQueryFilter.of()
                        .eq("status", 1)
                        .group(group -> group
                                .gt("age", 18)
                                .gt("score", 60)
                        )
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例6】AND条件组");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例7：OR条件组
     * SQL: SELECT * FROM user WHERE status = ? OR (age > ? AND vip = ?)
     */
    static void example7() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "status", "age", "vip")
                .where(GirAdvQueryFilter.of()
                        .eq("status", 1)
                        .or()
                        .group(group -> group
                                .gt("age", 18)
                                .eq("vip", 1)
                        )
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例7】OR条件组");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例8：复杂嵌套查询
     * SQL: SELECT * FROM user WHERE name LIKE ? AND (age > ? OR status = ?) AND (dept_id = ? OR role = ?)
     */
    static void example8() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "age", "status", "dept_id", "role")
                .where(GirAdvQueryFilter.of()
                        .like("name", "张")
                        .group(group -> group
                                .gt("age", 18)
                                .or()
                                .eq("status", 1)
                        )
                        .group(group -> group
                                .eq("dept_id", 100)
                                .or()
                                .eq("role", "admin")
                        )
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例8】复杂嵌套查询");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例9：带排序的分页查询
     * SQL: SELECT * FROM user WHERE status = ? ORDER BY create_time DESC, id ASC LIMIT ? OFFSET ?
     */
    static void example9() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "status", "create_time")
                .where(GirAdvQueryFilter.of().eq("status", 1))
                .orderByDesc("create_time")
                .orderByAsc("id")
                .page(2, 10)
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildPageSql(query);
        System.out.println("【示例9】带排序的分页查询");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例10：自定义SQL模式
     * SQL: SELECT * FROM user WHERE age > 18 ORDER BY create_time DESC LIMIT ? OFFSET ?
     */
    static void example10() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .customSql("SELECT * FROM user WHERE age > 18")
                .orderByDesc("create_time")
                .page(1, 20)
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildPageSql(query);
        System.out.println("【示例10】自定义SQL模式");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例11：忽略NULL值查询
     * SQL: SELECT * FROM user WHERE name = ? (age=null会被忽略)
     */
    static void example11() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "age")
                .where(GirAdvQueryFilter.of()
                        .eq("name", "张三")
                        .eq("age", null)  // 会被忽略
                )
                .nullHandling(AdvNullHandling.IGNORE)
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例11】忽略NULL值查询");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例12：使用OrderApo排序
     * SQL: SELECT * FROM user ORDER BY CAST(age AS numeric) DESC, name ASC
     */
    static void example12() {
        List<OrderApo> orders = Arrays.asList(
                OrderApo.ofDescFunction("CAST(age AS numeric)"),
                OrderApo.ofASCFieldName("name")
        );

        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "age")
                .where(GirAdvQueryFilter.of())
                .orders(orders)
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例12】使用OrderApo排序");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例13：多层嵌套复杂场景
     * SQL: SELECT * FROM user WHERE (name LIKE ? AND (age > ? OR vip = ?)) OR (status IN (?,?) AND (score > ? OR level = ?))
     */
    static void example13() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "age", "vip", "status", "score", "level")
                .where(GirAdvQueryFilter.of()
                        .group(group -> group
                                .like("name", "张")
                                .group(sub -> sub
                                        .gt("age", 18)
                                        .or()
                                        .eq("vip", 1)
                                )
                        )
                        .or()
                        .group(group -> group
                                .in("status", Arrays.asList(1, 2))
                                .group(sub -> sub
                                        .gt("score", 60)
                                        .or()
                                        .eq("level", 3)
                                )
                        )
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例13】多层嵌套复杂场景");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例14：NOT条件组
     * SQL: SELECT * FROM user WHERE NOT (status = ?) AND NOT (age < ?)
     */
    static void example14() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "status", "age")
                .where(GirAdvQueryFilter.of()
                        .notGroup(group -> group.eq("status", 0))
                        .notGroup(group -> group.lt("age", 18))
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildSelectSql(query);
        System.out.println("【示例14】NOT条件组");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }

    /**
     * 示例15：组合所有特性（最复杂场景）
     * 包含：AND/OR组合、IN、BETWEEN、LIKE、NULL判断、排序、分页
     */
    static void example15() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()
                .table("user")
                .fields("id", "name", "age", "status", "score", "dept_id", "create_time")
                .where(GirAdvQueryFilter.of()
                        // 基础条件
                        .eq("status", 1)
                        .isNotNull("email")
                        // 年龄范围
                        .group(ageGroup -> ageGroup
                                .gt("age", 18)
                                .and()
                                .lt("age", 60)
                        )
                        // 或者VIP用户
                        .or()
                        .group(vipGroup -> vipGroup
                                .eq("vip", 1)
                                .gt("score", 80)
                        )
                        // 部门条件
                        .group(deptGroup -> deptGroup
                                .in("dept_id", Arrays.asList(100, 101, 102))
                                .and()
                                .group(sub -> sub
                                        .like("name", "张")
                                        .or()
                                        .like("name", "李")
                                )
                        )
                )
                .orderByDesc("score")
                .orderByAsc("create_time")
                .page(1, 15)
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = GirAdvQuerySqlBuilder.buildPageSql(query);
        System.out.println("【示例15】组合所有特性");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println();
    }


}
