package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvLogicOperatorEnums;
import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.*;

/**
 * SQL生成工具类
 * <p>根据QueryRequest生成完整的SQL语句</p>
 *
 * @author zhangjun
 */
public class GirAdvQuerySqlBuilder {

    /**
     * 生成查询SQL
     */
    public static SqlBuildResult buildSelectSql(GirAdvQueryRequest param) {
        if (param.isCustomSqlMode()) {
            return buildCustomSql(param);
        } else {
            return buildObjectModeSql(param);
        }
    }

    /**
     * 生成分页查询SQL
     */
    public static SqlBuildResult buildPageSql(GirAdvQueryRequest param) {
        SqlBuildResult result = buildSelectSql(param);

        if (param.hasPagination()) {
            String sql = result.getSql();
            List<Object> params = result.getParams();

            String pageSql = sql + " LIMIT ? OFFSET ?";
            params.add(param.getPageSize());
            params.add(param.getOffset());

            return new SqlBuildResult(pageSql, params);
        }

        return result;
    }

    /**
     * 生成统计总数SQL
     */
    public static SqlBuildResult buildCountSql(GirAdvQueryRequest param) {
        if (param.isCustomSqlMode()) {
            String customSql = param.getCustomSql();
            String countSql = "SELECT COUNT(*) FROM (" + customSql + ") t";
            return new SqlBuildResult(countSql, new ArrayList<>());
        } else {
            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();

            sql.append("SELECT COUNT(*) FROM ");
            sql.append(param.getTableOrViewName());

            GirAdvQueryFilter where = param.getWhereOption();
            if (where != null && where.hasExpression()) {
                String whereClause = buildWhereClause(where.getExpression(), params);
                if (StrUtil.isNotBlank(whereClause)) {
                    sql.append(" WHERE ").append(whereClause);
                }
            }

            return new SqlBuildResult(sql.toString(), params);
        }
    }

    /**
     * 构建对象模式SQL
     */
    private static SqlBuildResult buildObjectModeSql(GirAdvQueryRequest param) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        // SELECT
        sql.append("SELECT ");
        sql.append(String.join(", ", param.getFieldNames()));

        // FROM
        sql.append(" FROM ").append(param.getTableOrViewName());

        // WHERE
        GirAdvQueryFilter where = param.getWhereOption();
        if (where != null && where.hasExpression()) {
            String whereClause = buildWhereClause(where.getExpression(), params);
            if (StrUtil.isNotBlank(whereClause)) {
                sql.append(" WHERE ").append(whereClause);
            }
        }

        // ORDER BY
        if (param.hasOrders()) {
            String orderByClause = buildOrderByClause(param.getOrders());
            if (StrUtil.isNotBlank(orderByClause)) {
                sql.append(" ORDER BY ").append(orderByClause);
            }
        }

