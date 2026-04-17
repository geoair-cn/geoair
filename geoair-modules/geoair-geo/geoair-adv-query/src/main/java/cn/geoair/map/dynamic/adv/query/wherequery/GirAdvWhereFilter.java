package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.map.dynamic.adv.query.enums.AdvLogicOperatorEnums;
import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import lombok.Getter;

import java.io.Serializable;
import java.util.*;

/**
 * WHERE条件参数构建器
 * <p>支持复杂AND/OR嵌套条件和各种操作符</p>
 * <p>核心特性：保持条件的添加顺序，每个条件可以有自己的连接符</p>
 *
 * @author 张俊
 * @date Created in 2026/4/16 10:18
 */
@Getter
public class GirAdvWhereFilter   implements Serializable {

    // 条件条目列表（保持添加顺序，每个条件有自己的连接符）
    private final List<ConditionEntry> entries = new ArrayList<>();

    // 当前连接符（用于下一个添加的条件）
    private AdvLogicOperatorEnums currentConnector = AdvLogicOperatorEnums.AND;

    public static GirAdvWhereFilter of() {
        return new GirAdvWhereFilter();
    }

    public static GirAdvWhereFilter ofBean(Object bean) {
        ConvertOptions options = ConvertOptions.defaultOptions();
        return BeanToQueryFilterConverter.convert(bean, options);
    }

    public static GirAdvWhereFilter ofBean(Object bean, ConvertOptions convertOptions) {
        return BeanToQueryFilterConverter.convert(bean, convertOptions);
    }

    // ==================== 连接符设置 ====================

    /**
     * 设置下一个条件使用 AND 连接（默认行为）
     */
    public GirAdvWhereFilter and() {
        this.currentConnector = AdvLogicOperatorEnums.AND;
        return this;
    }

    /**
     * 设置下一个条件使用 OR 连接
     */
    public GirAdvWhereFilter or() {
        this.currentConnector = AdvLogicOperatorEnums.OR;
        return this;
    }

    // ==================== 简单条件 ====================

    /**
     * 添加条件（自动处理NULL值）
     */
    public GirAdvWhereFilter addCondition(String column, AdvOperatorEnums operator, Object value) {
        if (value == null && !operator.isNullCheck()) {
            return this;
        }
        ConditionExpression expr = new ConditionExpression(column, operator, value);
        addEntry(expr);
        return this;
    }

    private void addEntry(ConditionExpression expr) {
        entries.add(new ConditionEntry(currentConnector, expr));
        currentConnector = AdvLogicOperatorEnums.AND;
    }

