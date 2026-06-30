package cn.geoair.map.dynamic.adv.query.dialect;

import static cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt.不做任何操作;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLoggerFactory;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsKeyTran;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvQueryCommonUtils;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractExecAdvSimplePageOpt implements IAdvSimplePageOpt {

    protected static final GiLogger log = GirLoggerFactory.getLogger();

    protected IDataSourceGetter dataSourceGetter;

    protected DialectTableNameProcessor dialectTableNameProcessor;

    public AbstractExecAdvSimplePageOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
        this.dialectTableNameProcessor = getDialectTableNameProcessor();
    }

    protected abstract IAdvBaseOpt getAdvBaseOpt();

    protected abstract IAdvDDLOpt getAdvDDLOpt();

    protected abstract IAdvGeoPreOpt getAdvGeoPreOpt();

    @Override
    public AdvQueryGlobalConfig getConfig() {
        return getAdvBaseOpt().getConfig();
    }

    /**
     * 获取方言专属的表名处理器
     */
    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();


    /**
     * 核心分页查询实现
     *
     * @param noPageSqlStatement 原始SQL（不分页）
     * @param sqlParam           SQL参数
     * @param pageNum            页码
     * @param pageSize           每页大小
     * @param pageNumStartZero   页码是否从0开始
     * @param advEnumsGeomOpt    几何对象处理选项
     * @param hasFieldsInfo      是否包含字段元数据
     * @param orders             排序规则
     * @param advEnumsKeyTran    键名转换规则
     * @return 分页结果
     */
    protected PageApo<GirAdvOneRow> corePage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            List<OrderApo> orders,
            AdvEnumsKeyTran advEnumsKeyTran) {

        validateFullPageParams(noPageSqlStatement, pageNum, pageSize, pageNumStartZero, orders);


        DataFieldsApo dataFieldsApo = null;
        try {
            dataFieldsApo = getColumnsBySQLWithParam(noPageSqlStatement, sqlParam);
        } catch (Exception e) {
            log.error("查询带参数SQL字段元数据失败，SQL：{}，参数：{}", noPageSqlStatement, sqlParam, e);
            throw new RuntimeException("获取字段信息异常：" + e.getMessage(), e);
        }


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

        String pageSql = dialectTableNameProcessor.tbBuildPageSql(sqlWithOrder, pageNum, pageSize, pageNumStartZero);

        Map<String, Object> resultMap = Stream.of("count", "list")
                .parallel()
                .map(task -> {
                    Map<String, Object> map = new HashMap<>();
                    if ("count".equals(task)) {
                        map.put("count", pCount(sqlWithOrder, sqlParam));
                    } else {
                        List<GirAdvOneRow> girAdvOneRows = getAdvGeoPreOpt().eSelectList(pageSql, sqlParam, advEnumsGeomOpt, geomFieldNameList);
                        convertPageOriginalResults(girAdvOneRows);
                        map.put("list", girAdvOneRows);
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
        long pageOffset = dialectTableNameProcessor.getPageOffset(pageNum, pageSize, pageNumStartZero);
        // 8. 通用：构建分页结果
        PageApo<GirAdvOneRow> pageApo =
                GirAdvQueryCommonUtils.createPageApo(
                        total, pageNum, pageSize, pageNumStartZero, lastPageNum, pageOffset, records);

        // 9. 通用：组装字段元数据
        if (hasFieldsInfo) {
            pageApo.setDataFieldsApo(dataFieldsApo);
        }

        return pageApo;
    }


    @Override
    public Long pCount(String noPageSql) {
        return pCount(noPageSql, new SqlParamMap());
    }

    @Override
    public String pBuildPageSql(
            String noPageSql, int pageSize, int pageNum, boolean pageNumStartZero) {
        String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSql);
        return dialectTableNameProcessor.tbBuildPageSql(cleanSql, pageNum, pageSize, pageNumStartZero);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, false,
                不做任何操作, false, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, pageNumStartZero,
                不做任何操作, false, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            List<OrderApo> orders) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, pageNumStartZero,
                不做任何操作, false, orders, AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql, int pageNum, int pageSize, List<OrderApo> orders) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, false,
                不做任何操作, false, orders, AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, false,
                advEnumsGeomOpt, false, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, pageNumStartZero,
                advEnumsGeomOpt, false, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            List<OrderApo> orders) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, pageNumStartZero,
                advEnumsGeomOpt, false, orders, AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, false,
                advEnumsGeomOpt, hasFieldsInfo, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            List<OrderApo> orders) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, false,
                advEnumsGeomOpt, hasFieldsInfo, orders, AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            boolean hasFieldsInfo) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, pageNumStartZero,
                不做任何操作, hasFieldsInfo, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, pageNumStartZero,
                advEnumsGeomOpt, hasFieldsInfo, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            List<OrderApo> orders) {
        return corePage(noPageSql, new SqlParamMap(), pageNum, pageSize, pageNumStartZero,
                advEnumsGeomOpt, hasFieldsInfo, orders, AdvEnumsKeyTran.不转换);
    }

    @Override
    public Long pCount(String noPageSqlStatement, GirSqlParam sqlParam) {
        if (StrUtil.isEmpty(noPageSqlStatement)) {
            throw new IllegalArgumentException("分页统计SQL不能为空");
        }
        try {
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSqlStatement);
            String template = dialectTableNameProcessor.tbBuildAsTable(" SELECT COUNT(*) AS count FROM ({})", "{}");
            String countSql =
                    StrUtil.format(
                            template,
                            cleanSql,
                            dialectTableNameProcessor.tbGetTempAliasTableName());
            return executeCountSqlWithParam(countSql, sqlParam);
        } catch (Exception e) {
            log.error("带参数分页统计失败，SQL: {}, 参数: {}", noPageSqlStatement, sqlParam, e);
            throw new RuntimeException("带参数分页统计异常: " + e.getMessage(), e);
        }
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
            List<OrderApo> orders) {
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero,
                advEnumsGeomOpt, hasFieldsInfo, orders, AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement, GirSqlParam sqlParam, int pageNum, int pageSize) {
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, false,
                不做任何操作, false, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt) {
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, false,
                advEnumsGeomOpt, false, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            List<OrderApo> orders) {
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, false,
                不做任何操作, false, orders, AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            boolean hasFieldsInfo) {
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero,
                不做任何操作, hasFieldsInfo, ListUtil.empty(), AdvEnumsKeyTran.不转换);
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
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero,
                advEnumsGeomOpt, hasFieldsInfo, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero) {
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero,
                不做任何操作, false, ListUtil.empty(), AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            List<OrderApo> orders) {
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero,
                不做任何操作, false, orders, AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt) {
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero,
                advEnumsGeomOpt, false, ListUtil.empty(), AdvEnumsKeyTran.不转换);
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
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero,
                advEnumsGeomOpt, false, orders, AdvEnumsKeyTran.不转换);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo) {
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, false,
                advEnumsGeomOpt, hasFieldsInfo, ListUtil.empty(), AdvEnumsKeyTran.不转换);
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
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, false,
                advEnumsGeomOpt, hasFieldsInfo, orders, AdvEnumsKeyTran.不转换);
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
            List<OrderApo> orders,
            AdvEnumsKeyTran advEnumsKeyTran) {
        return corePage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero,
                advEnumsGeomOpt, hasFieldsInfo, orders, advEnumsKeyTran);
    }


    /**
     * 执行带参数的统计SQL，返回总数
     */
    protected Long executeCountSqlWithParam(String countSql, GirSqlParam sqlParam) {
        GirAdvOneRow result = getAdvBaseOpt().bSelectOne(countSql, sqlParam);
        return result != null ? result.getLong("count") : 0L;
    }

    /**
     * 带参数获取SQL字段元数据
     */
    protected DataFieldsApo getColumnsBySQLWithParam(String noPageSql, GirSqlParam sqlParam) {
        return getAdvDDLOpt().dGetColumnsBySQL(noPageSql, sqlParam);
    }


    protected int calculateLastPageNum(long total, int pageSize) {
        if (total <= 0 || pageSize <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders, String tableAlias) {
        if (CollectionUtil.isEmpty(orders)) {
            return baseSql;
        }

        String orderByClause =
                orders.stream()
                        .map(
                                order -> {
                                    String orderTarget;
                                    if (order.isFunction()) {
                                        orderTarget = order.getFunction();
                                    } else {
                                        if (StrUtil.isEmpty(tableAlias)) {
                                            orderTarget = quoteFieldName(order.getFieldName());
                                        } else {
                                            orderTarget =
                                                    StrUtil.format(
                                                            "{}.{}",
                                                            tableAlias,
                                                            quoteFieldName(order.getFieldName()));
                                        }
                                    }
                                    return StrUtil.format(
                                            "{} {}",
                                            orderTarget,
                                            order.getAdvEnumsOrder().getValue());
                                })
                        .collect(Collectors.joining(", "));

        return StrUtil.format(
                "{} ORDER BY {}",
                dialectTableNameProcessor.tbRemoveSqlSpaces(baseSql),
                orderByClause);
    }

    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders) {
        return pBuildSqlWithOrder(baseSql, orders, null);
    }

    public void convertPageOriginalResults(List<GirAdvOneRow> records) {

    }

    protected String quoteFieldName(String fieldName) {
        return getDialectTableNameProcessor().tbQuoteFieldName(fieldName);
    }

    protected void validateFullPageParams(
            String noPageSql,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            List<OrderApo> orders) {
        if (StrUtil.isEmpty(noPageSql)) {
            throw new IllegalArgumentException("分页SQL不能为空");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("每页条数必须大于0，当前值：" + pageSize);
        }
        if ((pageNumStartZero && pageNum < 0) || (!pageNumStartZero && pageNum < 1)) {
            throw new IllegalArgumentException(
                    StrUtil.format(
                            "页码不合法：页码{}从{}开始，当前值：{}",
                            pageNumStartZero ? "允许" : "不允许",
                            pageNumStartZero ? 0 : 1,
                            pageNum));
        }

        if (CollectionUtil.isNotEmpty(orders)) {
            for (int i = 0; i < orders.size(); i++) {
                OrderApo order = orders.get(i);
                if (order == null) {
                    throw new IllegalArgumentException("排序规则第" + (i + 1) + "项不能为空");
                }
                if (StrUtil.isEmpty(order.getFieldName()) && StrUtil.isEmpty(order.getFunction())) {
                    throw new IllegalArgumentException("排序规则第" + (i + 1) + "项：字段名和排序函数不能同时为空");
                }
                if (order.getAdvEnumsOrder() == null) {
                    throw new IllegalArgumentException(
                            "排序规则第" + (i + 1) + "项：排序方向（AdvEnumsOrder）不能为空");
                }
            }
        }
    }
}