        return new SqlBuildResult(sql.toString(), params);
    }

    /**
     * 构建自定义SQL模式
     */
    private static SqlBuildResult buildCustomSql(GirAdvQueryRequest param) {
        String sql = param.getCustomSql();
        List<Object> params = new ArrayList<>();

        // ORDER BY
        if (param.hasOrders()) {
            String orderByClause = buildOrderByClause(param.getOrders());
            if (StrUtil.isNotBlank(orderByClause)) {
                if (sql.toUpperCase().contains("ORDER BY")) {
                    sql = sql + ", " + orderByClause;
                } else {
                    sql = sql + " ORDER BY " + orderByClause;
                }
            }
        }

        return new SqlBuildResult(sql, params);
    }

    /**
     * 构建WHERE子句
     * <p>核心逻辑：按照条件的原始顺序和连接符生成SQL，不额外添加括号</p>
     */
    private static String buildWhereClause(GirAdvQueryFilter.ConditionExpression expr, List<Object> params) {
        if (expr == null) {
            return "";
        }

        // 叶子节点：直接生成条件
        if (expr.isLeaf()) {
            return buildLeafCondition(expr, params);
        }

        // 处理 NOT 逻辑
        if (expr.getLogicOperator() == AdvLogicOperatorEnums.NOT) {
            List<GirAdvQueryFilter.ConditionExpression> children = expr.getChildren();
            if (children != null && children.size() == 1) {
                String subCondition = buildWhereClause(children.get(0), params);
                if (StrUtil.isNotBlank(subCondition)) {
                    return "NOT (" + subCondition + ")";
                }
            }
            return "";
        }

        // 处理 AND/OR 逻辑组
        // 关键：按照条件的原始顺序生成，每个子条件保持原样，组内用连接符连接
        List<String> subConditions = new ArrayList<>();
        for (GirAdvQueryFilter.ConditionExpression child : expr.getChildren()) {
            String subSql = buildWhereClause(child, params);
            if (StrUtil.isNotBlank(subSql)) {
                subConditions.add(subSql);
            }
        }

        if (subConditions.isEmpty()) {
            return "";
        }

        String connector = expr.getLogicOperator() == AdvLogicOperatorEnums.AND ? " AND " : " OR ";

        // 根据您的期望：保持原始结构，不额外添加括号
        // 只有当组内条件数量大于1时才加括号，保证优先级正确
        if (subConditions.size() > 1) {
            return "(" + String.join(connector, subConditions) + ")";
        } else {
            return subConditions.get(0);
        }
    }

    /**
     * 构建叶子条件
     */
    private static String buildLeafCondition(GirAdvQueryFilter.ConditionExpression expr, List<Object> params) {
        String column = expr.getColumn();
        AdvOperatorEnums operator = expr.getOperator();
        Object value = expr.getValue();

        // IS NULL / IS NOT NULL
        if (operator == AdvOperatorEnums.IS_NULL) {
            return column + " IS NULL";
        }
        if (operator == AdvOperatorEnums.IS_NOT_NULL) {
            return column + " IS NOT NULL";
        }

        // IN / NOT IN
        if (operator == AdvOperatorEnums.IN || operator == AdvOperatorEnums.NOT_IN) {
            Collection<?> collection = (Collection<?>) value;
            if (collection == null || collection.isEmpty()) {
                return "";
            }
            String placeholders = String.join(", ", Collections.nCopies(collection.size(), "?"));
            params.addAll(collection);
            return column + " " + operator.getSqlValue() + " (" + placeholders + ")";
        }

        // BETWEEN / NOT BETWEEN
        if (operator == AdvOperatorEnums.BETWEEN || operator == AdvOperatorEnums.NOT_BETWEEN) {
            Object[] between = (Object[]) value;
            if (between == null || between.length != 2) {
                return "";
            }
            params.add(between[0]);
            params.add(between[1]);
            return column + " " + operator.getSqlValue() + " ? AND ?";
        }

        // EXISTS / NOT EXISTS
        if (operator == AdvOperatorEnums.EXISTS || operator == AdvOperatorEnums.NOT_EXISTS) {
            return operator.getSqlValue() + " (" + value + ")";
        }

        // LIKE / ILIKE
        if (operator.isLike()) {
            String formattedValue = formatLikeValue(operator, String.valueOf(value));
            params.add(formattedValue);
            return column + " " + operator.getSqlValue() + " ?";
        }

        // 普通比较操作符
        params.add(value);
        return column + " " + operator.getSqlValue() + " ?";
    }

    /**
     * 格式化LIKE值
     */
    private static String formatLikeValue(AdvOperatorEnums operator, String value) {
        if (value == null) {
            return null;
        }

        switch (operator) {
            case LIKE_LEFT:
            case ILIKE_LEFT:
                return value + "%";
            case LIKE_RIGHT:
            case ILIKE_RIGHT:
                return "%" + value;
            case LIKE_ALL:
            case ILIKE_ALL:
                return "%" + value + "%";
            case NOT_LIKE_LEFT:
                return value + "%";
            case NOT_LIKE_RIGHT:
                return "%" + value;
            case NOT_LIKE_ALL:
                return "%" + value + "%";
            default:
                return value;
        }
    }

    /**
     * 构建ORDER BY子句
     */
    private static String buildOrderByClause(List<OrderApo> orders) {
        if (orders == null || orders.isEmpty()) {
            return "";
        }

        List<String> orderItems = new ArrayList<>();
        for (OrderApo order : orders) {
            if (order.isFunction()) {
                orderItems.add(order.getFunction() + " " + order.getAdvEnumsOrder().getValue());
            } else {
                orderItems.add(order.getFieldName() + " " + order.getAdvEnumsOrder().getValue());
            }
        }

        return String.join(", ", orderItems);
    }

    /**
     * SQL构建结果
     */
    @Getter
    public static class SqlBuildResult {
        private final String sql;
        private final List<Object> params;

        public SqlBuildResult(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }

        public String getPreparedSql() {
            return sql;
        }

        public String getExecutableSql() {
            String executableSql = sql;
            for (Object param : params) {
                String valueStr = formatParamValue(param);
                executableSql = executableSql.replaceFirst("\\?", valueStr);
            }
            return executableSql;
        }

        private String formatParamValue(Object param) {
            if (param == null) {
                return "NULL";
            }
            if (param instanceof String) {
                return "'" + param.toString().replace("'", "''") + "'";
            }
            if (param instanceof Date) {
                return "'" + param.toString() + "'";
            }
            return param.toString();
        }

        @Override
        public String toString() {
            return "SqlBuildResult{sql='" + sql + "', params=" + params + "}";
        }
    }
}
