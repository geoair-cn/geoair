package cn.geoair.map.dynamic.mvt.exec;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.map.dynamic.adv.query.IAdvExecutor;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsOrder;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.mvt.consumer.VectorTileBuilderConsumer;
import cn.geoair.map.dynamic.mvt.dto.TileExecutorConfig;
import cn.geoair.map.dynamic.mvt.dto.TileGlobalConfig;
import cn.geoair.map.dynamic.mvt.dto.TileRequestParams;
import cn.geoair.map.dynamic.mvt.tools.AdvMvtDensityUtils;
import cn.geoair.map.dynamic.mvt.tools.param.TileExecParams;

import cn.geoair.map.dynamic.tools.GirGeoTools;
import cn.geoair.map.dynamic.tools.page.PageActuator;
import cn.geoair.map.dynamic.tools.page.PageConditionDef;
import cn.geoair.map.dynamic.tools.page.PageConfig;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;

/**
 * 矢量瓦片查询抽象基类
 * <p>
 * 封装了瓦片查询的公共逻辑（SQL执行、分页、密度合并、几何变换），
 * 子类只需实现各数据库方言的 SQL 生成方法。
 */
public abstract class AbstractVectorTileExecutor extends AbstractITileExecutor {
    public static GiLogger log = GirLoggerFactory.getLogger();

    List<String> keepFieldList = new ArrayList<>();

    static final String geomBox = "geo_box";

    /** 空间字段的别名 */
    static final String GEOM_FIELD_ALIAS_IN_SQL = "geo_root";

    /**
     * 获取 WITH CTE 与主表的连接方式（子类覆盖以适配不同数据库）
     * <p>PG/MySQL 用逗号即可，Oracle 需 CROSS JOIN
     */
    protected String getCteJoinSeparator() {
        return ",  ";
    }

    static final String GEOM_FIELD_ALIAS_IN_TRAN = "geom";

    public AbstractVectorTileExecutor(TileRequestParams requestParams, String layerName) {
        super(requestParams, layerName);
        initKeepFieldList();
    }

    public AbstractVectorTileExecutor(
            TileRequestParams requestParams, String layerName, IAdvExecutor iAdvExecutor) {
        super(requestParams, layerName, iAdvExecutor);
        initKeepFieldList();
    }

    private void initKeepFieldList() {
        List<String> keepFieldListTemp = new ArrayList<>();
        String keepField = requestParams.getKeepFields();
        if (ObjectUtil.isNotEmpty(keepField)) {
            keepFieldListTemp = StrUtil.split(keepField, ",");
        } else {
            keepFieldListTemp =
                    requestParams.getKeepFieldList() == null
                            ? new ArrayList<>()
                            : requestParams.getKeepFieldList();
        }
        for (String field : keepFieldListTemp) {
            if (StrUtil.isEmpty(field)) {
                continue;
            }
            String trimmed = field.replace("\"", "").trim();
            int dotIndex = trimmed.lastIndexOf(".");
            if (dotIndex != -1 && dotIndex < trimmed.length() - 1) {
                trimmed = trimmed.substring(dotIndex + 1);
            }
            if (StrUtil.isNotEmpty(trimmed)) {
                keepFieldList.add(trimmed);
            }
        }
    }

    @Override
    public TileGlobalConfig getTileGlobalConfig() {
        TileGlobalConfig tileGlobalConfig = new TileGlobalConfig();
        tileGlobalConfig
                .setTileRequestParams(requestParams.copy())
                .setTileExecConfig(this.tileExecutorConfig.copy())
                .setTileExecParams(this.tileExecParams.copy())
                .setLayerName(layerName)
                .setVersion(2)
                .setCustomVariable(customVariable);
        return tileGlobalConfig;
    }

    /**
     * 获取缓冲区边界框 SQL 表达式（数据库方言相关）
     */
    protected abstract String getBufferBboxSqlFunction(TileExecParams tileExecParams);

    /**
     * 获取几何字段的导出表达式（统一返回 Base64(WKB) 或 WKT）
     */
    protected abstract String getGeomExportExpr(String tableAlias, String geomFieldName);

    /**
     * 获取空间相交判断的 WHERE 条件表达式
     */
    protected abstract String getIntersectsWhereExpr(String geomFieldExpr, String withQueryAlias);

