package cn.geoair.map.dynamic.adv.query.utils;

import cn.geoair.base.Gir;
import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.anno.GirTransient;
import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanColumnMapper;
import cn.geoair.map.dynamic.adv.query.mapping.AdvBeanMappingMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvSqlComposer;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.hutool.core.bean.BeanDesc;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.PropDesc;
import cn.hutool.core.util.StrUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;


import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/16 12:51
 * @description： 查询的相关通用处理逻辑
 */
public class GirAdvSqlUtils {

    /**
     * 解析带参数的SQL语句，生成可执行的SQL和参数列表
     */
    public static SqlMeta parseSqlWithParam(String dynamicSql, GirSqlParam sqlParam, DialectTableNameProcessor dialectTableNameProcessor) {
        if (StrUtil.isEmpty(dynamicSql)) {
            throw new IllegalArgumentException("SQL语句不能为空");
        }
        if (sqlParam == null || GutilObject.isEmpty(sqlParam)) {
            return new SqlMeta(dynamicSql, new ArrayList<>());
        }
        if (sqlParam instanceof SqlParamList) {
            SqlParamList sqlParamList = (SqlParamList) sqlParam;
            return new SqlMeta(dynamicSql, sqlParamList.toList());
        }
        String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(dynamicSql);
        return SqlEngineUtil.getEngine().parse(cleanSql, (SqlParamMap) sqlParam);
    }


    /**
     * bean对象转换成 键值对的map
     *
     * @param entity
     * @param isToUnderlineCase
     * @param ignoreNullValue
     * @param ignoreFieldNames
     * @param columnMapper     用于转换 bean 字段值到 JDBC 参数值的列映射器
     * @param <T>
     * @return
     */
    public static <T> Map<String, Object> getRowData(
            T entity,
            boolean isToUnderlineCase,
            boolean ignoreNullValue,
            List<String> ignoreFieldNames,
            AdvBeanColumnMapper columnMapper) {
        return getRowData(entity, isToUnderlineCase, ignoreNullValue, true, ignoreFieldNames, columnMapper);
    }

    public static <T> Map<String, Object> getRowData(
            T entity,
            boolean isToUnderlineCase,
            boolean ignoreNullValue,
            boolean ignoreEmptyString,
            List<String> ignoreFieldNames,
            AdvBeanColumnMapper columnMapper) {
        if (entity == null) {
            return new HashMap<>();
        }
        return columnMapper.toColumnValueMap(
                entity,
                isToUnderlineCase,
                ignoreNullValue,
                ignoreEmptyString,
                ignoreFieldNames);
    }

    /**
     * 向后兼容的重载：使用默认 Registry（SPI-only，不含方言 Geometry handler）。
     * 仅用于非 Executor 上下文的工具类（如 BeanToQueryFilterConverter）。
     * Executor 内部请使用带 AdvBeanColumnMapper 参数的版本。
     */
    public static <T> Map<String, Object> getRowData(T entity, boolean isToUnderlineCase, boolean ignoreNullValue, List<String> ignoreFieldNames) {
        return getRowData(entity, isToUnderlineCase, ignoreNullValue, true, ignoreFieldNames);
    }

    /**
     * @deprecated 请使用带 AdvBeanColumnMapper 参数的重载版本
     */
    private static final AdvBeanColumnMapper DEFAULT_COLUMN_MAPPER =
            new AdvBeanColumnMapper(AdvTypeHandlerRegistry.defaultInstance());

    public static <T> Map<String, Object> getRowData(
            T entity,
            boolean isToUnderlineCase,
            boolean ignoreNullValue,
            boolean ignoreEmptyString,
            List<String> ignoreFieldNames) {
        if (entity == null) {
            return new HashMap<>();
        }
        return DEFAULT_COLUMN_MAPPER.toColumnValueMap(
                entity,
                isToUnderlineCase,
                ignoreNullValue,
                ignoreEmptyString,
                ignoreFieldNames);
    }

    public static List<String> getIdByAnnotation(Class<?> clazz) {
        return AdvBeanMappingMeta.of(clazz).getIdPropertyNames();
    }

    public static List<String> getIdColumnNames(Class<?> clazz, boolean toUnderlineCase) {
        return AdvBeanMappingMeta.of(clazz).getIdColumnNames(toUnderlineCase);
    }

    public static String resolveColumnName(Class<?> clazz, String fieldOrColumnName, boolean toUnderlineCase) {
        return AdvBeanMappingMeta.of(clazz).resolveColumnName(fieldOrColumnName, toUnderlineCase);
    }

    public static List<String> getIgnoreFieldByAnnotation(Class<?> clazz) {
        List<String> ignores = new ArrayList<>();
        BeanDesc beanDesc = BeanUtil.getBeanDesc(clazz);
        if (GutilObject.isNotEmpty(beanDesc)) {
            Map<String, PropDesc> propMap = beanDesc.getPropMap(false);
            if (GutilObject.isNotEmpty(propMap)) {
                for (Map.Entry<String, PropDesc> propDescEntry : propMap.entrySet()) {
                    PropDesc value = propDescEntry.getValue();
                    Field field = value.getField();
                    GirTransient girTransient = field.getAnnotation(GirTransient.class);
                    if (girTransient != null) {
                        ignores.add(field.getName());
                        continue;
                    }

                    Transient aTransient = field.getAnnotation(Transient.class);
                    if (aTransient != null) {
                        ignores.add(field.getName());
                        continue;
                    }

                }
                return ignores;
            }
        }
        return ignores;
    }


