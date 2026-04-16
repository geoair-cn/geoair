package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvLogicOperatorEnums;
import cn.geoair.map.dynamic.adv.query.enums.AdvOperatorEnums;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.*;

/**
 * SQL生成工具类
 * <p>根据QueryRequest生成完整的SQL语句，支持数据库方言处理</p>
 *
 * @author zhangjun
 */
public class GirAdvQuerySqlBuilder {

    private final DialectTableNameProcessor dialectProcessor;
    private final IDataSourceGetter dataSourceGetter;

    public GirAdvQuerySqlBuilder(DialectTableNameProcessor dialectProcessor, IDataSourceGetter dataSourceGetter) {
        this.dialectProcessor = dialectProcessor;
        this.dataSourceGetter = dataSourceGetter;
    }

    /**
     * 生成查询SQL
     */
    public SqlBuildResult buildSelectSql(GirAdvQueryRequest param) {
        if (param.isCustomSqlMode()) {
            return buildCustomSql(param);
        } else {
            return buildObjectModeSql(param);
        }
    }

    /**
     * 生成分页查询SQL
     */
    public SqlBuildResult buildPageSql(GirAdvQueryRequest param) {
        SqlBuildResult result = buildSelectSql(param);

        if (param.hasPagination()) {
            String sql = result.getSql();
            List<Object> params = result.getParams();

            // 使用方言处理器构建分页SQL
            String pageSql = dialectProcessor.tbBuildPageSql(sql);
            params.add(param.getPageSize());
            params.add(param.getOffset());

            return new SqlBuildResult(pageSql, params);
        }

        return result;
    }

    /**
     * 生成统计总数SQL
     */
    public SqlBuildResult buildCountSql(GirAdvQueryRequest param) {
        if (param.isCustomSqlMode()) {
            String customSql = param.getCustomSql();
            String countSql = "SELECT COUNT(*) FROM (" + customSql + ") t";
            return new SqlBuildResult(countSql, new ArrayList<>());
        } else {
            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();

            sql.append("SELECT COUNT(*) FROM ");

            // 使用方言处理器获取带Schema的表名
            String tableName = dialectProcessor.tbGetTableNameWithSchema(
                  dataSourceGetter,
                    param.getTableOrViewName()
            );
            sql.append(tableName);

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
    private SqlBuildResult buildObjectModeSql(GirAdvQueryRequest param) {
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        // SELECT - 字段名转义
        sql.append("SELECT ");
        List<String> escapedFields = new ArrayList<>();
        for (String field : param.getFieldNames()) {
            if ("*".equals(field)) {
                escapedFields.add("*");
            } else {
                escapedFields.add(dialectProcessor.tbQuoteFieldName(field));
            }
        }
        sql.append(String.join(", ", escapedFields));

        // FROM - 使用方言处理器获取带Schema的表名
        sql.append(" FROM ");
        String tableName = dialectProcessor.tbGetTableNameWithSchema(
              dataSourceGetter,
                param.getTableOrViewName()
        );
        sql.append(tableName);

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
    private SqlBuildResult buildCustomSql(GirAdvQueryRequest param) {
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
     */
    private String buildWhereClause(GirAdvQueryFilter.ConditionExpression expr, List<Object> params) {
        if (expr == null) {
            return "";
        }

        if (expr.isLeaf()) {
            return buildLeafCondition(expr, params);
        }

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

        if (subConditions.size() > 1) {
            return "(" + String.join(connector, subConditions) + ")";
        } else {
            return subConditions.get(0);
        }
    }

    /**
     * 构建叶子条件
     */
    private String buildLeafCondition(GirAdvQueryFilter.ConditionExpression expr, List<Object> params) {
        // 字段名转义
        String column = dialectProcessor.tbQuoteFieldName(expr.getColumn());
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
    private String formatLikeValue(AdvOperatorEnums operator, String value) {
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
    private String buildOrderByClause(List<OrderApo> orders) {
        if (orders == null || orders.isEmpty()) {
            return "";
        }

        List<String> orderItems = new ArrayList<>();
        for (OrderApo order : orders) {
            String field;
            if (order.isFunction()) {
                field = order.getFunction();
            } else {
                field = dialectProcessor.tbQuoteFieldName(order.getFieldName());
            }
            orderItems.add(field + " " + order.getAdvEnumsOrder().getValue());
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
