package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.map.dynamic.adv.query.enums.AdvLogicOperatorEnums;
import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import lombok.Getter;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static cn.geoair.map.dynamic.adv.query.utils.LambdaUtils.getColumnName;

/**
 * Lambda风格的WHERE条件构建器
 * <p>模仿MyBatis-Plus的LambdaWrapper，提供类型安全的查询条件构建</p>
 * <p>使用示例：</p>
 * <pre>
 * // 基本查询
 * GirAdvWhereLambdaFilter&lt;User&gt; wrapper = GirAdvWhereLambdaFilter.of()
 *     .eq(User::getName, "张三")
 *     .ge(User::getAge, 18)
 *     .like(User::getEmail, "qq.com");
 *
 * // 嵌套条件
 * wrapper.and(w -> w
 *     .eq(User::getStatus, 1)
 *     .or()
 *     .isNull(User::getDeletedAt)
 * );
 *
 * // IN查询
 * wrapper.in(User::getRole, Arrays.asList("admin", "user"));
 * </pre>
 *
 * @author 张俊
 * @date Created in 2026/5/17 19:27
 */
@Getter
public class GirAdvWhereLambdaFilter<T> implements Serializable {

    private final GirAdvWhereFilter whereFilter;
    /**
     * 实体类名称
     */
    private final Class<T> entityClass;
    /**
     * 是否驼峰转下划线
     */
    boolean isToUnderlineCase;


    private GirAdvWhereLambdaFilter(Class<T> entityClass, boolean isToUnderlineCase) {
        this.whereFilter = GirAdvWhereFilter.of();
        this.entityClass = entityClass;
        this.isToUnderlineCase = isToUnderlineCase;
    }

    private GirAdvWhereLambdaFilter(GirAdvWhereFilter whereFilter, Class<T> entityClass, boolean isToUnderlineCase) {
        this.whereFilter = whereFilter;
        this.entityClass = entityClass;
        this.isToUnderlineCase = isToUnderlineCase;
    }

    /**
     * 创建Lambda条件构建器
     *
     * @param <T> 实体类型
     * @return Lambda条件构建器实例
     */
    public static <T> GirAdvWhereLambdaFilter<T> of(Class<T> entityClass) {
        return new GirAdvWhereLambdaFilter<>(entityClass, true);
    }

    public static <T> GirAdvWhereLambdaFilter<T> of(Class<T> entityClass, boolean isToUnderlineCase) {
        return new GirAdvWhereLambdaFilter<>(entityClass, isToUnderlineCase);
    }

    /**
     * 从现有WhereFilter创建Lambda条件构建器
     *
     * @param whereFilter 现有条件过滤器
     * @param <T>         实体类型
     * @return Lambda条件构建器实例
     */
    public static <T> GirAdvWhereLambdaFilter<T> of(GirAdvWhereFilter whereFilter, Class<T> entityClass) {
        return new GirAdvWhereLambdaFilter<>(whereFilter, entityClass, true);
    }

    public static <T> GirAdvWhereLambdaFilter<T> of(GirAdvWhereFilter whereFilter, Class<T> entityClass, boolean isToUnderlineCase) {
        return new GirAdvWhereLambdaFilter<>(whereFilter, entityClass, isToUnderlineCase);
    }

    // ==================== 连接符设置 ====================

    /**
     * 设置下一个条件使用 AND 连接（默认行为）
     */
    public GirAdvWhereLambdaFilter<T> and() {
        whereFilter.and();
        return this;
    }

    /**
     * 设置下一个条件使用 OR 连接
     */
    public GirAdvWhereLambdaFilter<T> or() {
        whereFilter.or();
        return this;
    }

    // ==================== 基础条件方法 ====================

    /**
     * 等于 =
     */
    public GirAdvWhereLambdaFilter<T> eq(Function<T, ?> column, Object value) {
        whereFilter.eq(getColumnName(column, isToUnderlineCase), value);
        return this;
    }

    /**
     * 不等于 !=
     */
    public GirAdvWhereLambdaFilter<T> ne(Function<T, ?> column, Object value) {
        whereFilter.ne(getColumnName(column, isToUnderlineCase), value);
        return this;
    }

    /**
     * 大于 >
     */
    public GirAdvWhereLambdaFilter<T> gt(Function<T, ?> column, Object value) {
        whereFilter.gt(getColumnName(column, isToUnderlineCase), value);
        return this;
    }

    /**
     * 大于等于 >=
     */
    public GirAdvWhereLambdaFilter<T> ge(Function<T, ?> column, Object value) {
        whereFilter.ge(getColumnName(column, isToUnderlineCase), value);
        return this;
    }

    /**
     * 小于 <
     */
    public GirAdvWhereLambdaFilter<T> lt(Function<T, ?> column, Object value) {
        whereFilter.lt(getColumnName(column, isToUnderlineCase), value);
        return this;
    }

