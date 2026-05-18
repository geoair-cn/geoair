package cn.geoair.map.dynamic.adv.query.wherequery.test;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.comp.dynamic.ds.MockDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.dialect.mysql.MysqlDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.dialect.pg.PgDialectTableNameUtil;
import cn.geoair.map.dynamic.adv.query.enums.AdvNullHandling;
import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQuerySqlBuilder;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * WHERE查询示例
 * <p>展示GirAdvQuerySqlBuilder的各种使用场景</p>
 *
 * @author zhangjun
 */
public class WhereQueryExample {

    private final GirAdvQuerySqlBuilder sqlBuilder;

    public WhereQueryExample(DialectTableNameProcessor dialectProcessor, IDataSourceGetter dataSourceGetter) {
        this.sqlBuilder = new GirAdvQuerySqlBuilder(dialectProcessor, dataSourceGetter);
    }

    public static void main(String[] args) {
        DialectTableNameProcessor dialect = PgDialectTableNameUtil.getInstance();
        DialectTableNameProcessor masql = MysqlDialectTableNameUtil.getInstance();
        IDataSourceGetter dataSourceGetter = MockDataSourceGetter.getInstance();
        WhereQueryExample example = new WhereQueryExample(masql, dataSourceGetter);

//        example.runAllExamples();
        GirAdvWhereFilter filter = GirAdvWhereFilter.of()
                .expr("YEAR(create_time)", AdvOperatorEnums.等于, 2024)
                .expr("price * quantity", AdvOperatorEnums.大于, 1000)
                .eq("status", 1);
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "status")
                .where(filter
                ).order(OrderApo.ofASCFunction("create_time1")).order(OrderApo.ofASCFieldName("aaaaa"))
                .build();

//        GirAdvQuerySqlBuilder.SqlBuildResult result = example.sqlBuilder.buildSelectSql(query);
//        printResult("示例1：表达式", result);