    /**
     * 返回几何编码格式："wkb_base64" 或 "wkt"
     */
    protected abstract String getGeomEncodingFormat();

    /**
     * 对 SRID 为 0 时的特殊处理表达式（PostGIS 需 SetSRID，Oracle 不需）
     */
    protected String getGeomFieldWithSrid(String tableAlias, String geomFieldName, String srid) {
        if (ObjectUtil.equals(srid, "0")) {
            return tableAlias + "." + geomFieldName;
        }
        return tableAlias + "." + geomFieldName;
    }

    public String getExecSql(TileExecParams tileExecParams) {
        String excuteSQL = "";
        String tableAlias = "root_tt";
        String withQueryAlias = "with_as";
        // 构建表名
        String tbNameOrSql = requestParams.getTbNameOrSql();
        String schema = requestParams.getSchemaName();
        boolean isSqlView = iAdvExecutor.tbTableIsSqlView(tbNameOrSql);
        String finalTbName = null;
        if (isSqlView) {
            String tbRemoveSqlSpaces = iAdvExecutor.tbRemoveSqlSpaces(tbNameOrSql);
            // Oracle 子查询后面不能用 AS，oracleDialectTableNameUtil 里面处理了
            finalTbName = iAdvExecutor.tbBuildAsTable(
                    StrUtil.format("({})", tbRemoveSqlSpaces), tableAlias);
        } else {
            String tbGetTableNameWithSchema =
                    iAdvExecutor.tbGetTableNameWithSchema(tbNameOrSql, schema);
            finalTbName = iAdvExecutor.tbBuildAsTable(
                    StrUtil.format("( select * from  {})", tbGetTableNameWithSchema), tableAlias);
        }
        // 校验必要参数
        Envelope dataExtent = tileExecParams.getDataExtent();
        if (ObjectUtils.anyNull(
                dataExtent,
                requestParams.getSrid(),
                requestParams.getGeomFieldName(),
                requestParams.getDsId())) {
            log.error("缺少必要参数，无法查询瓦片数据，当前参数：{}", requestParams.toString());
            return null;
        }
        String polygonFunction = getBufferBboxSqlFunction(tileExecParams);
        String geomFieldName = requestParams.getGeomFieldName();
        String srid = requestParams.getSrid();
        // 构建geom字段表达式（添加表别名前缀，处理 SRID=0 的情况）
        String geomField = getGeomFieldWithSrid(tableAlias, geomFieldName, srid);

        StringBuilder withSQL = new StringBuilder();
        withSQL.append("\n")
                .append("WITH   ")
                .append(withQueryAlias)
                .append(" AS ( select  ")
                .append(polygonFunction)
                .append("   AS  ")
                .append(geomBox)
                .append(") ");
        StringBuilder rootSql = new StringBuilder();
        rootSql.append("SELECT ");
        if (ObjectUtil.isEmpty(keepFieldList) && !requestParams.isKeepFieldAll()) {
            rootSql.append(getGeomExportExpr(tableAlias, geomFieldName))
                    .append(" as  ")
                    .append(GEOM_FIELD_ALIAS_IN_SQL);
        } else if (requestParams.isKeepFieldAll()) {
            rootSql.append(tableAlias)
                    .append(".*")
                    .append(", ")
                    .append(getGeomExportExpr(tableAlias, geomFieldName))
                    .append(" as  ")
                    .append(GEOM_FIELD_ALIAS_IN_SQL);
        } else {
            List<String> aliasFields = new ArrayList<>();
            for (String field : keepFieldList) {
                if (StrUtil.isNotBlank(field)) {
                    aliasFields.add(tableAlias + "." + StrUtil.wrap(field, "\""));
                }
            }
            rootSql.append(String.join(", ", aliasFields))
                    .append(", ")
                    .append(getGeomExportExpr(tableAlias, geomFieldName))
                    .append(" as  ")
                    .append(GEOM_FIELD_ALIAS_IN_SQL);
        }

        rootSql.append(" FROM ")
                .append(finalTbName)
                .append(getCteJoinSeparator())
                .append(withQueryAlias)
                .append(" ")
                .append(withQueryAlias)
                .append(" WHERE ")
                .append(getIntersectsWhereExpr(geomField, withQueryAlias))
                .append(" AND ")
                .append(geomField)
                .append(" IS NOT NULL  ");

            excuteSQL = withSQL.toString() + "\n" + rootSql;
        return excuteSQL;
    }

