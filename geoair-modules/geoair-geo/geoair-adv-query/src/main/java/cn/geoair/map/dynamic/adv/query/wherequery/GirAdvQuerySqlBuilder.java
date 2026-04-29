package cn.geoair.map.dynamic.adv.query.wherequery;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
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
            String countSql = dialectProcessor.tbBuildAsTable("SELECT COUNT(*) FROM (" + customSql + ")", "t");
            return new SqlBuildResult(countSql, new ArrayList<>());
        } else {
            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();

            sql.append("SELECT COUNT(*) FROM ");


            boolean b = dialectProcessor.tbTableIsSqlView(param.getTableOrSqlView());
            if (b) {
                String format = dialectProcessor.tbBuildAsTable(" ( {} ) ", "{}");
                String aliasTable = StrUtil.format(format, param.getTableOrSqlView(), dialectProcessor.tbGetTempAliasTableName());
                sql.append(aliasTable);
            } else {
                String tableName = dialectProcessor.tbGetTableNameWithSchema(
                        dataSourceGetter,
                        param.getTableOrSqlView()
                );
                sql.append(tableName);
            }


            GirAdvWhereFilter where = param.getWhereOption();
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
        boolean b = dialectProcessor.tbTableIsSqlView(param.getTableOrSqlView());
        if (b) {
            String format = dialectProcessor.tbBuildAsTable(" ( {} ) ", "{}");
            String aliasTable = StrUtil.format(format, param.getTableOrSqlView(), dialectProcessor.tbGetTempAliasTableName());
            sql.append(aliasTable);
        } else {
            String tableName = dialectProcessor.tbGetTableNameWithSchema(
                    dataSourceGetter,
                    param.getTableOrSqlView()
            );
            sql.append(tableName);
        }
        // WHERE
        GirAdvWhereFilter where = param.getWhereOption();
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
    private String buildWhereClause(GirAdvWhereFilter.ConditionExpression expr, List<Object> params) {
        if (expr == null) {
            return "";
        }

        if (expr.isLeaf()) {
            return buildLeafCondition(expr, params);
        }

        if (expr.getLogicOperator() == AdvLogicOperatorEnums.NOT) {
            List<GirAdvWhereFilter.ConditionExpression> children = expr.getChildren();
            if (children != null && children.size() == 1) {
                String subCondition = buildWhereClause(children.get(0), params);
                if (StrUtil.isNotBlank(subCondition)) {
                    return "NOT (" + subCondition + ")";
                }
            }
            return "";
        }

        List<String> subConditions = new ArrayList<>();
        for (GirAdvWhereFilter.ConditionExpression child : expr.getChildren()) {
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
     * <p>支持普通字段和SQL表达式两种模式</p>
     */
    private String buildLeafCondition(GirAdvWhereFilter.ConditionExpression expr, List<Object> params) {
        // 获取列名或表达式
        String column = expr.getColumn();
        AdvOperatorEnums operator = expr.getOperator();
        Object value = expr.getValue();
        boolean isExpression = expr.isExpression();

        // 如果不是表达式，需要进行字段名转义；表达式原样输出
        String columnPart = isExpression ? column : dialectProcessor.tbQuoteFieldName(column);

        // IS NULL / IS NOT NULL
        if (operator == AdvOperatorEnums.IS_NULL) {
            return columnPart + " IS NULL";
        }
        if (operator == AdvOperatorEnums.IS_NOT_NULL) {
            return columnPart + " IS NOT NULL";
        }

        // IN / NOT IN
        if (operator == AdvOperatorEnums.IN || operator == AdvOperatorEnums.NOT_IN) {
            Collection<?> collection = (Collection<?>) value;
            if (collection == null || collection.isEmpty()) {
                return "";
            }
            String placeholders = String.join(", ", Collections.nCopies(collection.size(), "?"));
            params.addAll(collection);
            return columnPart + " " + operator.getSqlValue() + " (" + placeholders + ")";
        }

        // BETWEEN / NOT BETWEEN
        if (operator == AdvOperatorEnums.BETWEEN || operator == AdvOperatorEnums.NOT_BETWEEN) {
            Object[] between = (Object[]) value;
            if (between == null || between.length != 2) {
                return "";
            }
            params.add(between[0]);
            params.add(between[1]);
            return columnPart + " " + operator.getSqlValue() + " ? AND ?";
        }

        // EXISTS / NOT EXISTS
        if (operator == AdvOperatorEnums.EXISTS || operator == AdvOperatorEnums.NOT_EXISTS) {
            return operator.getSqlValue() + " (" + value + ")";
        }

        // LIKE / ILIKE
        if (operator.isLike()) {
            String formattedValue = formatLikeValue(operator, String.valueOf(value));
            params.add(formattedValue);
            return columnPart + " " + operator.getSqlValue() + " ?";
        }

        // 普通比较操作符
        params.add(value);
        return columnPart + " " + operator.getSqlValue() + " ?";
    }

    /**
     * 格式化LIKE值
     */
    public static String formatLikeValue(AdvOperatorEnums operator, String value) {
        if (value == null) {
            return null;
        }

        // 如果值中已经包含通配符，不再进行额外包装
        // 但需要根据实际模式进行智能处理
        switch (operator) {
            case LIKE_LEFT:
            case ILIKE_LEFT:
            case NOT_LIKE_LEFT:
                // 左模糊：期望 value%
                if (value.endsWith("%")) {
                    // 已经以%结尾，直接返回
                    return value;
                }
                return value + "%";

            case LIKE_RIGHT:
            case ILIKE_RIGHT:
            case NOT_LIKE_RIGHT:
                // 右模糊：期望 %value
                if (value.startsWith("%")) {
                    // 已经以%开头，直接返回
                    return value;
                }
                return "%" + value;

            case LIKE_ALL:
            case ILIKE_ALL:
            case NOT_LIKE_ALL:
                // 全模糊：期望 %value%
                boolean hasLeft = value.startsWith("%");
                boolean hasRight = value.endsWith("%");

                if (hasLeft && hasRight) {
                    // 已经是全模糊格式，直接返回
                    return value;
                }
                if (hasLeft) {
                    // 只有左通配符，添加右通配符
                    return value + "%";
                }
                if (hasRight) {
                    // 只有右通配符，添加左通配符
                    return "%" + value;
                }
                // 没有通配符，添加双通配符
                return "%" + value + "%";

            default:
                return value;
        }
    }

    /**
     * 判断是否已经被左模糊包装（以%结尾）
     */
    private static boolean isAlreadyWrappedLeft(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.endsWith("%");
    }

    /**
     * 判断是否已经被右模糊包装（以%开头）
     */
    private static boolean isAlreadyWrappedRight(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.startsWith("%");
    }

    /**
     * 判断是否已经被全模糊包装（以%开头且以%结尾）
     */
    private static boolean isAlreadyWrappedAll(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.startsWith("%") && value.endsWith("%");
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
        private final SqlParamList params;

        public SqlBuildResult(String sql, List<Object> params) {
            this.sql = sql;
            this.params = SqlParamList.of(params);
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