    /**
     * 小于等于 <=
     */
    public GirAdvWhereLambdaFilter<T> le(Function<T, ?> column, Object value) {
        whereFilter.le(getColumnName(column, isToUnderlineCase), value);
        return this;
    }

    /**
     * IN条件
     */
    public GirAdvWhereLambdaFilter<T> in(Function<T, ?> column, Collection<?> values) {
        whereFilter.in(getColumnName(column, isToUnderlineCase), values);
        return this;
    }

    /**
     * IN条件（数组）
     */
    public GirAdvWhereLambdaFilter<T> in(Function<T, ?> column, Object[] values) {
        whereFilter.in(getColumnName(column, isToUnderlineCase), values);
        return this;
    }

    /**
     * NOT IN条件
     */
    public GirAdvWhereLambdaFilter<T> notIn(Function<T, ?> column, Collection<?> values) {
        whereFilter.notIn(getColumnName(column, isToUnderlineCase), values);
        return this;
    }

    /**
     * LIKE条件（全模糊 %value%）
     */
    public GirAdvWhereLambdaFilter<T> like(Function<T, ?> column, String value) {
        whereFilter.like(getColumnName(column, isToUnderlineCase), value);
        return this;
    }

    /**
     * 左模糊匹配 value%
     */
    public GirAdvWhereLambdaFilter<T> likeLeft(Function<T, ?> column, String value) {
        whereFilter.likeLeft(getColumnName(column, isToUnderlineCase), value);
        return this;
    }

    /**
     * 右模糊匹配 %value
     */
    public GirAdvWhereLambdaFilter<T> likeRight(Function<T, ?> column, String value) {
        whereFilter.likeRight(getColumnName(column, isToUnderlineCase), value);
        return this;
    }

    /**
     * BETWEEN条件
     */
    public GirAdvWhereLambdaFilter<T> between(Function<T, ?> column, Object start, Object end) {
        whereFilter.between(getColumnName(column, isToUnderlineCase), start, end);
        return this;
    }

    /**
     * IS NULL条件
     */
    public GirAdvWhereLambdaFilter<T> isNull(Function<T, ?> column) {
        whereFilter.isNull(getColumnName(column, isToUnderlineCase));
        return this;
    }

    /**
     * IS NOT NULL条件
     */
    public GirAdvWhereLambdaFilter<T> isNotNull(Function<T, ?> column) {
        whereFilter.isNotNull(getColumnName(column, isToUnderlineCase));
        return this;
    }

    // ==================== 条件组 ====================

    /**
     * AND条件组
     * <p>组内的条件将作为一个整体与其他条件进行AND连接</p>
     * <pre>
     * wrapper.and(w -> w
     *     .eq(User::getStatus, 1)
     *     .or()
     *     .eq(User::getStatus, 2)
     * )
     * </pre>
     */
    public GirAdvWhereLambdaFilter<T> and(Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        GirAdvWhereLambdaFilter<T> nestedWrapper = new GirAdvWhereLambdaFilter<>(entityClass, isToUnderlineCase);
        consumer.accept(nestedWrapper);
        whereFilter.group(group -> {
            copyConditionsToGroup(nestedWrapper.getWhereFilter(), group);
        });
        return this;
    }

    /**
     * OR条件组
     * <p>组内的条件将作为一个整体与其他条件进行OR连接</p>
     */
    public GirAdvWhereLambdaFilter<T> or(Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        GirAdvWhereLambdaFilter<T> nestedWrapper = new GirAdvWhereLambdaFilter<>(entityClass, isToUnderlineCase);
        consumer.accept(nestedWrapper);
        whereFilter.or().group(group -> {
            copyConditionsToGroup(nestedWrapper.getWhereFilter(), group);
        });
        return this;
    }

    /**
     * NOT条件组
     */
    public GirAdvWhereLambdaFilter<T> not(Consumer<GirAdvWhereLambdaFilter<T>> consumer) {
        GirAdvWhereLambdaFilter<T> nestedWrapper = new GirAdvWhereLambdaFilter<>(entityClass, isToUnderlineCase);
        consumer.accept(nestedWrapper);
        whereFilter.notGroup(group -> {
            copyConditionsToGroup(nestedWrapper.getWhereFilter(), group);
        });
        return this;
    }

    // ==================== SQL表达式 ====================

    /**
     * SQL表达式条件
     *
     * @param sqlExpr  SQL表达式（如 "YEAR(create_time)"）
     * @param operator 操作符
     * @param value    值
     */
    public GirAdvWhereLambdaFilter<T> expr(String sqlExpr, AdvOperatorEnums operator, Object value) {
        whereFilter.expr(sqlExpr, operator, value);
        return this;
    }

    /**
     * SQL表达式等值条件
     */
    public GirAdvWhereLambdaFilter<T> exprEq(String sqlExpr, Object value) {
        whereFilter.exprEq(sqlExpr, value);
        return this;
    }