        WhereQueryExample example2 = new WhereQueryExample(dialect, dataSourceGetter);
        example2.runAllExamples();
    }

    public void runAllExamples() {


        // 示例1：简单等值查询
        example1();

        // 示例2：比较运算符查询
        example2();

        // 示例3：IN和BETWEEN查询
        example3();

        // 示例4：模糊查询
        example4();

        // 示例5：NULL判断查询
        example5();

        // 示例6：AND条件组
        example6();

        // 示例7：OR条件组
        example7();

        // 示例8：复杂嵌套查询
        example8();

        // 示例9：带排序的分页查询
        example9();

        // 示例10：自定义SQL模式
        example10();

        // 示例11：忽略NULL值查询
        example11();

        // 示例12：使用OrderApo排序
        example12();

        // 示例13：多层嵌套复杂场景
        example13();

        // 示例14：NOT条件组
        example14();

        // 示例15：组合所有特性
        example15();
    }

    /**
     * 示例1：简单等值查询
     */
    void example1() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "status")
                .where(GirAdvWhereFilter.of()
                        .eq("name", "张三")
                        .eq("status", 1)
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例1：简单等值查询", result);
    }

    /**
     * 示例2：比较运算符查询
     */
    void example2() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "age", "score")
                .where(GirAdvWhereFilter.of()
                        .gt("age", 18)
                        .ge("score", 60)
                        .lt("create_time", new Date())
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例2：比较运算符查询", result);
    }

    /**
     * 示例3：IN和BETWEEN查询
     */
    void example3() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "age")
                .where(GirAdvWhereFilter.of()
                        .in("id", Arrays.asList(1, 2, 3, 4, 5))
                        .between("age", 18, 30)
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例3：IN和BETWEEN查询", result);
    }

    /**
     * 示例4：模糊查询
     */
    void example4() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "email")
                .where(GirAdvWhereFilter.of()
                        .like("name", "张")
                        .likeLeft("email", "admin")
                        .likeRight("phone", "1234")
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例4：模糊查询", result);
    }

    /**
     * 示例5：NULL判断查询
     */
    void example5() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "email")
                .where(GirAdvWhereFilter.of()
                        .isNull("deleted_at")
                        .isNotNull("email")
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例5：NULL判断查询", result);
    }

    /**
     * 示例6：AND条件组
     */
    void example6() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "status", "age", "score")
                .where(GirAdvWhereFilter.of()
                        .eq("status", 1)
                        .group(group -> group
                                .gt("age", 18)
                                .gt("score", 60)
                        )
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例6：AND条件组", result);
    }

    /**
     * 示例7：OR条件组
     */
    void example7() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "status", "age", "vip")
                .where(GirAdvWhereFilter.of()
                        .eq("status", 1)
                        .or()
                        .group(group -> group
                                .gt("age", 18)
                                .eq("vip", 1)
                                .eq("vip", 1)
                        )
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例7：OR条件组", result);
    }

    /**
     * 示例8：复杂嵌套查询
     */
    void example8() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "age", "status", "dept_id", "role")
                .where(GirAdvWhereFilter.of()
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

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例8：复杂嵌套查询", result);
    }

    /**
     * 示例9：带排序的分页查询
     */
    void example9() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "status", "create_time")
                .where(GirAdvWhereFilter.of().eq("status", 1))
                .orderByDesc("create_time")
                .orderByAsc("id")
                .page(2, 10)
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildPageSql(query);
        printResult("示例9：带排序的分页查询", result);
    }

    /**
     * 示例10：自定义SQL模式
     */
    void example10() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .customSql("SELECT * FROM \"user\" WHERE age > 18")
                .orderByDesc("create_time")
                .page(1, 20)
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildPageSql(query);
        printResult("示例10：自定义SQL模式", result);
    }

    /**
     * 示例11：忽略NULL值查询
     */
    void example11() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "age")
                .where(GirAdvWhereFilter.of()
                        .eq("name", "张三")
                        .eq("age", null)
                )
                .nullHandling(AdvNullHandling.IGNORE)
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例11：忽略NULL值查询", result);
    }

    /**
     * 示例12：使用OrderApo排序
     */
    void example12() {
        List<OrderApo> orders = Arrays.asList(
                OrderApo.ofDescFunction("CAST(age AS numeric)"),
                OrderApo.ofASCFieldName("name")
        );

        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "age")
                .where(GirAdvWhereFilter.of())
                .orders(orders)
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例12：使用OrderApo排序", result);
    }

    /**
     * 示例13：多层嵌套复杂场景
     */
    void example13() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "age", "vip", "status", "score", "level")
                .where(GirAdvWhereFilter.of()
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

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例13：多层嵌套复杂场景", result);
    }

    /**
     * 示例14：NOT条件组
     */
    void example14() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "status", "age")
                .where(GirAdvWhereFilter.of()
                        .notGroup(group -> group.eq("status", 0))
                        .notGroup(group -> group.lt("age", 18))
                )
                .build();

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildSelectSql(query);
        printResult("示例14：NOT条件组", result);
    }

    /**
     * 示例15：组合所有特性
     */
    void example15() {
        GirAdvQueryRequest query = GirAdvQueryRequest.builder()

                .table("user")
                .fields("id", "name", "age", "status", "score", "dept_id", "create_time")
                .where(GirAdvWhereFilter.of()
                        .eq("status", 1)
                        .isNotNull("email")
                        .group(ageGroup -> ageGroup
                                .gt("age", 18)
                                .and()
                                .lt("age", 60)
                        )
                        .or()
                        .group(vipGroup -> vipGroup
                                .eq("vip", 1)
                                .gt("score", 80)
                        )
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

        GirAdvQuerySqlBuilder.SqlBuildResult result = sqlBuilder.buildPageSql(query);
        printResult("示例15：组合所有特性", result);
    }

    private static void printResult(String title, GirAdvQuerySqlBuilder.SqlBuildResult result) {
        System.out.println("【" + title + "】");
        System.out.println("SQL: " + result.getSql());
        System.out.println("参数: " + result.getParams());
        System.out.println("可执行SQL: " + result.getExecutableSql());
        System.out.println();
    }


}
