package cn.geoair.map.dynamic.adv.query.apo;

import cn.geoair.map.dynamic.adv.query.enums.AdvLogicOperatorEnums;
import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import cn.hutool.core.bean.BeanUtil;
import lombok.Getter;

import java.io.Serializable;
import java.util.*;

/**
 * WHERE条件参数构建器
 * <p>支持复杂AND/OR嵌套条件和各种操作符</p>
 *
 * @author 张俊
 * @date Created in 2026/4/16 10:18
 */
@Getter
public class QueryFilter extends LinkedHashMap<String, Object> implements Serializable {

    // 条件表达式（支持复杂嵌套）
    private ConditionExpression expression;

    public static QueryFilter of() {
        return new QueryFilter();
    }

    /**
     * 通过bean创建 SqlParamMap
     *
     * @param bean              bean对象
     * @param isToUnderlineCase 是否转换为下划线模式
     * @param ignoreNullValue   是否忽略值为空的字段
     * @return
     */
    public static QueryFilter ofBean(Object bean, boolean isToUnderlineCase, boolean ignoreNullValue) {
        QueryFilter queryFilter = new QueryFilter();
        BeanUtil.beanToMap(bean, queryFilter, isToUnderlineCase, ignoreNullValue);
        return queryFilter;
    }
    // ==================== 简单条件（默认AND连接） ====================

    /**
     * 添加条件（自动处理NULL值）
     */
    public QueryFilter addCondition(String column, AdvOperatorEnums operator, Object value) {
        if (value == null && !operator.isNullCheck()) {
            return this;
        }
        ConditionExpression expr = new ConditionExpression(column, operator, value);
        addExpression(expr);
        return this;
    }