    /**
     * SQL表达式大于条件
     */
    public GirAdvWhereLambdaFilter<T> exprGt(String sqlExpr, Object value) {
        whereFilter.exprGt(sqlExpr, value);
        return this;
    }

    /**
     * SQL表达式LIKE条件
     */
    public GirAdvWhereLambdaFilter<T> exprLike(String sqlExpr, String value) {
        whereFilter.exprLike(sqlExpr, value);
        return this;
    }

    // ==================== 工具方法 ====================

    /**
     * 是否包含条件
     */
    public boolean hasExpression() {
        return whereFilter.hasExpression();
    }

    /**
     * 获取原始WhereFilter
     */
    public GirAdvWhereFilter toWhereFilter() {
        return whereFilter;
    }

    /**
     * 清空所有条件
     */
    public GirAdvWhereLambdaFilter<T> clear() {
        // 由于GirAdvWhereFilter没有clear方法，这里重新创建
        return new GirAdvWhereLambdaFilter<>(entityClass, isToUnderlineCase);
    }


    /**
     * 复制条件到条件组构建器
     */
    private void copyConditionsToGroup(GirAdvWhereFilter source, GirAdvWhereFilter.ConditionGroupBuilder target) {
        // 直接访问GirAdvWhereFilter的内部结构，通过反射或添加公共方法
        // 这里假设GirAdvWhereFilter提供了getEntries方法
        try {
            // 通过反射获取entries列表
            java.lang.reflect.Field entriesField = GirAdvWhereFilter.class.getDeclaredField("entries");
            entriesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> entries = (List<Object>) entriesField.get(source);

            for (Object entry : entries) {
                // 获取entry中的connector和expression
                java.lang.reflect.Field connectorField = entry.getClass().getDeclaredField("connector");
                java.lang.reflect.Field expressionField = entry.getClass().getDeclaredField("expression");
                connectorField.setAccessible(true);
                expressionField.setAccessible(true);

                AdvLogicOperatorEnums connector = (AdvLogicOperatorEnums) connectorField.get(entry);
                GirAdvWhereFilter.ConditionExpression expression = (GirAdvWhereFilter.ConditionExpression) expressionField.get(entry);

                // 设置连接符
                if (connector == AdvLogicOperatorEnums.AND) {
                    target.and();
                } else if (connector == AdvLogicOperatorEnums.OR) {
                    target.or();
                }

                // 添加表达式到目标组
                addExpressionToGroup(target, expression);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy conditions to group", e);
        }
    }

    /**
     * 添加表达式到条件组
     */
    private void addExpressionToGroup(GirAdvWhereFilter.ConditionGroupBuilder target,
                                      GirAdvWhereFilter.ConditionExpression expression) {
        if (expression.isLeaf()) {
            // 叶子节点：具体条件
            if (expression.isExpression()) {
                target.expr(expression.getColumn(), expression.getOperator(), expression.getValue());
            } else {
                // 根据操作符调用对应方法
                addLeafCondition(target, expression);
            }
        } else {
            // 逻辑节点：嵌套组
            target.group(group -> {
                for (GirAdvWhereFilter.ConditionExpression child : expression.getChildren()) {
                    addExpressionToGroup(group, child);
                }
            });
        }
    }

    /**
     * 添加叶子条件到目标组
     */
    private void addLeafCondition(GirAdvWhereFilter.ConditionGroupBuilder target,
                                  GirAdvWhereFilter.ConditionExpression expression) {
        String column = expression.getColumn();
        AdvOperatorEnums operator = expression.getOperator();
        Object value = expression.getValue();

        switch (operator) {
            case 等于:
                target.eq(column, value);
                break;
            case 不等于:
                target.ne(column, value);
                break;
            case 大于:
                target.gt(column, value);
                break;
            case 大于等于:
                target.ge(column, value);
                break;
            case 小于:
                target.lt(column, value);
                break;
            case 小于等于:
                target.le(column, value);
                break;
            case IN:
                target.in(column, (Collection<?>) value);
                break;
            case NOT_IN:
                // ConditionGroupBuilder 没有 notIn 方法，需要手动实现
                target.and().expr(column, operator, value);
                break;
            case LIKE_ALL:
                target.like(column, (String) value);
                break;
            case LIKE_LEFT:
                target.likeLeft(column, (String) value);
                break;
            case LIKE_RIGHT:
                target.likeRight(column, (String) value);
                break;
            case BETWEEN:
                Object[] range = (Object[]) value;
                target.between(column, range[0], range[1]);
                break;
            case IS_NULL:
                target.isNull(column);
                break;
            case IS_NOT_NULL:
                target.isNotNull(column);
                break;
            default:
                target.and().expr(column, operator, value);
                break;
        }
    }


}
