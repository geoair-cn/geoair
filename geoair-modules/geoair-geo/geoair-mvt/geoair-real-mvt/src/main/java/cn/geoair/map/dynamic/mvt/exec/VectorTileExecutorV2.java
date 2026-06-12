package cn.geoair.map.dynamic.mvt.exec;

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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

/** 矢量瓦片查询工具 V2版本，该版本把客户端传入的sql全部当做一个临时表进行处理 */
@Slf4j
public class VectorTileExecutorV2 extends AbstractITileExecutor {

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

    List<String> keepFieldList = new ArrayList<>();

    static final String geomBox = "geo_box";

    /** 空间字段的别名 */
    static final String GEOM_FIELD_ALIAS_IN_SQL = "geo_root";

    static final String GEOM_FIELD_ALIAS_IN_TRAN = "geom";

    public static VectorTileExecutorV2 getInstance(
            TileRequestParams requestParams, String layerName) {
        return new VectorTileExecutorV2(requestParams, layerName);
    }

    public VectorTileExecutorV2(TileRequestParams requestParams, String layerName) {
        super(requestParams, layerName);
        String keepField = requestParams.getKeepFields();
        List<String> keepFieldListTemp = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(keepField)) {
            keepFieldListTemp = StrUtil.split(keepField, ",");
            // 处理每个字段：移除双引号 + 提取.后面的字段名
        } else {
            keepFieldListTemp =
                    requestParams.getKeepFieldList() == null
                            ? new ArrayList<>()
                            : requestParams.getKeepFieldList();
        }
        for (String field : keepFieldListTemp) {
            if (StrUtil.isEmpty(field)) {
                continue; // 跳过空字符串
            }
            // 1. 移除双引号（处理可能的前后引号）
            String trimmed = field.replace("\"", "").trim();
            // 2. 提取.后面的字段名（如果包含.的话）
            int dotIndex = trimmed.lastIndexOf(".");
            if (dotIndex != -1 && dotIndex < trimmed.length() - 1) {
                trimmed = trimmed.substring(dotIndex + 1);
            }
            // 添加到结果列表（避免空值）
            if (StrUtil.isNotEmpty(trimmed)) {
                keepFieldList.add(trimmed);
            }
        }
    }

    /**
     * 获取缓冲区SQL函数
     *
     * @return
     */
    protected String getBufferBboxSqlFunction(TileExecParams tileExecParams) {
        Envelope dataExtentBufferEnvelope = tileExecParams.getDataExtentBufferEnvelope();
        double xmin = dataExtentBufferEnvelope.getMinX();
        double ymin = dataExtentBufferEnvelope.getMinY();
        double xmax = dataExtentBufferEnvelope.getMaxX();
        double ymax = dataExtentBufferEnvelope.getMaxY();
        return StrUtil.format(
                "public.ST_MakeEnvelope({}, {}, {}, {}, {})",
                xmin,
                ymin,
                xmax,
                ymax,
                sourceDataSrid);
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
            finalTbName = StrUtil.format("({})  as {} ", tbRemoveSqlSpaces, tableAlias);
        } else {
            String tbGetTableNameWithSchema =
                    iAdvExecutor.tbGetTableNameWithSchema(tbNameOrSql, schema);
            finalTbName =
                    StrUtil.format(
                            "( select * from  {})   as {} ", tbGetTableNameWithSchema, tableAlias);
        }
        // 校验必要参数
        // 2. 校验必要参数（从Params读取）
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
        // 构建geom字段表达式（添加表别名前缀）
        String geomField = null;
        if (ObjectUtil.equals(srid, "0")) {
            geomField = "public.ST_SetSRID(" + tableAlias + "." + geomFieldName + "," + srid + ")";
        } else {
            geomField = tableAlias + "." + geomFieldName;
        }
        StringBuilder withSQL = new StringBuilder();
        // 构建WITH查询块 - 定义查询区域
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
            rootSql.append("encode(public.ST_AsBinary(public.ST_Force2D(")
                    .append(geomField)
                    .append(")), 'base64') as  ")
                    .append(GEOM_FIELD_ALIAS_IN_SQL);
        } else if (requestParams.isKeepFieldAll()) {
            rootSql.append(tableAlias)
                    .append(".*")
                    .append(", encode(")
                    .append(StrUtil.format("public.ST_AsBinary({})", geomField))
                    .append(", 'base64') as  ")
                    .append(GEOM_FIELD_ALIAS_IN_SQL);
        } else {
            // 保留指定字段并添加表别名前缀，同时拼接geom字段
            List<String> aliasFields = new ArrayList<>();
            for (String field : keepFieldList) {
                if (StrUtil.isNotBlank(field)) {
                    // 给每个字段添加表别名前缀
                    aliasFields.add(tableAlias + "." + StrUtil.wrap(field, "\""));
                }
            }

            // 拼接所有带别名的字段
            rootSql.append(String.join(", ", aliasFields))
                    // 拼接geom字段（处理交集逻辑）
                    .append(", encode(")
                    .append(StrUtil.format("public.ST_AsBinary({})", geomField))
                    .append(", 'base64') as  ")
                    .append(GEOM_FIELD_ALIAS_IN_SQL);
        }

        // 构建FROM和WHERE部分（主表添加别名T）
        rootSql.append(" FROM ")
                .append(finalTbName)
                .append(" ")
                .append(",  ")
                .append(withQueryAlias)
                .append(" ")
                .append(withQueryAlias)
                .append(" WHERE public.ST_Intersects(")
                .append(geomField)
                .append(",")
                .append(withQueryAlias)
                .append(".")
                .append(geomBox)
                .append(" )")
                .append(" and ")
                .append(geomField)
                .append(" is not null  ");

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
                // 策略1：分页查询
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

    /**
     * 处理 GirAdvOneRow 列表，完成转换、密度合并、写入 TileBuilder
     *
     * @param girAdvOneRows 待处理数据列表
     * @param densityOptStrategy 密度优化策略
     * @param vectorTileBuilder 瓦片构建器
     * @param gridSrid 网格坐标系
     */
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
            String geomEncodeStr = oneRow.getStr(GEOM_FIELD_ALIAS_IN_SQL);
            byte[] decode = Base64.decode(geomEncodeStr);
            WKBReader wkbReader = new WKBReader();
            Geometry geometry = wkbReader.read(decode);
            // 将geom转到extent坐标系下
            if (!ObjectUtil.equals(gridSrid, sourceDataSrid)) {
                geometry =
                        GirGeoTools.defaultInstance()
                                .getSridOpt()
                                .convert(geometry, sourceDataSrid, gridSrid);
            }
            // 内存裁剪数据
            Geometry finalGeometry = geometry;
            if (geometry != null) {
                try {
                    // intersection 返回的「空几何类型」由调用方的几何类型
                    Geometry gridExtentBufferBoxGeom =
                            this.tileExecParams.getGridExtentBufferBoxGeom();
                    finalGeometry = gridExtentBufferBoxGeom.intersection(geometry);
                } catch (Exception e) {
                    // 有的数据库里面的几何裁剪不了，这里就直接抛弃，因为在转换成屏幕坐标的时候，也会再裁剪一遍的
                }
            }
            if (finalGeometry != null && !finalGeometry.isEmpty()) {
                oneRow.put(GEOM_FIELD_ALIAS_IN_TRAN, finalGeometry);
            }
            oneRow.remove(GEOM_FIELD_ALIAS_IN_SQL);
        } catch (Exception e) {
            log.error("featuresTransform异常", e);
            throw new RuntimeException("wkt转几何错误");
        }
    }

    /**
     * 并行分页查询
     *
     * @param excuteSQL 执行SQL
     * @param totalCount 总条数
     * @param orderFileIdName 排序字段
     */
    public List<GirAdvOneRow> parallelPageQueryReturnList(
            String excuteSQL,
            Long totalCount,
            String orderFileIdName,
            Consumer<GirAdvOneRow> featuresTransform) {

        Long maxPageNumber = tileExecutorConfig.getMaxPageNumber();
        Long maxPageSize = tileExecutorConfig.getMaxPageSize();

        PageActuator<GirAdvOneRow> pageActuatorOpt =
                GirGeoTools.defaultInstance()
                        .getPageActuatorOpt(
                                new PageConditionDef<GirAdvOneRow>() {

                                    @Override
                                    public Long getTotalRecordCount() {
                                        return totalCount;
                                    }

                                    @Override
                                    public void setPageConfig(PageConfig pageConfig) {
                                        pageConfig.setMaxPageNo(maxPageNumber);
                                        pageConfig.setPageSize(maxPageSize);
                                        //
                                        // pageConfig.setTotalCount(totalCount);
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
