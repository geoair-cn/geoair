package cn.geoair.comp.db.service.core.basic.util;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.comp.db.service.core.basic.dto.ApiSqlDto;
import cn.geoair.comp.db.service.core.basic.dto.SQLTaskDto;
import cn.geoair.comp.db.service.core.typehander.TypeHandler;
import cn.geoair.comp.db.service.core.typehander.TypeHandlerRegistry;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.hutool.core.util.StrUtil;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SQL安全执行工具类 功能：拦截危险SQL操作（新增/删除/修改/清空表/删除库等），仅允许查询操作
 */
public class SafeSqlExecutor {

    private static final Logger log = LoggerFactory.getLogger(SafeSqlExecutor.class);

    // 危险SQL模式正则表达式（不区分大小写）
    private static final Pattern DANGEROUS_SQL_PATTERN =
            Pattern.compile(
                    // 匹配DELETE/UPDATE/INSERT/TRUNCATE/DROP等危险操作
                    "^\\s*(DELETE|UPDATE|INSERT|TRUNCATE|DROP|ALTER|CREATE|RENAME|COMMIT|ROLLBACK|MERGE)\\s+",
                    Pattern.CASE_INSENSITIVE);

    // 特殊危险操作（清空表）
    private static final Pattern TRUNCATE_PATTERN =
            Pattern.compile("^\\s*TRUNCATE\\s+TABLE\\s+", Pattern.CASE_INSENSITIVE);

    // 删除数据库/表
    private static final Pattern DROP_PATTERN =
            Pattern.compile(
                    "^\\s*DROP\\s+(DATABASE|SCHEMA|TABLE|VIEW|INDEX)\\s+",
                    Pattern.CASE_INSENSITIVE);


    /**
     * 检测是否为危险SQL
     */
    private static boolean isDangerousSql(String sql) {
        // 去除注释（简单处理）
        String cleanSql = removeComments(sql);
        // 检测危险模式
        return DANGEROUS_SQL_PATTERN.matcher(cleanSql).find()
               || TRUNCATE_PATTERN.matcher(cleanSql).find()
               || DROP_PATTERN.matcher(cleanSql).find();
    }

    /**
     * 移除SQL中的注释（简单处理）
     */
    private static String removeComments(String sql) {
        // 移除/* */注释
        String noBlockComments = sql.replaceAll("/\\*.*?\\*/", " ");
        // 移除-- 注释
        return noBlockComments.replaceAll("--.*?$", " ");
    }

    public static List<Object> getObjects(SQLTaskDto task, Map<String, Object> sqlParam, IAdvExecutor iAdvExecutor, boolean humpIs) {
        List<Object> dataList = new ArrayList<>();
        List<ApiSqlDto> sqlList = task.getSqlList();
        GiPageParam giPageParam = null;
        if (task.pageIs()) {
            giPageParam = GiPageParam.of();
        }
        for (ApiSqlDto apiSql : sqlList) {
            SqlMeta sqlMeta = null;
            try {
                sqlMeta = SqlEngineUtil.getEngine().parse(apiSql.getSqlText(), sqlParam);
            } catch (RuntimeException runtimeException) {
                String message = runtimeException.getMessage();
                if (message.contains("could not found value")) {
                    String prefix = "could not found value : "; // 固定前缀
                    // 计算前缀长度，从前缀结束的位置开始截取
                    int prefixLength = prefix.length();
                    // 确保原字符串包含前缀，避免索引越界
                    String result = message.substring(prefixLength);
                    throw new RuntimeException("无法找到必填参数！" + result);
                } else {
                    throw runtimeException;
                }
            }
            if (isDangerousSql(sqlMeta.getSql())) {
                String errorMsg = "拒绝执行危险SQL操作: " + sqlMeta.getSql();
                log.error(errorMsg);
                throw new SecurityException(errorMsg);
            }
            if (task.pageIs()) {
                int pageSize = giPageParam.pageSize();
                Number number = iAdvExecutor.bSelectRecordRowCount(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()));
                Long count = number.longValue();
                String pageSql = iAdvExecutor.tbBuildPageSql(sqlMeta.getSql(), giPageParam.pageNum(), pageSize, giPageParam.isPageNumStartZero());
                List<GirAdvOneRow> girAdvOneRows = new ArrayList<>();
                iAdvExecutor.bSelectListStream(pageSql, SqlParamList.of(sqlMeta.getJdbcParamValues()), girAdvOneRow -> {
                    GirAdvOneRow row = tranOneRow(girAdvOneRow, humpIs);
                    girAdvOneRows.add(row);
                });

                GiPager<GirAdvOneRow> pager = new GirPager<>();
                giPageParam.setPageNumStartZero(true);
                pager.put(girAdvOneRows, count, giPageParam, true);
                dataList.add(pager);
            } else {
                List<GirAdvOneRow> girAdvOneRows = new ArrayList<>();
                iAdvExecutor.bSelectListStream(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()), new Consumer<GirAdvOneRow>() {
                    @Override
                    public void accept(GirAdvOneRow girAdvOneRow) {
                        GirAdvOneRow row = tranOneRow(girAdvOneRow, humpIs);
                        girAdvOneRows.add(row);
                    }
                });
                dataList.add(girAdvOneRows);

            }
        }

        return dataList;
    }

    public static GirAdvOneRow tranOneRow(GirAdvOneRow girAdvOneRow, boolean humpIs) {
        GirAdvOneRow row = GirAdvOneRow.ofByMap(new HashMap<>());
        Set<Map.Entry<String, Object>> entries = girAdvOneRow.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            TypeHandler typeHandlerByJavaType =
                    TypeHandlerRegistry.getTypeHandlerByJavaType(value);
            value = typeHandlerByJavaType.getResult(value);
            if (value instanceof Date) {
                value = formatDate((Date) value);
            }
            key = humpIs ? StrUtil.toCamelCase(key) : key;
            row.put(key, value);
        }
        return row;
    }


    /**
     * 日期格式化
     */
    private static String formatDate(Date date) {
        // 可根据需要修改日期格式
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    /**
     * 关闭结果集
     */
    private static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("关闭ResultSet失败", e);
            }
        }
    }

    /**
     * 关闭语句
     */
    private static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                log.error("关闭Statement失败", e);
            }
        }
    }

    /**
     * 批量执行安全检查（用于批量操作场景）
     */
    public static void checkBatchSqlSafety(List<String> sqlList) throws SecurityException {
        for (String sql : sqlList) {
            if (isDangerousSql(sql)) {
                String errorMsg = "批量操作中包含危险SQL: " + sql;
                log.error(errorMsg);
                throw new SecurityException(errorMsg);
            }
        }
    }
}
