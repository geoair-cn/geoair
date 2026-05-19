package cn.geoair.map.dynamic.adv.query.wherequery.test;

import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQuerySqlBuilderExample;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereLambdaFilter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Lambda条件构建器使用示例
 *
 * @author 张俊
 * @date Created in 2026/5/17
 */
public class LambdaFilterExample {

    public static void main(String[] args) {
        //        // 示例1：基础查询
        //        basicQueryExample();
        //
        //        // 示例2：复杂嵌套查询
        //        nestedQueryExample();
        //
        //        // 示例3：IN和BETWEEN查询
        //        inAndBetweenExample();
        //
        //        // 示例4：模糊查询
        //        likeQueryExample();
        //
        //        // 示例5：NULL判断
        //        nullCheckExample();
        //
        //        // 示例6：SQL表达式
        sqlExpressionExample();

        // 示例7：实际业务场景
        //        businessScenarioExample();
    }

    /** 示例1：基础查询 */
    public static void basicQueryExample() {
        System.out.println("========== 基础查询示例 ==========");

        GirAdvWhereLambdaFilter<User> wrapper =
                GirAdvWhereLambdaFilter.of(User.class)
                        .eq(User::getName, "张三")
                        .ge(User::getAge, 18)
                        .eq(User::getStatus, 1);

        GirAdvWhereFilter whereFilter = wrapper.toWhereFilter();
        System.out.println("查询条件：" + whereFilter);
        // 预期：name = '张三' AND age >= 18 AND status = 1
    }

    /** 示例2：复杂嵌套查询 查询：年龄>=18 并且 (状态=1 或者 角色='admin') */
    public static void nestedQueryExample() {
        System.out.println("\n========== 嵌套查询示例 ==========");

        GirAdvWhereLambdaFilter<User> wrapper =
                GirAdvWhereLambdaFilter.of(User.class)
                        .ge(User::getAge, 18)
                        .and(w -> w.eq(User::getStatus, 1).or().eq(User::getRole, "admin"));

        GirAdvWhereFilter whereFilter = wrapper.toWhereFilter();
        System.out.println("查询条件：age >= 18 AND (status = 1 OR role = 'admin')");
    }

    /** 示例3：IN和BETWEEN查询 */
    public static void inAndBetweenExample() {
        System.out.println("\n========== IN和BETWEEN查询示例 ==========");

        // IN查询
        List<String> roles = Arrays.asList("admin", "manager", "supervisor");
        GirAdvWhereLambdaFilter<User> wrapper1 =
                GirAdvWhereLambdaFilter.of(User.class)
                        .in(User::getRole, roles)
                        .between(User::getAge, 18, 30);

        // 数组IN查询
        Integer[] statusList = {1, 2, 3};
        GirAdvWhereLambdaFilter<User> wrapper2 =
                GirAdvWhereLambdaFilter.of(User.class).in(User::getStatus, statusList);

        System.out.println("IN查询示例完成");
    }

    /** 示例4：模糊查询 */
    public static void likeQueryExample() {
        System.out.println("\n========== 模糊查询示例 ==========");

        GirAdvWhereLambdaFilter<User> wrapper =
                GirAdvWhereLambdaFilter.of(User.class)
                        .like(User::getName, "张") // 全模糊：%张%
                        .likeLeft(User::getEmail, "admin") // 左模糊：admin%
                        .likeRight(User::getPhone, "138"); // 右模糊：%138

        GirAdvWhereFilter whereFilter = wrapper.toWhereFilter();
        System.out.println("模糊查询条件构建完成");
    }

    /** 示例5：NULL判断 */
    public static void nullCheckExample() {
        System.out.println("\n========== NULL判断示例 ==========");

        // 查询未删除的用户
        GirAdvWhereLambdaFilter<User> wrapper =
                GirAdvWhereLambdaFilter.of(User.class)
                        .isNull(User::getDeletedAt)
                        .isNotNull(User::getName);

        GirAdvWhereFilter whereFilter = wrapper.toWhereFilter();
        System.out.println("NULL判断：deleted_at IS NULL AND name IS NOT NULL");
    }

