package cn.geoair.map.dynamic.adv.query.utils;

import cn.geoair.base.data.model.annotation.GaModel;
import cn.geoair.base.data.model.annotation.GaModelField;
import cn.geoair.base.util.GutilObject;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.apo.GirSqlParam;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQuerySqlBuilder;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.hutool.core.bean.copier.BeanCopier;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;

import javax.persistence.Column;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (sqlParam == null) {
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
     * @param <T>
     * @return
     */
    public static <T> Map<String, Object> getRowData(T entity, boolean isToUnderlineCase,
                                                     boolean ignoreNullValue,
                                                     List<String> ignoreFieldNames) {
        Map<String, Object> rowData = new HashMap<>();
        Class<?> clazz = entity.getClass();
        String tableName = getTableNameByJavax(clazz);
        BeanCopier.create(entity, rowData,
                CopyOptions.create()
                        .setIgnoreNullValue(ignoreNullValue)
                        .setTransientSupport(true)
                        .setFieldNameEditor(fieldName -> {
                            if (ignoreFieldNames != null && ignoreFieldNames.contains(fieldName)) {
                                return null;
                            }
                            String columnNameByAnnotation = GirAdvSqlUtils.getColumnNameByAnnotation(clazz, fieldName);
                            if (GutilObject.isNotEmpty(columnNameByAnnotation)) {
                                return columnNameByAnnotation;
                            }
                            if (isToUnderlineCase) {
                                return StrUtil.toUnderlineCase(fieldName);
                            }
                            return fieldName;
                        })
        ).copy();

        // 可以将表名也存入返回结果
        if (tableName != null) {
            rowData.put("_tableName", tableName);
        }

        return rowData;
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
        Table table = clazz.getAnnotation (Table.class);
        if (table != null && StrUtil.isNotBlank(table.name())) {
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
            GaModelField column = field.getAnnotation(GaModelField.class);
            if (column != null && StrUtil.isNotBlank(column.columnName())) {
                return column.columnName();
            }
        } catch (NoSuchFieldException e) {

        }
        return null;
    }

    public static GirAdvQuerySqlBuilder getSqlBuilder(DialectTableNameProcessor dialectProcessor, IDataSourceGetter dataSourceGetter) {
        return new GirAdvQuerySqlBuilder(dialectProcessor, dataSourceGetter);
    }

    public static String buildWhereClause(GirAdvWhereFilter whereFilter, List<Object> params, GirAdvQuerySqlBuilder sqlBuilder) {
        return sqlBuilder.buildWhereSql(whereFilter, params);
    }

    public static String buildWhereClause(GirAdvWhereFilter whereFilter, List<Object> params, DialectTableNameProcessor dialectProcessor, IDataSourceGetter dataSourceGetter) {
        return getSqlBuilder(dialectProcessor, dataSourceGetter).buildWhereSql(whereFilter, params);
    }
}