    @Override
    public void getRecordByStream(VectorTileBuilderConsumer vectorTileBuilder) {
        String execSql = this.tileExecParams.getExecSql();
        if (StrUtil.isEmpty(execSql)) {
            execSql = getExecSql(this.tileExecParams);
        }
        if (execSql == null) {
            return;
        }

        doExecSQL(vectorTileBuilder, execSql);
    }

    protected void doExecSQL(VectorTileBuilderConsumer vectorTileBuilder, String excuteSQL) {
        TileExecutorConfig.LowLevelOptStrategy lowLevelOptStrategy =
                tileExecutorConfig.getLowLevelOptStrategy();
        int pagingStartLevel = tileExecutorConfig.getPagingStartLevel();
        int limitStartLevel = tileExecutorConfig.getLimitStartLevel();
        TileExecutorConfig.DensityOptStrategy densityOptStrategy =
                tileExecutorConfig.getDensityOptStrategy();
        final int DEFAULT_DENSITY_COUNT = 3000;
        List<GirAdvOneRow> girAdvOneRows = new ArrayList<>();
        boolean needTrans = true;
        try {
            if (lowLevelOptStrategy.equals(TileExecutorConfig.LowLevelOptStrategy.PAGING)
                    && zoom <= pagingStartLevel) {
                Long totalCount = iAdvExecutor.pCount(excuteSQL);
                if (totalCount == 0) {
                    return;
                }
                int pagingThreshold = tileExecutorConfig.getPagingThreshold();

                if (totalCount > pagingThreshold) {
                    girAdvOneRows =
                            parallelPageQueryReturnList(
                                    excuteSQL,
                                    totalCount,
                                    GEOM_FIELD_ALIAS_IN_SQL,
                                    this::featuresTransform);
                    needTrans = false;
                } else {
                    girAdvOneRows = iAdvExecutor.bSelectList(excuteSQL);
                }
            } else if (lowLevelOptStrategy.equals(TileExecutorConfig.LowLevelOptStrategy.LIMIT)
                    && zoom <= limitStartLevel) {
                excuteSQL = iAdvExecutor.tbRemoveSqlSpaces(excuteSQL);
                String template = "select * from ( {tableName} ) as limit_table ";
                String sql = StrUtil.replaceFirst(template, "{tableName}", excuteSQL);
                Long maxLimitCount = tileExecutorConfig.getMaxLimitCount();
                girAdvOneRows = iAdvExecutor.bSelectList(sql + "limit " + maxLimitCount);
            } else {
                girAdvOneRows = iAdvExecutor.bSelectList(excuteSQL);
            }
        } catch (Exception e) {
            log.error("获取瓦片数据列表失败", e);
            throw new RuntimeException(e);
        }
        processGirAdvOneRowList(
                girAdvOneRows,
                needTrans,
                densityOptStrategy,
                vectorTileBuilder,
                DEFAULT_DENSITY_COUNT,
                gridSrid);
    }

    private void processGirAdvOneRowList(
            List<GirAdvOneRow> girAdvOneRows,
            boolean needTransform,
            TileExecutorConfig.DensityOptStrategy densityOptStrategy,
            Consumer<GirAdvOneRow> vectorTileBuilder,
            int limit,
            int gridSrid) {
        if (CollectionUtil.isEmpty(girAdvOneRows)) {
            return;
        }
        List<GirAdvOneRow> transList = girAdvOneRows;
        if (needTransform) {
            transList =
                    girAdvOneRows
                            .stream()
                            .peek(this::featuresTransform)
                            .collect(Collectors.toList());
        }
        if (TileExecutorConfig.DensityOptStrategy.DENSITY_MERGING.equals(densityOptStrategy)) {
            List<GirAdvOneRow> girAdvOneRowsCoalesce =
                    AdvMvtDensityUtils.doCoalesceBySpatialDensity(
                            transList,
                            Math.toIntExact(limit),
                            GEOM_FIELD_ALIAS_IN_TRAN,
                            GEOM_FIELD_ALIAS_IN_TRAN,
                            gridSrid);
            girAdvOneRowsCoalesce.forEach(vectorTileBuilder);
        } else {
            transList.forEach(vectorTileBuilder);
        }
    }