    /** 示例6：SQL表达式 */
    public static void sqlExpressionExample() {
        System.out.println("\n========== SQL表达式示例 ==========");

        GirAdvWhereLambdaFilter<User> wrapper =
                GirAdvWhereLambdaFilter.of(User.class)
                        // 按年份查询
                        .exprEq("YEAR(create_time)", 2024)
                        // 按计算值比较
                        .exprGt("salary * 1.1", new BigDecimal("10000"))
                        // 字符串拼接
                        .exprLike("CONCAT(first_name, ' ', last_name)", "张%");

        GirAdvWhereFilter whereFilter = wrapper.toWhereFilter();
        ArrayList<Object> objects = new ArrayList<>();
        System.out.println(
                "  - Where条件："
                        + GirAdvQuerySqlBuilderExample.getGirAdvQuerySqlBuilderPg()
                                .buildWhereSql(whereFilter, objects));
        System.out.println(
                "  - Where条件："
                        + GirAdvQuerySqlBuilderExample.getGirAdvQuerySqlBuilderOracle()
                                .buildWhereSql(whereFilter, objects));
        System.out.println(
                "  - Where条件："
                        + GirAdvQuerySqlBuilderExample.getGirAdvQuerySqlBuilderMysql()
                                .buildWhereSql(whereFilter, objects));
    }

    /** 示例7：实际业务场景 - 高级用户查询 */
    public static void businessScenarioExample() {
        System.out.println("\n========== 业务场景示例 ==========");

        // 复杂查询：查询VIP用户或管理员，且年龄在18-60之间，未被删除
        GirAdvWhereLambdaFilter<User> wrapper =
                GirAdvWhereLambdaFilter.of(User.class)
                        .and(w -> w.eq(User::getRole, "vip").or().eq(User::getRole, "admin"))
                        .between(User::getAge, 18, 60)
                        .isNull(User::getDeletedAt);

        // 转换为原有的WhereFilter
        GirAdvWhereFilter whereFilter = wrapper.toWhereFilter();
        ArrayList<Object> objects = new ArrayList<>();

        System.out.println("业务查询条件：");
        System.out.println("  - 角色：VIP 或 管理员");
        System.out.println("  - 年龄：18-60岁");
        System.out.println("  - 状态：未删除");
        System.out.println("  - 排序：按创建时间倒序");
        System.out.println("  - 限制：10条");
        System.out.println(
                "  - Where条件："
                        + GirAdvQuerySqlBuilderExample.getGirAdvQuerySqlBuilderPg()
                                .buildWhereSql(whereFilter, objects));
        System.out.println(
                "  - Where条件："
                        + GirAdvQuerySqlBuilderExample.getGirAdvQuerySqlBuilderOracle()
                                .buildWhereSql(whereFilter, objects));
        System.out.println(
                "  - Where条件："
                        + GirAdvQuerySqlBuilderExample.getGirAdvQuerySqlBuilderMysql()
                                .buildWhereSql(whereFilter, objects));
    }

    /** 示例8：动态条件构建（根据参数是否为空） */
    public static void dynamicConditionExample(String name, Integer minAge, String role) {
        System.out.println("\n========== 动态条件示例 ==========");

        GirAdvWhereLambdaFilter<User> wrapper = GirAdvWhereLambdaFilter.of(User.class);

        // 条件性添加
        if (name != null && !name.isEmpty()) {
            wrapper.like(User::getName, name);
        }

        if (minAge != null && minAge > 0) {
            wrapper.ge(User::getAge, minAge);
        }

        if (role != null && !role.isEmpty()) {
            wrapper.eq(User::getRole, role);
        }

        // 默认条件：只查询未删除的用户
        wrapper.isNull(User::getDeletedAt);

        System.out.println("动态条件构建完成，添加的条件：" + wrapper.hasExpression());
    }
}
