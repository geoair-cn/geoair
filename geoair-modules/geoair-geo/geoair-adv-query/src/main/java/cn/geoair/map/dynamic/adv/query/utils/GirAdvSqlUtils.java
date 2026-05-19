package cn.geoair.map.dynamic.adv.query.utils;

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
    /** 解析带参数的SQL语句，生成可执行的SQL和参数列表 */
    public static SqlMeta parseSqlWithParam(
            String dynamicSql,
            GirSqlParam sqlParam,
            DialectTableNameProcessor dialectTableNameProcessor) {
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
    public static <T> Map<String, Object> getRowData(
            T entity,
            boolean isToUnderlineCase,
            boolean ignoreNullValue,
            List<String> ignoreFieldNames) {
        Map<String, Object> rowData = new HashMap<>();
        BeanCopier.create(
                        entity,
                        rowData,
                        CopyOptions.create()
                                .setIgnoreNullValue(ignoreNullValue)
                                .setTransientSupport(true)
                                .setFieldNameEditor(
                                        key -> {
                                            if (ignoreFieldNames != null
                                                    && ignoreFieldNames.contains(key)) {
                                                return null;
                                            }
                                            if (isToUnderlineCase) {
                                                return StrUtil.toUnderlineCase(key);
                                            }
                                            return key;
                                        }))
                .copy();
        return rowData;
    }

    public static GirAdvQuerySqlBuilder getSqlBuilder(
            DialectTableNameProcessor dialectProcessor, IDataSourceGetter dataSourceGetter) {
        return new GirAdvQuerySqlBuilder(dialectProcessor, dataSourceGetter);
    }

    public static String buildWhereClause(
            GirAdvWhereFilter whereFilter, List<Object> params, GirAdvQuerySqlBuilder sqlBuilder) {
        return sqlBuilder.buildWhereSql(whereFilter, params);
    }

    public static String buildWhereClause(
            GirAdvWhereFilter whereFilter,
            List<Object> params,
            DialectTableNameProcessor dialectProcessor,
            IDataSourceGetter dataSourceGetter) {
        return getSqlBuilder(dialectProcessor, dataSourceGetter).buildWhereSql(whereFilter, params);
    }
}