    public void featuresTransform(GirAdvOneRow oneRow) {
        try {
            boolean geoIsWkt = "wkt".equals(getGeomEncodingFormat());
            String geomEncodeStr = oneRow.getStr(GEOM_FIELD_ALIAS_IN_SQL);
            if (StrUtil.isEmpty(geomEncodeStr)) {
                oneRow.remove(GEOM_FIELD_ALIAS_IN_SQL);
                return;
            }
            Geometry geometry;
            if (geoIsWkt) {
                // Oracle WKT 路径
                WKTReader wktReader = new WKTReader();
                geometry = wktReader.read(geomEncodeStr);
            } else {
                // 默认 WKB Base64 路径 (PostGIS, MySQL)
                byte[] decode = Base64.decode(geomEncodeStr);
                WKBReader wkbReader = new WKBReader();
                geometry = wkbReader.read(decode);
            }
            if (!ObjectUtil.equals(gridSrid, sourceDataSrid)) {
                geometry = GirGeoTools.defaultInstance().getSridOpt().convert(geometry, sourceDataSrid, gridSrid);
            }
            Geometry finalGeometry = geometry;
            if (geometry != null) {
                try {
                    Geometry gridExtentBufferBoxGeom =
                            this.tileExecParams.getGridExtentBufferBoxGeom();
                    finalGeometry = gridExtentBufferBoxGeom.intersection(geometry);
                } catch (Exception e) {
                    // 裁剪失败时忽略，后续 PipelineBuilder 会再裁剪
                }
            }
            if (finalGeometry != null && !finalGeometry.isEmpty()) {
                oneRow.put(GEOM_FIELD_ALIAS_IN_TRAN, finalGeometry);
            }
            oneRow.remove(GEOM_FIELD_ALIAS_IN_SQL);
        } catch (Exception e) {
            log.error("featuresTransform异常", e);
            throw new RuntimeException("几何转换错误");
        }
    }

    public List<GirAdvOneRow> parallelPageQueryReturnList(
            String excuteSQL,
            Long totalCount,
            String orderFileIdName,
            Consumer<GirAdvOneRow> featuresTransform) {

        Long maxPageNumber = tileExecutorConfig.getMaxPageNumber();
        Long maxPageSize = tileExecutorConfig.getMaxPageSize();

        PageActuator<GirAdvOneRow> pageActuatorOpt =
                GirGeoTools.defaultInstance().getPageActuatorOpt(
                        new PageConditionDef<GirAdvOneRow>() {

                            @Override
                            public Long getTotalRecordCount() {
                                return totalCount;
                            }

                            @Override
                            public void setPageConfig(PageConfig pageConfig) {
                                pageConfig.setMaxPageNo(maxPageNumber);
                                pageConfig.setPageSize(maxPageSize);
                                pageConfig.setPageNumStartByZero(false);
                                pageConfig.setParallelConsumeRecordIs(true);
                                pageConfig.setParallelExecPageIs(true);
                                pageConfig.setSaveResultListIs(true);
                            }

                            @Override
                            public boolean handlePageException(
                                    Integer pageNo, Integer pageSize, Exception e) {
                                log.warn(
                                        "分页查询异常：页码[{}]，页大小[{}]，异常信息：{}",
                                        pageNo,
                                        pageSize,
                                        e.getMessage());
                                return true;
                            }

                            @Override
                            public List<GirAdvOneRow> getPageRecords(
                                    Integer pageNo, Integer pageSize) {
                                String orderSql =
                                        iAdvExecutor.pBuildSqlWithOrder(
                                                excuteSQL,
                                                ListUtil.of(
                                                        OrderApo.create(
                                                                orderFileIdName,
                                                                AdvEnumsOrder.升序)));
                                String pageSQL =
                                        iAdvExecutor.pBuildPageSql(
                                                orderSql, pageSize, pageNo, false);
                                List<GirAdvOneRow> girAdvOneRows =
                                        iAdvExecutor.bSelectList(pageSQL);
                                return girAdvOneRows
                                        .stream()
                                        .parallel()
                                        .peek(featuresTransform)
                                        .collect(Collectors.toList());
                            }
                        });
        pageActuatorOpt.execute();

        return pageActuatorOpt.getFinalDataList();
    }
}
