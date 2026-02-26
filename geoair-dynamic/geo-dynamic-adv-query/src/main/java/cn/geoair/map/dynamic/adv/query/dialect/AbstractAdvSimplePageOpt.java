package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.gtc.base.log.GiLogger;
import cn.geoair.gtc.base.log.GirLogger;
import cn.geoair.map.dynamic.adv.query.IAdvSimplePageOpt;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

import static cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt.不做任何操作;


public abstract class AbstractAdvSimplePageOpt implements IAdvSimplePageOpt {
    protected static final GiLogger log = GirLogger.getLoger();

    protected IDataSourceGetter dataSourceGetter;

    protected DialectTableNameProcessor dialectTableNameProcessor;

    public AbstractAdvSimplePageOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
        this.dialectTableNameProcessor = getDialectTableNameProcessor();
    }

    // ========== 抽象方法：子类实现差异化逻辑 ==========
    /**
     * 获取方言专属的表名处理器
     */
    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();

    /**
     * 执行统计SQL，返回总数
     */
    protected abstract Long executeCountSql(String countSql);

    /**
     * 构建分页SQL（不同数据库语法不同）
     */
    protected abstract String buildPageSql(String noPageSql, int pageSize, long offset);


    /**
     * 获取SQL对应的字段元数据
     */
    protected abstract DataFieldsApo getColumnsBySQL(String noPageSql);

    /**
     * 执行分页查询，返回结果列表
     */
    protected abstract List<GirAdvOneRow> executePageSql(String pageSql, AdvEnumsGeomOpt advEnumsGeomOpt, List<String> geomFieldNameList);

    /**
     * 获取临时表别名
     */
    protected abstract String getTempTableAlias();

    // ========== 通用逻辑：所有数据库都适用 ==========
    @Override
    public Long pCount(String noPageSql) {
        if (StrUtil.isEmpty(noPageSql)) {
            throw new IllegalArgumentException("分页统计SQL不能为空");
        }

        try {
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSql);
            String countSql = StrUtil.format("SELECT COUNT (1) AS count FROM ({}) AS {}",
                    cleanSql, dialectTableNameProcessor.tbGetTempAliasTableName());
            return executeCountSql(countSql);
        } catch (Exception e) {
            log.error("分页统计失败，SQL: {}", noPageSql, e);
            throw new RuntimeException("分页统计异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String pBuildPageSql(String noPageSql, int pageSize, int pageNum, boolean pageNumStartZero) {
        String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSql);
        long offset = calculateOffset(pageNum, pageSize, pageNumStartZero);
        return buildPageSql(cleanSql, pageSize, offset);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize) {
        return pPage(noPageSql, pageNum, pageSize, false);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero) {
        return pPage(noPageSql, pageNum, pageSize, pageNumStartZero, 不做任何操作);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, List<OrderApo> orders) {
        return pPage(noPageSql, pageNum, pageSize, pageNumStartZero, 不做任何操作, false, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, List<OrderApo> orders) {
        return pPage(noPageSql, pageNum, pageSize, false, 不做任何操作, false, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pPage(noPageSql, pageNum, pageSize, false, advEnumsGeomOpt);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pPage(noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, false);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, List<OrderApo> orders) {
        return pPage(noPageSql, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, false, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo) {
        return pPage(noPageSql, pageNum, pageSize, false, advEnumsGeomOpt, hasFieldsInfo);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders) {
        return pPage(noPageSql, pageNum, pageSize, false, advEnumsGeomOpt, hasFieldsInfo, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, boolean hasFieldsInfo) {
        return pPage(noPageSql, pageNum, pageSize, pageNumStartZero, 不做任何操作, false);
    }

    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo) {
        return pPage(noPageSql, pageNum, pageSize, pageNumStartZero, 不做任何操作, false, ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSql, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders) {
        // 通用参数校验
        validateFullPageParams(noPageSql, pageNum, pageSize, pageNumStartZero, orders);

        // 子类实现：获取字段元数据
        DataFieldsApo dataFieldsApo = null;
        try {
            dataFieldsApo = getColumnsBySQL(noPageSql);
        } catch (Exception e) {
            log.error("查询SQL字段元数据失败，SQL：{}", noPageSql, e);
            throw new RuntimeException("获取字段信息异常：" + e.getMessage(), e);
        }

        // 通用：提取字段列表
        List<String> fieldNames = dataFieldsApo.getFieldList(FieldBySchemaApo::getColumnName, true);
        List<String> geomFieldNameList = dataFieldsApo.getGeomFieldNameList();

        // 子类实现：字段名转义
        String quotedFields = fieldNames.stream()
                .map(this::quoteFieldName)
                .collect(Collectors.joining(", "));

        // 通用：临时表别名
        String tableAlias = getTempTableAlias();

        // 通用：重构SQL
        String refactorNoPageSql = StrUtil.format(
                "SELECT {} FROM ({}) AS {}",
                quotedFields,
                dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSql),
                tableAlias
        );

        // 通用：拼接排序
        String sqlWithOrder = pBuildSqlWithOrder(refactorNoPageSql, orders, tableAlias);

        // 通用：计算总条数
        long total = pCount(sqlWithOrder);

        // 通用：计算分页参数
        long offset = calculateOffset(pageNum, pageSize, pageNumStartZero);
        int lastPageNum = calculateLastPageNum(total, pageSize);

        // 子类实现：构建分页SQL
        String pageSql = buildPageSql(sqlWithOrder, pageSize, offset);

        // 子类实现：执行分页查询
        List<GirAdvOneRow> records = executePageSql(pageSql, advEnumsGeomOpt, geomFieldNameList);

        // 通用：构建分页结果
        PageApo<GirAdvOneRow> pageApo = createPageApo(total, pageNum, pageSize, pageNumStartZero, lastPageNum, offset, records);

        // 通用：组装字段元数据
        if (hasFieldsInfo) {
            pageApo.setDataFieldsApo(dataFieldsApo);
        }

        return pageApo;
    }


    protected void validateFullPageParams(String noPageSql, int pageNum, int pageSize,
                                          boolean pageNumStartZero, List<OrderApo> orders) {
        if (StrUtil.isEmpty(noPageSql)) {
            throw new IllegalArgumentException("分页SQL不能为空");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("每页条数必须大于0，当前值：" + pageSize);
        }
        if ((pageNumStartZero && pageNum < 0) || (!pageNumStartZero && pageNum < 1)) {
            throw new IllegalArgumentException(StrUtil.format(
                    "页码不合法：页码{}从{}开始，当前值：{}",
                    pageNumStartZero ? "允许" : "不允许",
                    pageNumStartZero ? 0 : 1,
                    pageNum
            ));
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
                    throw new IllegalArgumentException("排序规则第" + (i + 1) + "项：排序方向（AdvEnumsOrder）不能为空");
                }
            }
        }
    }

    protected long calculateOffset(int pageNum, int pageSize, boolean pageNumStartZero) {
        return pageNumStartZero ? (long) pageNum * pageSize : (long) (pageNum - 1) * pageSize;
    }

    protected int calculateLastPageNum(long total, int pageSize) {
        if (total <= 0 || pageSize <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    protected PageApo<GirAdvOneRow> createPageApo(long total, int pageNum, int pageSize,
                                                  boolean pageNumStartZero, int lastPageNum,
                                                  long startRow, List<GirAdvOneRow> records) {
        PageApo<GirAdvOneRow> pageApo = new PageApo<>();
        pageApo.setTotal(total)
                .setPageNum(pageNum)
                .setPageSize(pageSize)
                .setPageNumStartZero(pageNumStartZero)
                .setLastPageNum(lastPageNum)
                .setStartRow(startRow)
                .setRecords(records);
        return pageApo;
    }

    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders, String tableAlias) {
        if (CollectionUtil.isEmpty(orders)) {
            return baseSql;
        }

        String orderByClause = orders.stream()
                .map(order -> {
                    String orderTarget;
                    if (order.isFunction()) {
                        orderTarget = order.getFunction();
                    } else {
                        if (StrUtil.isEmpty(tableAlias)) {
                            orderTarget = quoteFieldName(order.getFieldName());
                        } else {
                            orderTarget = StrUtil.format(
                                    "{}.{}",
                                    tableAlias,
                                    quoteFieldName(order.getFieldName())
                            );
                        }
                    }
                    return StrUtil.format("{} {}", orderTarget, order.getAdvEnumsOrder().getValue());
                })
                .collect(Collectors.joining(", "));

        return StrUtil.format(
                "{} ORDER BY {}",
                dialectTableNameProcessor.tbRemoveSqlSpaces(baseSql),
                orderByClause
        );
    }

    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders) {
        return pBuildSqlWithOrder(baseSql, orders, null);
    }
    protected String quoteFieldName(String fieldName) {
        return getDialectTableNameProcessor().tbQuoteFieldName(fieldName);
    }
}