    /**
     * 等值条件
     */
    public QueryFilter eq(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.等于, value);
    }

    /**
     * 不等条件
     */
    public QueryFilter ne(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.不等于, value);
    }

    /**
     * 大于条件
     */
    public QueryFilter gt(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.大于, value);
    }

    /**
     * 大于等于条件
     */
    public QueryFilter ge(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.大于等于, value);
    }

    /**
     * 小于条件
     */
    public QueryFilter lt(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.小于, value);
    }

    /**
     * 小于等于条件
     */
    public QueryFilter le(String column, Object value) {
        return addCondition(column, AdvOperatorEnums.小于等于, value);
    }

    /**
     * IN条件
     */
    public QueryFilter in(String column, Collection<?> values) {
        return addCondition(column, AdvOperatorEnums.IN, values);
    }

    /**
     * IN条件（数组）
     */
    public QueryFilter in(String column, Object[] values) {
        return addCondition(column, AdvOperatorEnums.IN, Arrays.asList(values));
    }

    /**
     * NOT IN条件
     */
    public QueryFilter notIn(String column, Collection<?> values) {
        return addCondition(column, AdvOperatorEnums.NOT_IN, values);
    }

    /**
     * LIKE条件（全模糊）
     */
    public QueryFilter like(String column, String value) {
        return addCondition(column, AdvOperatorEnums.LIKE_ALL, value);
    }

    /**
     * 左模糊匹配
     */
    public QueryFilter likeLeft(String column, String value) {
        return addCondition(column, AdvOperatorEnums.LIKE_LEFT, value);
    }

    /**
     * 右模糊匹配
     */
    public QueryFilter likeRight(String column, String value) {
        return addCondition(column, AdvOperatorEnums.LIKE_RIGHT, value);
    }

    /**
     * BETWEEN条件
     */
    public QueryFilter between(String column, Object start, Object end) {
        return addCondition(column, AdvOperatorEnums.BETWEEN, new Object[]{start, end});
    }

    /**
     * IS NULL条件
     */
    public QueryFilter isNull(String column) {
        return addCondition(column, AdvOperatorEnums.IS_NULL, null);
    }

    /**
     * IS NOT NULL条件
     */
    public QueryFilter isNotNull(String column) {
        return addCondition(column, AdvOperatorEnums.IS_NOT_NULL, null);
    }

    // ==================== 条件组支持 ====================

    /**
     * 开始一个AND条件组
     * 使用方式: andGroup(() -> {...})
     */
    public QueryFilter andGroup(GroupBuilderCallback callback) {
        ConditionGroupBuilder groupBuilder = new ConditionGroupBuilder(AdvLogicOperatorEnums.AND);
        callback.build(groupBuilder);
        ConditionExpression groupExpr = groupBuilder.build();
        if (groupExpr != null) {
            addExpression(groupExpr);
        }
        return this;
    }

    /**
     * 开始一个OR条件组
     * 使用方式: orGroup(() -> {...})
     */
    public QueryFilter orGroup(GroupBuilderCallback callback) {
        ConditionGroupBuilder groupBuilder = new ConditionGroupBuilder(AdvLogicOperatorEnums.OR);
        callback.build(groupBuilder);
        ConditionExpression groupExpr = groupBuilder.build();
        if (groupExpr != null) {
            addExpression(groupExpr);
        }
        return this;
    }

    /**
     * 开始一个NOT条件组
     */
    public QueryFilter notGroup(GroupBuilderCallback callback) {
        ConditionGroupBuilder groupBuilder = new ConditionGroupBuilder(AdvLogicOperatorEnums.NOT);
        callback.build(groupBuilder);
        ConditionExpression groupExpr = groupBuilder.build();
        if (groupExpr != null) {
            addExpression(groupExpr);
        }
        return this;
    }

    /**
     * 添加完整的条件表达式
     */
    public QueryFilter addExpression(ConditionExpression expression) {
        if (this.expression == null) {
            this.expression = expression;
        } else {
            // 默认用AND连接
            List<ConditionExpression> children = new ArrayList<>();
            children.add(this.expression);
            children.add(expression);
            this.expression = new ConditionExpression(AdvLogicOperatorEnums.AND, children);
        }
        return this;
    }

    /**
     * 是否有条件
     */
    public boolean hasExpression() {
        return expression != null;
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
     */
    public static class ConditionGroupBuilder {
        private final AdvLogicOperatorEnums logicOperator;
        private final List<ConditionExpression> conditions = new ArrayList<>();

        public ConditionGroupBuilder(AdvLogicOperatorEnums logicOperator) {
            this.logicOperator = logicOperator;
        }

        public ConditionGroupBuilder eq(String column, Object value) {
            if (value != null) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.等于, value));
            }
            return this;
        }

        public ConditionGroupBuilder ne(String column, Object value) {
            if (value != null) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.不等于, value));
            }
            return this;
        }

        public ConditionGroupBuilder gt(String column, Object value) {
            if (value != null) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.大于, value));
            }
            return this;
        }

        public ConditionGroupBuilder ge(String column, Object value) {
            if (value != null) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.大于等于, value));
            }
            return this;
        }

        public ConditionGroupBuilder lt(String column, Object value) {
            if (value != null) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.小于, value));
            }
            return this;
        }

        public ConditionGroupBuilder le(String column, Object value) {
            if (value != null) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.小于等于, value));
            }
            return this;
        }

        public ConditionGroupBuilder in(String column, Collection<?> values) {
            if (values != null && !values.isEmpty()) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.IN, values));
            }
            return this;
        }

        public ConditionGroupBuilder like(String column, String value) {
            if (value != null && !value.isEmpty()) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.LIKE_ALL, value));
            }
            return this;
        }

        public ConditionGroupBuilder likeLeft(String column, String value) {
            if (value != null && !value.isEmpty()) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.LIKE_LEFT, value));
            }
            return this;
        }

        public ConditionGroupBuilder likeRight(String column, String value) {
            if (value != null && !value.isEmpty()) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.LIKE_RIGHT, value));
            }
            return this;
        }

        public ConditionGroupBuilder between(String column, Object start, Object end) {
            if (start != null && end != null) {
                conditions.add(new ConditionExpression(column, AdvOperatorEnums.BETWEEN, new Object[]{start, end}));
            }
            return this;
        }

        public ConditionGroupBuilder isNull(String column) {
            conditions.add(new ConditionExpression(column, AdvOperatorEnums.IS_NULL, null));
            return this;
        }

        public ConditionGroupBuilder isNotNull(String column) {
            conditions.add(new ConditionExpression(column, AdvOperatorEnums.IS_NOT_NULL, null));
            return this;
        }

        /**
         * 嵌套AND组
         */
        public ConditionGroupBuilder andGroup(GroupBuilderCallback callback) {
            ConditionGroupBuilder nestedBuilder = new ConditionGroupBuilder(AdvLogicOperatorEnums.AND);
            callback.build(nestedBuilder);
            ConditionExpression nestedExpr = nestedBuilder.build();
            if (nestedExpr != null) {
                conditions.add(nestedExpr);
            }
            return this;
        }

        /**
         * 嵌套OR组
         */
        public ConditionGroupBuilder orGroup(GroupBuilderCallback callback) {
            ConditionGroupBuilder nestedBuilder = new ConditionGroupBuilder(AdvLogicOperatorEnums.OR);
            callback.build(nestedBuilder);
            ConditionExpression nestedExpr = nestedBuilder.build();
            if (nestedExpr != null) {
                conditions.add(nestedExpr);
            }
            return this;
        }

        /**
         * 构建条件表达式
         */
        private ConditionExpression build() {
            if (conditions.isEmpty()) {
                return null;
            }
            if (conditions.size() == 1 && logicOperator != AdvLogicOperatorEnums.NOT) {
                return conditions.get(0);
            }
            return new ConditionExpression(logicOperator, new ArrayList<>(conditions));
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

        // 叶子节点构造器
        public ConditionExpression(String column, AdvOperatorEnums operator, Object value) {
            this.logicOperator = null;
            this.children = null;
            this.column = column;
            this.operator = operator;
            this.value = value;
        }

        // 逻辑节点构造器
        public ConditionExpression(AdvLogicOperatorEnums logicOperator, List<ConditionExpression> children) {
            this.logicOperator = logicOperator;
            this.children = children;
            this.column = null;
            this.operator = null;
            this.value = null;
        }

        public boolean isLeaf() {
            return operator != null;
        }

    }

}