    /**
     * 等值条件 =
     */
    public GirAdvWhereFilter eq(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.等于, value);
    }

    /**
     * 不等条件 !=
     */
    public GirAdvWhereFilter ne(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.不等于, value);
    }

    /**
     * 大于条件 >
     */
    public GirAdvWhereFilter gt(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.大于, value);
    }

    /**
     * 大于等于条件 >=
     */
    public GirAdvWhereFilter ge(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.大于等于, value);
    }

    /**
     * 小于条件 <
     */
    public GirAdvWhereFilter lt(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.小于, value);
    }

    /**
     * 小于等于条件 <=
     */
    public GirAdvWhereFilter le(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.小于等于, value);
    }

    /**
     * IN条件
     */
    public GirAdvWhereFilter in(String column, Collection<?> values) {
        return addCondition(column, AdvOperatorEnums.IN, values);
    }

    /**
     * IN条件（数组）
     */
    public GirAdvWhereFilter in(String column, Object[] values) {
        return addCondition(column, AdvOperatorEnums.IN, Arrays.asList(values));
    }

    /**
     * NOT IN条件
     */
    public GirAdvWhereFilter notIn(String column, Collection<?> values) {
        return addCondition(column, AdvOperatorEnums.NOT_IN, values);
    }

    /**
     * LIKE条件（全模糊 %value%）
     */
    public GirAdvWhereFilter like(String column, String value) {
        return addCondition(column, AdvOperatorEnums.LIKE_ALL, value);
    }

    /**
     * 左模糊匹配 value%
     */
    public GirAdvWhereFilter likeLeft(String column, String value) {
        return addCondition(column, AdvOperatorEnums.LIKE_LEFT, value);
    }

    /**
     * 右模糊匹配 %value
     */
    public GirAdvWhereFilter likeRight(String column, String value) {
        return addCondition(column, AdvOperatorEnums.LIKE_RIGHT, value);
    }

    /**
     * BETWEEN条件
     */
    public GirAdvWhereFilter between(String column, Object start, Object end) {
        return addCondition(column, AdvOperatorEnums.BETWEEN, new Object[]{start, end});
    }

    /**
     * IS NULL条件
     */
    public GirAdvWhereFilter isNull(String column) {
        return addCondition(column, AdvOperatorEnums.IS_NULL, null);
    }

    /**
     * IS NOT NULL条件
     */
    public GirAdvWhereFilter isNotNull(String column) {
        return addCondition(column, AdvOperatorEnums.IS_NOT_NULL, null);
    }

    // ==================== 条件组 ====================

    /**
     * 条件组
     * <p>组内的连接关系通过 .and() / .or() 来控制</p>
     * <p>
     * 使用示例：
     * <pre>
     * QueryFilter.of()
     *     .ge("外面", "")
     *     .group(group -> group
     *         .isNotNull("andGroup")
     *         .eq("andGroup", "cc")
     *     )
     *     .or()
     *     .group(group -> group
     *         .isNotNull("orGroup")
     *         .eq("orGroup", "cc")
     *         .group(sub -> sub
     *             .isNotNull("andSub")
     *             .or()
     *             .eq("andSub", "cc")
     *         )
     *     )
     * </pre>
     */
    public GirAdvWhereFilter group(GroupBuilderCallback callback) {
        ConditionGroupBuilder groupBuilder = new ConditionGroupBuilder();
        callback.build(groupBuilder);
        ConditionExpression groupExpr = groupBuilder.build();
        if (groupExpr != null) {
            addEntry(groupExpr);
        }
        return this;
    }

    /**
     * NOT条件组
     */
    public GirAdvWhereFilter notGroup(GroupBuilderCallback callback) {
        ConditionGroupBuilder groupBuilder = new ConditionGroupBuilder();
        callback.build(groupBuilder);
        ConditionExpression groupExpr = groupBuilder.build();
        if (groupExpr != null) {
            List<ConditionExpression> children = new ArrayList<>();
            children.add(groupExpr);
            ConditionExpression notExpr = new ConditionExpression(AdvLogicOperatorEnums.NOT, children);
            addEntry(notExpr);
        }
        return this;
    }

    /**
     * 是否有条件
     */
    public boolean hasExpression() {
        return !entries.isEmpty();
    }

    /**
     * 获取完整的条件表达式
     */
    public ConditionExpression getExpression() {
        return buildExpression();
    }

    /**
     * 构建条件表达式
     * 按照添加顺序，使用每个条件自己的连接符构建扁平的顺序条件树
     */
    private ConditionExpression buildExpression() {
        if (entries.isEmpty()) {
            return null;
        }

        if (entries.size() == 1) {
            return entries.get(0).getExpression();
        }

        ConditionExpression result = null;
        for (ConditionEntry entry : entries) {
            if (result == null) {
                result = entry.getExpression();
            } else {
                List<ConditionExpression> children = new ArrayList<>();
                children.add(result);
                children.add(entry.getExpression());
                // 使用 entry 自己的连接符
                result = new ConditionExpression(entry.getConnector(), children);
            }
        }
        return result;
    }

    /**
     * 添加SQL表达式条件（字段名可以是SQL表达式）
     * <p>适用于需要对字段进行函数计算或复杂表达式比较的场景</p>
     * <p>
     * 使用示例：
     * <pre>
     * // 按年份查询
     * where.expr("YEAR(create_time)", AdvOperatorEnums.等于, 2024)
     *
     * // 按计算值比较
     * where.expr("price * quantity", AdvOperatorEnums.大于, 1000)
     *
     * // 字符串拼接比较
     * where.expr("CONCAT(first_name, ' ', last_name)", AdvOperatorEnums.LIKE, "张%")
     *
     * // 聚合函数条件
     * where.expr("SUM(amount)", AdvOperatorEnums.大于, 10000)
     * </pre>
     * </p>
     *
     * @param sqlExpr  SQL表达式（如 "YEAR(create_time)"、"price * quantity"）
     * @param operator 操作符
     * @param value    值
     * @return 当前实例
     */
    public GirAdvWhereFilter expr(String sqlExpr, AdvOperatorEnums operator, Object value) {
        if (value == null && !operator.isNullCheck()) {
            return this;
        }
        ConditionExpression expr = new ConditionExpression(sqlExpr, operator, value, true);
        addEntry(expr);
        return this;
    }

    /**
     * 添加SQL表达式条件（等值查询）
     *
     * @param sqlExpr SQL表达式
     * @param value   值
     * @return 当前实例
     */
    public GirAdvWhereFilter exprEq(String sqlExpr, Object value) {
        return expr(sqlExpr, AdvOperatorEnums.等于, value);
    }

    /**
     * 添加SQL表达式条件（大于查询）
     *
     * @param sqlExpr SQL表达式
     * @param value   值
     * @return 当前实例
     */
    public GirAdvWhereFilter exprGt(String sqlExpr, Object value) {
        return expr(sqlExpr, AdvOperatorEnums.大于, value);
    }

    /**
     * 添加SQL表达式条件（大于等于查询）
     *
     * @param sqlExpr SQL表达式
     * @param value   值
     * @return 当前实例
     */
    public GirAdvWhereFilter exprGe(String sqlExpr, Object value) {
        return expr(sqlExpr, AdvOperatorEnums.大于等于, value);
    }

    /**
     * 添加SQL表达式条件（小于查询）
     *
     * @param sqlExpr SQL表达式
     * @param value   值
     * @return 当前实例
     */
    public GirAdvWhereFilter exprLt(String sqlExpr, Object value) {
        return expr(sqlExpr, AdvOperatorEnums.小于, value);
    }

    /**
     * 添加SQL表达式条件（小于等于查询）
     *
     * @param sqlExpr SQL表达式
     * @param value   值
     * @return 当前实例
     */
    public GirAdvWhereFilter exprLe(String sqlExpr, Object value) {
        return expr(sqlExpr, AdvOperatorEnums.小于等于, value);
    }

    /**
     * 添加SQL表达式条件（LIKE查询）
     *
     * @param sqlExpr SQL表达式
     * @param value   值
     * @return 当前实例
     */
    public GirAdvWhereFilter exprLike(String sqlExpr, String value) {
        return expr(sqlExpr, AdvOperatorEnums.LIKE_ALL, value);
    }

    /**
     * 添加SQL表达式条件（IN查询）
     *
     * @param sqlExpr SQL表达式
     * @param values  值集合
     * @return 当前实例
     */
    public GirAdvWhereFilter exprIn(String sqlExpr, Collection<?> values) {
        return expr(sqlExpr, AdvOperatorEnums.IN, values);
    }

    // ==================== 条件组构建器 ====================

    /**
     * 条件组构建回调接口
     */
    @FunctionalInterface
    public interface GroupBuilderCallback {
        void build(ConditionGroupBuilder builder);
    }

    /**
     * 条件组构建器
     * <p>组内可以通过 .and() / .or() 来控制条件之间的连接关系</p>
     */
    public static class ConditionGroupBuilder {
        private final List<ConditionEntry> entries = new ArrayList<>();
        private AdvLogicOperatorEnums currentConnector = AdvLogicOperatorEnums.AND;

        // ==================== 连接符设置 ====================

        /**
         * 设置下一个条件使用 AND 连接
         */
        public ConditionGroupBuilder and() {
            this.currentConnector = AdvLogicOperatorEnums.AND;
            return this;
        }

        /**
         * 设置下一个条件使用 OR 连接
         */
        public ConditionGroupBuilder or() {
            this.currentConnector = AdvLogicOperatorEnums.OR;
            return this;
        }

        private void addEntry(ConditionExpression expr) {
            entries.add(new ConditionEntry(currentConnector, expr));
            currentConnector = AdvLogicOperatorEnums.AND;
        }

        // 在 ConditionGroupBuilder 中添加
        public ConditionGroupBuilder expr(String sqlExpr, AdvOperatorEnums operator, Object value) {
            if (value == null && !operator.isNullCheck()) {
                return this;
            }
            addEntry(new ConditionExpression(sqlExpr, operator, value, true));
            return this;
        }

        public ConditionGroupBuilder exprEq(String sqlExpr, Object value) {
            return expr(sqlExpr, AdvOperatorEnums.等于, value);
        }

        public ConditionGroupBuilder exprGt(String sqlExpr, Object value) {
            return expr(sqlExpr, AdvOperatorEnums.大于, value);
        }

        public ConditionGroupBuilder exprGe(String sqlExpr, Object value) {
            return expr(sqlExpr, AdvOperatorEnums.大于等于, value);
        }

        public ConditionGroupBuilder exprLt(String sqlExpr, Object value) {
            return expr(sqlExpr, AdvOperatorEnums.小于, value);
        }

        public ConditionGroupBuilder exprLe(String sqlExpr, Object value) {
            return expr(sqlExpr, AdvOperatorEnums.小于等于, value);
        }

        public ConditionGroupBuilder exprLike(String sqlExpr, String value) {
            return expr(sqlExpr, AdvOperatorEnums.LIKE_ALL, value);
        }

        public ConditionGroupBuilder exprIn(String sqlExpr, Collection<?> values) {
            return expr(sqlExpr, AdvOperatorEnums.IN, values);
        }
        // ==================== 简单条件 ====================

        public ConditionGroupBuilder eq(String column, Object value) {
            if (value != null) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.等于, value));
            }
            return this;
        }

        public ConditionGroupBuilder ne(String column, Object value) {
            if (value != null) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.不等于, value));
            }
            return this;
        }

        public ConditionGroupBuilder gt(String column, Object value) {
            if (value != null) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.大于, value));
            }
            return this;
        }

        public ConditionGroupBuilder ge(String column, Object value) {
            if (value != null) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.大于等于, value));
            }
            return this;
        }

        public ConditionGroupBuilder lt(String column, Object value) {
            if (value != null) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.小于, value));
            }
            return this;
        }

        public ConditionGroupBuilder le(String column, Object value) {
            if (value != null) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.小于等于, value));
            }
            return this;
        }

        public ConditionGroupBuilder in(String column, Collection<?> values) {
            if (values != null && !values.isEmpty()) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.IN, values));
            }
            return this;
        }

        public ConditionGroupBuilder in(String column, Object[] values) {
            if (values != null && values.length > 0) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.IN, Arrays.asList(values)));
            }
            return this;
        }

        public ConditionGroupBuilder like(String column, String value) {
            if (value != null && !value.isEmpty()) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.LIKE_ALL, value));
            }
            return this;
        }

        public ConditionGroupBuilder likeLeft(String column, String value) {
            if (value != null && !value.isEmpty()) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.LIKE_LEFT, value));
            }
            return this;
        }

        public ConditionGroupBuilder likeRight(String column, String value) {
            if (value != null && !value.isEmpty()) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.LIKE_RIGHT, value));
            }
            return this;
        }

        public ConditionGroupBuilder between(String column, Object start, Object end) {
            if (start != null && end != null) {
                addEntry(new ConditionExpression(column, AdvOperatorEnums.BETWEEN, new Object[]{start, end}));
            }
            return this;
        }

        public ConditionGroupBuilder isNull(String column) {
            addEntry(new ConditionExpression(column, AdvOperatorEnums.IS_NULL, null));
            return this;
        }

        public ConditionGroupBuilder isNotNull(String column) {
            addEntry(new ConditionExpression(column, AdvOperatorEnums.IS_NOT_NULL, null));
            return this;
        }

        // ==================== 嵌套子组 ====================

        /**
         * 嵌套子组
         */
        public ConditionGroupBuilder group(GroupBuilderCallback callback) {
            ConditionGroupBuilder nestedBuilder = new ConditionGroupBuilder();
            callback.build(nestedBuilder);
            ConditionExpression nestedExpr = nestedBuilder.build();
            if (nestedExpr != null) {
                addEntry(nestedExpr);
            }
            return this;
        }

        /**
         * 构建条件表达式（保持扁平的顺序结构）
         */
        private ConditionExpression build() {
            if (entries.isEmpty()) {
                return null;
            }
            if (entries.size() == 1) {
                return entries.get(0).getExpression();
            }

            ConditionExpression result = null;
            for (ConditionEntry entry : entries) {
                if (result == null) {
                    result = entry.getExpression();
                } else {
                    List<ConditionExpression> children = new ArrayList<>();
                    children.add(result);
                    children.add(entry.getExpression());
                    result = new ConditionExpression(entry.getConnector(), children);
                }
            }
            return result;
        }
    }

    // ==================== 条件条目类 ====================

    /**
     * 条件条目（包含连接符和条件表达式）
     */
    @Getter
    private static class ConditionEntry {
        private final AdvLogicOperatorEnums connector;
        private final ConditionExpression expression;

        ConditionEntry(AdvLogicOperatorEnums connector, ConditionExpression expression) {
            this.connector = connector;
            this.expression = expression;
        }
    }

    // ==================== 条件表达式类 ====================

    /**
     * 条件表达式
     */
    @Getter
    public static class ConditionExpression {
        private final AdvLogicOperatorEnums logicOperator;
        private final List<ConditionExpression> children;
        private final String column;
        private final AdvOperatorEnums operator;
        private final Object value;
        private final boolean isExpression;  // 新增：是否为表达式

        /**
         * 叶子节点构造器（具体条件）
         */
        public ConditionExpression(String column, AdvOperatorEnums operator, Object value) {
            this(column, operator, value, false);
        }

        /**
         * 叶子节点构造器（支持表达式）
         *
         * @param column       列名或SQL表达式
         * @param operator     操作符
         * @param value        值
         * @param isExpression 是否为表达式（true表示column是SQL表达式，不进行引号转义）
         */
        public ConditionExpression(String column, AdvOperatorEnums operator, Object value, boolean isExpression) {
            this.logicOperator = null;
            this.children = null;
            this.column = column;
            this.operator = operator;
            this.value = value;
            this.isExpression = isExpression;
        }

        /**
         * 逻辑节点构造器（条件组合）
         */
        public ConditionExpression(AdvLogicOperatorEnums logicOperator, List<ConditionExpression> children) {
            this.logicOperator = logicOperator;
            this.children = children;
            this.column = null;
            this.operator = null;
            this.value = null;
            this.isExpression = false;
        }

        /**
         * 是否为叶子节点
         */
        public boolean isLeaf() {
            return operator != null;
        }

        /**
         * 是否为表达式（需要原样输出，不加引号）
         */
        public boolean isExpression() {
            return isExpression;
        }
    }

}
