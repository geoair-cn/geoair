package cn.geoair.map.dynamic.adv.query.dialect;

import static cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt.不做任何操作;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsKeyTran;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvQueryCommonUtils;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractExecAdvSimplePagePreOpt extends AbstractExecAdvSimplePageOpt
        implements IAdvSimplePagePreOpt {

    protected static final GiLogger log = GirLogger.getLoger();

    public AbstractExecAdvSimplePagePreOpt(IDataSourceGetter dataSourceGetter) {
        super(dataSourceGetter);
    }

    protected abstract IAdvBaseOpt getAdvBaseOpt();

    protected abstract IAdvDDLOpt getAdvDDLOpt();

    protected abstract IAdvGeoPreOpt getAdvGeoPreOpt();

    @Override
    public AdvQueryGlobalConfig getConfig() {
        return getAdvBaseOpt().getConfig();
    }

    // ========== 通用逻辑：带参数的总数统计 ==========
    @Override
    public Long pCount(String noPageSqlStatement, GirSqlParam sqlParam) {
        if (StrUtil.isEmpty(noPageSqlStatement)) {
            throw new IllegalArgumentException("分页统计SQL不能为空");
        }
        // 空参数初始化，避免NPE
//        SqlParamMap param = sqlParam == null ? new SqlParamMap() : sqlParam;

        try {
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSqlStatement);
            String template = dialectTableNameProcessor.tbBuildAsTable(" SELECT COUNT(*) AS count FROM ({})", "{}");
            String countSql =
                    StrUtil.format(
                            template,
                            cleanSql,
                            dialectTableNameProcessor.tbGetTempAliasTableName());
            // 子类实现：执行带参数的统计查询
            return executeCountSqlWithParam(countSql, sqlParam);
        } catch (Exception e) {
            log.error("带参数分页统计失败，SQL: {}, 参数: {}", noPageSqlStatement, sqlParam, e);
            throw new RuntimeException("带参数分页统计异常: " + e.getMessage(), e);
        }
    }

    // ========== 通用逻辑：核心带参数分页方法 ==========


    // ========== 通用逻辑：核心带参数分页方法 ==========
    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            List<OrderApo> orders) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, hasFieldsInfo, orders, AdvEnumsKeyTran.不转换);
    }


    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            List<OrderApo> orders, AdvEnumsKeyTran advEnumsKeyTran) {

        validateFullPageParams(noPageSqlStatement, pageNum, pageSize, pageNumStartZero, orders);

        // 2. 子类实现：带参数获取字段元数据
        DataFieldsApo dataFieldsApo = null;
        try {
            dataFieldsApo = getColumnsBySQLWithParam(noPageSqlStatement, sqlParam);
        } catch (Exception e) {
            log.error("查询带参数SQL字段元数据失败，SQL：{}，参数：{}", noPageSqlStatement, sqlParam, e);
            throw new RuntimeException("获取字段信息异常：" + e.getMessage(), e);
        }

        // 3. 通用：重构SQL（字段转义、临时表、排序）
        List<String> fieldNames = dataFieldsApo.getFieldList(FieldBySchemaApo::getColumnName, true);
        List<String> geomFieldNameList = dataFieldsApo.getGeomFieldNameList();
        String quotedFields =
                fieldNames.stream().map(this::quoteFieldName).collect(Collectors.joining(", "));
        String tableAlias = dialectTableNameProcessor.tbGetTempAliasTableName();
        String template = dialectTableNameProcessor.tbBuildAsTable(" SELECT {} FROM ({})", "{}");
        String refactorNoPageSql =
                StrUtil.format(
                        template,
                        quotedFields,
                        dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSqlStatement),
                        tableAlias);
        String sqlWithOrder = pBuildSqlWithOrder(refactorNoPageSql, orders, tableAlias);
        long offset = calculateOffset(pageNum, pageSize, pageNumStartZero);
        String pageSql = dialectTableNameProcessor.tbBuildPageSql(sqlWithOrder, pageSize, offset);
        Map<String, Object> resultMap = Stream.of("count", "list")
                .parallel()
                .map(task -> {
                    Map<String, Object> map = new HashMap<>();
                    if ("count".equals(task)) {
                        map.put("count", pCount(sqlWithOrder, sqlParam));
                    } else {
                        map.put("list", getAdvGeoPreOpt().eSelectList(pageSql, sqlParam, advEnumsGeomOpt, geomFieldNameList));
                    }
                    return map;
                })
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Long total = (Long) resultMap.get("count");
        List<GirAdvOneRow> records = resultMap.get("list") != null ? (List<GirAdvOneRow>) resultMap.get("list") : ListUtil.empty();
        if (Objects.equals(advEnumsKeyTran, AdvEnumsKeyTran.转换成大小写不敏感)) {
            records = GirAdvOneRow.toCaseInsensitiveList(records);
        }
        if (Objects.equals(advEnumsKeyTran, AdvEnumsKeyTran.转换成驼峰)) {
            records = GirAdvOneRow.toCamelCaseList(records);
        }
        int lastPageNum = calculateLastPageNum(total, pageSize);
        // 8. 通用：构建分页结果
        PageApo<GirAdvOneRow> pageApo =
                GirAdvQueryCommonUtils.createPageApo(
                        total, pageNum, pageSize, pageNumStartZero, lastPageNum, offset, records);

        // 9. 通用：组装字段元数据
        if (hasFieldsInfo) {
            pageApo.setDataFieldsApo(dataFieldsApo);
        }

        return pageApo;
    }

    // ========== 通用：所有重载方法（统一调用核心方法） ==========
    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement, GirSqlParam sqlParam, int pageNum, int pageSize) {
        return pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                false,
                不做任何操作,
                false,
                ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                false,
                advEnumsGeomOpt,
                false,
                ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            List<OrderApo> orders) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, 不做任何操作, false, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            boolean hasFieldsInfo) {
        return pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                pageNumStartZero,
                不做任何操作,
                hasFieldsInfo,
                ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo) {
        return pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                pageNumStartZero,
                advEnumsGeomOpt,
                hasFieldsInfo,
                ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero) {
        return pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                pageNumStartZero,
                不做任何操作,
                false,
                ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            List<OrderApo> orders) {
        return pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                pageNumStartZero,
                不做任何操作,
                false,
                orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                pageNumStartZero,
                advEnumsGeomOpt,
                false,
                ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<OrderApo> orders) {
        return pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                pageNumStartZero,
                advEnumsGeomOpt,
                false,
                orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo) {
        return pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                false,
                advEnumsGeomOpt,
                hasFieldsInfo,
                ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            List<OrderApo> orders) {
        return pPage(
                noPageSqlStatement,
                sqlParam,
                pageNum,
                pageSize,
                false,
                advEnumsGeomOpt,
                hasFieldsInfo,
                orders);
    }

    /**
     * 执行带参数的统计SQL，返回总数
     */
    protected Long executeCountSqlWithParam(String countSql, GirSqlParam sqlParam) {
        GirAdvOneRow result = getAdvBaseOpt().bSelectOne(countSql, sqlParam);
        return result != null ? result.getLong("count") : 0L;
    }

    @Override
    protected Long executeCountSql(String countSql) {
        return executeCountSqlWithParam(countSql, new SqlParamMap());
    }

    /**
     * 带参数获取SQL字段元数据
     */
    protected DataFieldsApo getColumnsBySQLWithParam(String noPageSql, GirSqlParam sqlParam) {
        return getAdvDDLOpt().dGetColumnsBySQL(noPageSql, sqlParam);
    }

    @Override
    protected DataFieldsApo getColumnsBySQL(String noPageSql) {
        return getColumnsBySQLWithParam(noPageSql, new SqlParamMap());
    }


    @Override
    protected List<GirAdvOneRow> executePageSql(
            String pageSql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList) {
        return getAdvGeoPreOpt().eSelectList(pageSql,   advEnumsGeomOpt, geomFieldNameList);

    }
}
