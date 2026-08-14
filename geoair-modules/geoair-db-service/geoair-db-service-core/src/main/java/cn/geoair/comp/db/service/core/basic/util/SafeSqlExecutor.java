package cn.geoair.comp.db.service.core.basic.util;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.data.page.support.GirPager;
import cn.geoair.comp.db.service.core.basic.dto.ApiSqlDto;
import cn.geoair.comp.db.service.core.basic.dto.SQLTaskDto;
import cn.geoair.comp.db.service.core.typehander.BlobAdvTypeHandler;
import cn.geoair.comp.db.service.core.typehander.ByteArrayBase64AdvTypeHandler;
import cn.geoair.comp.db.service.core.typehander.ClobAdvTypeHandler;
import cn.geoair.map.dynamic.adv.mybatis.SqlEngineUtil;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerContext;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandler;
import cn.geoair.map.dynamic.adv.query.typehandler.AdvTypeHandlerRegistry;
import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.convert.GirDMSpatialTran;
import cn.geoair.map.dynamic.tools.convert.GirDMTran;
import cn.geoair.map.dynamic.tools.convert.GirMysqlTran;
import cn.geoair.map.dynamic.tools.convert.GirOracleSpatialTran;
import cn.geoair.map.dynamic.tools.convert.GirOracleTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisJdbcTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisNetTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisOrgTran;
import cn.geoair.map.dynamic.tools.convert.GirPostGisTran;
import cn.hutool.core.util.StrUtil;
import org.locationtech.jts.geom.Geometry;

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

    /**
     * 构建 ds-service 专用的类型处理器注册表。
     * <p>
     * 基于 executor 的 typeHandlers（含方言几何 handler）+ ds-service 专用的 Clob/Blob/ByteArray handler。
     * 用于在 {@link #tranOneRow} 中对数据库原始值做类型转换。
     */
    private static AdvTypeHandlerRegistry buildRegistry(IAdvExecutor executor) {
        List<AdvTypeHandler<?>> handlers = new ArrayList<>(executor.getConfig().getTypeHandlers());
        // 添加 ds-service 专用的 handler（如果尚未存在）
        boolean hasClob = false, hasBlob = false, hasByteArray = false;
        for (AdvTypeHandler<?> h : handlers) {
            if (h instanceof ClobAdvTypeHandler) hasClob = true;
            else if (h instanceof BlobAdvTypeHandler) hasBlob = true;
            else if (h instanceof ByteArrayBase64AdvTypeHandler) hasByteArray = true;
        }
        if (!hasClob) handlers.add(new ClobAdvTypeHandler());
        if (!hasBlob) handlers.add(new BlobAdvTypeHandler());
        if (!hasByteArray) handlers.add(new ByteArrayBase64AdvTypeHandler());
        // Oracle 驱动存在时，注册 Oracle 特有的 Blob handler（输出 "(OracleBlob)"）
        try {
            Class.forName("oracle.sql.BLOB");
            handlers.add(new BlobAdvTypeHandler("(OracleBlob)"));
        } catch (ClassNotFoundException ignored) {
        }
        return AdvTypeHandlerRegistry.create(null, handlers);
    }

    public static List<Object> getObjects(SQLTaskDto task, Map<String, Object> sqlParam, IAdvExecutor iAdvExecutor, boolean humpIs) {
        // 构建包含 ds-service handler 的类型转换注册表
        AdvTypeHandlerRegistry registry = buildRegistry(iAdvExecutor);

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
                String pageSql = iAdvExecutor.tbBuildPageSql(sqlMeta.getSql(), giPageParam.pageNum(), pageSize, true);
                List<GirAdvOneRow> advOneRows = new ArrayList<>();
                iAdvExecutor.bSelectListStream(pageSql, SqlParamList.of(sqlMeta.getJdbcParamValues()), advOneRow -> {
                    GirAdvOneRow row = tranOneRow(advOneRow, humpIs, registry);
                    advOneRows.add(row);
                });

                GiPager<GirAdvOneRow> pager = new GirPager<>();
                giPageParam.setPageNumStartZero(true);
                pager.put(advOneRows, count, giPageParam, true);
                dataList.add(pager);
            } else {
                List<GirAdvOneRow> advOneRows = new ArrayList<>();
                iAdvExecutor.bSelectListStream(sqlMeta.getSql(), SqlParamList.of(sqlMeta.getJdbcParamValues()), new Consumer<GirAdvOneRow>() {
                    @Override
                    public void accept(GirAdvOneRow advOneRow) {
                        GirAdvOneRow row = tranOneRow(advOneRow, humpIs, registry);
                        advOneRows.add(row);
                    }
                });
                dataList.add(advOneRows);

            }
        }

        return dataList;
    }

    /**
     * 对数据库原始值做类型转换 + API 响应格式化。
     * <p>
     * 转换流程：
     * <ol>
     *   <li>通过 AdvTypeHandlerRegistry 做通用类型转换（Clob→String、Blob→占位符、byte[]→Base64）</li>
     *   <li>方言特定的几何类型转换（PGobject/MySQL binary/Oracle SDO → JTS Geometry）</li>
     *   <li>API 响应格式化（Date→字符串、Geometry→WKT）</li>
     * </ol>
     */
    private static GirAdvOneRow tranOneRow(GirAdvOneRow advOneRow, boolean humpIs, AdvTypeHandlerRegistry registry) {
        GirAdvOneRow row = GirAdvOneRow.ofByMap(new HashMap<>());
        Set<Map.Entry<String, Object>> entries = advOneRow.entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null) {
                // 1. 通用类型转换（Clob/Blob/byte[]/String 等）
                value = registry.convertForRead(value, value.getClass(), AdvTypeHandlerContext.simple(key));
                // 2. 方言特定的几何类型转换（registry 无法匹配的数据库原生几何对象）
                value = convertGeometry(value);
                // 3. API 响应格式化
                if (value instanceof Date) {
                    value = formatDate((Date) value);
                } else if (value instanceof Geometry) {
                    value = GirGeoTools.defaultInstance().getFormatOpt()
                            .jtsGeometryToWktString((Geometry) value, true);
                }
            }
            key = humpIs ? StrUtil.toCamelCase(key) : key;
            row.put(key, value);
        }
        return row;
    }

    /**
     * 方言特定的几何类型转换。
     * <p>
     * 处理 registry 无法识别的数据库原生几何对象（因为 value.getClass() 不是 Geometry.class，
     * 导致 AdvTypeHandler 的 supports() 不匹配）。
     *
     * @return 转换后的 JTS Geometry，如果不是几何类型则原样返回
     */
    private static Object convertGeometry(Object value) {
        // PostGIS — org 驱动
        if (GirPostGisTran.isOrgConvert() && GirPostGisOrgTran.isGeometry(value)) {
            return GirPostGisOrgTran.getGeometry(value);
        }
        // PostGIS — net 驱动
        if (GirPostGisTran.isNetConvert() && GirPostGisNetTran.isGeometry(value)) {
            return GirPostGisNetTran.getGeometry(value);
        }
        // PostGIS — JDBC PGobject
        if (GirPostGisTran.isPostGisAvailable() && GirPostGisJdbcTran.isPGobject(value)) {
            return GirPostGisJdbcTran.pGobjectToJts(value);
        }
        // MySQL — 二进制几何
        if (GirMysqlTran.isGeomValue(value)) {
            return GirMysqlTran.mysqlBinaryToJtsGeom(value);
        }
        // Oracle — SDO_GEOMETRY
        if (GirOracleTran.isOracleSpatialAvailable() && GirOracleSpatialTran.isSdoGeometry(value)) {
            return GirOracleSpatialTran.sdoGeometryToJtsGeom(value);
        }
        // 达梦 — DmdbStruct (Gserialized)
        if (GirDMTran.isDmDriverAvailable() && GirDMSpatialTran.isDmdbStruct(value)) {
            return GirDMSpatialTran.dmStructToJtsGeom(value);
        }
        return value;
    }


    /**
     * 日期格式化
     */
    private static String formatDate(Date date) {
        // 可根据需要修改日期格式
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }


}