    public static String getIdByJavax(Field field) {
        Id id = field.getAnnotation(Id.class);
        if (id != null) {
            return field.getName();
        }
        return null;
    }

    public static String getIdByGaModel(Field field) {
        GaModelField gaModel = field.getAnnotation(GaModelField.class);
        if (gaModel != null && gaModel.isID()) {
            return field.getName();
        }
        return null;
    }


    public static String getTableName(Class<?> clazz) {
        String tableNameByAnnotation = getTableNameByAnnotation(clazz);

        if (GutilObject.isNotEmpty(tableNameByAnnotation)) {
            return tableNameByAnnotation;
        }
        return StrUtil.lowerFirst(clazz.getSimpleName());
    }

    public static String getTableNameByAnnotation(Class<?> clazz) {
        String tableNameByJavax = getTableNameByJavax(clazz);

        if (GutilObject.isNotEmpty(tableNameByJavax)) {
            return tableNameByJavax;
        }
        String tableNameByGaModel = getTableNameByGaModel(clazz);
        if (GutilObject.isNotEmpty(tableNameByGaModel)) {
            return tableNameByGaModel;
        }
        return null;
    }


    public static String getTableNameByJavax(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && StrUtil.isNotBlank(table.name())) {
            if (StrUtil.isNotBlank(table.schema())) {
                return table.schema() + "." + table.name();
            }
            return table.name();
        }
        return null;
    }

    public static String getTableNameByGaModel(Class<?> clazz) {
        GaModel gaModel = clazz.getAnnotation(GaModel.class);
        if (gaModel != null && StrUtil.isNotBlank(gaModel.tableName())) {
            return gaModel.tableName();
        }
        return null;
    }

    public static String getColumnNameByAnnotation(Class<?> clazz, String fieldName) {
        String columnNameByJavax = GirAdvSqlUtils.getColumnNameByJavax(clazz, fieldName);
        if (GutilObject.isNotEmpty(columnNameByJavax)) {
            return columnNameByJavax;
        }
        String columnNameByGaModelField = GirAdvSqlUtils.getColumnNameByGaModelField(clazz, fieldName);
        if (GutilObject.isNotEmpty(columnNameByGaModelField)) {
            return columnNameByGaModelField;
        }
        return null;
    }


    // 获取字段对应的列名
    public static String getColumnNameByJavax(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Column column = field.getAnnotation(Column.class);
            if (column != null && StrUtil.isNotBlank(column.name())) {
                return column.name();
            }
        } catch (NoSuchFieldException e) {

        }
        return null;
    }


    public static String getColumnNameByGaModelField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            GaModelField column = field.getAnnotation(GaModelField.class);
            if (column != null && StrUtil.isNotBlank(column.columnName())) {
                return column.columnName();
            }
        } catch (NoSuchFieldException e) {

        }
        return null;
    }

    public static GirAdvSqlComposer getSqlBuilder(DialectTableNameProcessor dialectProcessor, IDataSourceGetter dataSourceGetter) {
        return new GirAdvSqlComposer(dialectProcessor, dataSourceGetter);
    }

    public static String buildWhereClause(GirAdvWhereFilter whereFilter, List<Object> params, GirAdvSqlComposer sqlBuilder) {
        return sqlBuilder.buildWhereSql(whereFilter, params);
    }

    public static String buildWhereClause(GirAdvWhereFilter whereFilter, List<Object> params, DialectTableNameProcessor dialectProcessor, IDataSourceGetter dataSourceGetter) {
        return getSqlBuilder(dialectProcessor, dataSourceGetter).buildWhereSql(whereFilter, params);
    }


    public static String buildWhereClause(Map<String, Object> whereMap, DialectTableNameProcessor dialectProcessor) {
        return whereMap.keySet().stream()
                .map(dialectProcessor::tbQuoteFieldName)
                .map(field -> StrUtil.format("{} = ?", field))
                .collect(Collectors.joining(" AND "));
    }

    public static String buildSetClause(Map<String, Object> rowData, DialectTableNameProcessor dialectProcessor) {
        return buildSetClause(rowData, dialectProcessor, null);
    }

    /**
     * 构建 SET 子句（支持 TypeHandler 占位符表达式）。
     */
    public static String buildSetClause(Map<String, Object> rowData,
                                        DialectTableNameProcessor dialectProcessor,
                                        AdvTypeHandlerRegistry registry) {
        return rowData.keySet()
                .stream()
                .map(field -> {
                    String quoted = dialectProcessor.tbQuoteFieldName(field);
                    String placeholder = "?";
                    if (registry != null) {
                        String customPh = registry.getSqlPlaceholder(rowData.get(field));
                        if (customPh != null) placeholder = customPh;
                    }
                    return StrUtil.format("{} = {}", quoted, placeholder);
                })
                .collect(Collectors.joining(","));
    }

    public static void rollbackConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                Gir.log.error("批量插入回滚失败", e);
            }
        }
    }

    public static void restoreAutoCommit(Connection connection) {
        if (connection != null) {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                Gir.log.error("恢复自动提交失败", e);
            }
        }
    }


}
