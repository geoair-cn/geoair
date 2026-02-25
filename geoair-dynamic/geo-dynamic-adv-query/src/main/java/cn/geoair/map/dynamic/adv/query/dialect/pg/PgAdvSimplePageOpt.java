package cn.geoair.map.dynamic.adv.query.dialect.pg;

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


/**
 * @author ：张逢吉
 * @date ：Created in 2025/10/10 11:09
 * @description： PostgreSQL 分页相关处理实现类
 * 基于 PostgreSQL 的 LIMIT/OFFSET 语法实现分页，支持空间查询与常规查询的分页统一处理
 * <p>
 * 后期打算 拓展的内容：
 * 1 封装序列化策略, 打算沿用  com.fasterxml.jackson.Serializer
 */

public class PgAdvSimplePageOpt implements IAdvSimplePageOpt {
    private static final GiLogger log = GirLogger.getLoger();
    // PostgreSQL 字段转义前缀后缀（避免关键字冲突）
    private static final String FIELD_QUOTE_PREFIX = "\"";
    private static final String FIELD_QUOTE_SUFFIX = "\"";

    PgAdvGeoOpt pgAdvGeoOpt;
    PgAdvBaseOpt baseOpt;
    PgAdvDDLOpt pgAdvDDLOpt;
    IDataSourceGetter dataSourceGetter;

    public PgAdvSimplePageOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
        baseOpt = new PgAdvBaseOpt(dataSourceGetter);
        pgAdvDDLOpt = new PgAdvDDLOpt(dataSourceGetter);
        pgAdvGeoOpt = new PgAdvGeoOpt(dataSourceGetter);
    }

    DialectTableNameProcessor dialectTableNameProcessor = PgDialectTableNameUtil.getInstance();

    @Override
    public Long pCount(String noPageSql) {
        // 参数校验
        if (StrUtil.isEmpty(noPageSql)) {
            throw new IllegalArgumentException("分页统计SQL不能为空");
        }

        try {
            // 构建计数SQL并执行
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSql);
            String countSql = StrUtil.format("SELECT COUNT (1) AS count FROM ({}) AS {}", cleanSql, dialectTableNameProcessor.tbGetTempAliasTableName());

            GirAdvOneRow result = baseOpt.bSelectOne(countSql);
            return result != null ? result.getLong("count") : 0L;
        } catch (Exception e) {
            log.error("分页统计失败，SQL: {}", noPageSql, e);
            throw new RuntimeException("分页统计异常: " + e.getMessage(), e);
        }
    }

    @Override
    public String pBuildPageSql(String noPageSql, int pageSize, int pageNum, boolean pageNumStartZero) {
        String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSql);
        long offset = calculateOffset(pageNum, pageSize, pageNumStartZero);
        return StrUtil.format("{} LIMIT {} OFFSET {}", cleanSql, pageSize, offset);
    }


    /**
     * 构建带排序的SQL（追加ORDER BY子句）
     *
     * @param baseSql    基础SQL（已重构的SQL）
     * @param orders     排序规则列表
     * @param tableAlias 临时表别名（用于排序字段定位，避免多表冲突）
     */
    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders, String tableAlias) {
        // 无排序规则，直接返回基础SQL
        if (CollectionUtil.isEmpty(orders)) {
            return baseSql;
        }

        // 构建ORDER BY子句（支持排序字段/排序函数）
        String orderByClause = orders.stream()
                .map(order -> {
                    String orderTarget;
                    // 优先使用排序函数（如 CAST(gtc_id AS numeric)）
                    if (order.isFunction()) {
                        orderTarget = order.getFunction();
                    } else {
                        // 排序字段：临时表别名.转义字段名（如 T_1a2b3c4d."gtc_id"）
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
                    // 拼接排序方向（如 ASC、DESC）
                    return StrUtil.format("{} {}", orderTarget, order.getAdvEnumsOrder().getValue());
                })
                .collect(Collectors.joining(", "));

        // 追加ORDER BY到基础SQL（去除末尾分号，避免语法错误）
        return StrUtil.format(
                "{} ORDER BY {}",
                dialectTableNameProcessor.tbRemoveSqlSpaces(baseSql),
                orderByClause
        );
    }

    public String pBuildSqlWithOrder(String baseSql, List<OrderApo> orders) {
        return pBuildSqlWithOrder(baseSql, orders, null);
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
        return pPage(noPageSql, pageNum, pageSize, false, advEnumsGeomOpt, false);
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
        // 参数校验
        validateFullPageParams(noPageSql, pageNum, pageSize, pageNumStartZero, orders);

        // 2. 查询SQL对应的字段元数据（含空间字段识别）
        DataFieldsApo dataFieldsApo = null;
        try {
            dataFieldsApo = pgAdvGeoOpt.dGetColumnsBySQL(noPageSql);
        } catch (Exception e) {
            log.error("查询SQL字段元数据失败，SQL：{}", noPageSql, e);
            throw new RuntimeException("获取字段信息异常：" + e.getMessage(), e);
        }

        // 3. 重构原始SQL：统一字段格式（避免重复字段、关键字冲突）
        // 3.1 提取所有字段名（去重）
        List<String> fieldNames = dataFieldsApo.getFieldList(FieldBySchemaApo::getColumnName, true);
        // 3.2 识别空间字段（用于后续空间操作）
        List<String> geomFieldNameList = dataFieldsApo.getGeomFieldNameList();
        // 3.3 字段名转义（如 "id"、"type_code"，避免PostgreSQL关键字冲突）
        String quotedFields = fieldNames.stream()
                .map(this::quoteFieldName)
                .collect(Collectors.joining(", "));
        // 3.4 生成临时表别名（避免同表多次关联冲突）
//        String tableAlias = dialectTableNameProcessor.tbGetTempAliasTableName();
        String tableAlias = "t_384_page_temp"; // 这里会出现由于每次随机别名，导致排序混乱，导致会出现第二页与第一页出现重复数据，这里就指定一个很难冲撞的临时表名
        // 3.5 重构SQL（统一格式：SELECT "字段1","字段2" FROM (原始SQL) AS 临时表）
        String refactorNoPageSql = StrUtil.format(
                "SELECT {} FROM ({}) AS {}",
                quotedFields,
                dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSql),
                tableAlias
        );
        String sqlWithOrder = pBuildSqlWithOrder(refactorNoPageSql, orders, tableAlias);

        // 计算总条数
        long total = pCount(sqlWithOrder);

        // 计算分页参数
        long offset = calculateOffset(pageNum, pageSize, pageNumStartZero);
        int lastPageNum = calculateLastPageNum(total, pageSize);

        // 构建分页SQL
        String pageSql = buildPageSql(sqlWithOrder, pageSize, offset);


        // 执行分页查询
        List<GirAdvOneRow> records = pgAdvGeoOpt.eSelectList(pageSql, advEnumsGeomOpt, geomFieldNameList);

        // 构建分页结果对象
        PageApo<GirAdvOneRow> pageApo = createPageApo(total, pageNum, pageSize, pageNumStartZero, lastPageNum, offset, records);

        // 如果需要包含字段元数据，就进行组装
        if (hasFieldsInfo) {
            pageApo.setDataFieldsApo(dataFieldsApo);
        }

        return pageApo;
    }


    private void validateFullPageParams(String noPageSql, int pageNum, int pageSize,
                                        boolean pageNumStartZero, List<OrderApo> orders) {
        // 1. 基础分页参数校验
        if (StrUtil.isEmpty(noPageSql)) {
            throw new IllegalArgumentException("分页SQL不能为空");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("每页条数必须大于0，当前值：" + pageSize);
        }
        // 页码校验（按起始规则）
        if ((pageNumStartZero && pageNum < 0) || (!pageNumStartZero && pageNum < 1)) {
            throw new IllegalArgumentException(StrUtil.format(
                    "页码不合法：页码{}从{}开始，当前值：{}",
                    pageNumStartZero ? "允许" : "不允许",
                    pageNumStartZero ? 0 : 1,
                    pageNum
            ));
        }


        // 3. 排序参数校验（非空列表需有有效排序规则）
        if (CollectionUtil.isNotEmpty(orders)) {
            for (int i = 0; i < orders.size(); i++) {
                OrderApo order = orders.get(i);
                if (order == null) {
                    throw new IllegalArgumentException("排序规则第" + (i + 1) + "项不能为空");
                }
                // 排序字段/函数二选一
                if (StrUtil.isEmpty(order.getFieldName()) && StrUtil.isEmpty(order.getFunction())) {
                    throw new IllegalArgumentException("排序规则第" + (i + 1) + "项：字段名和排序函数不能同时为空");
                }
                // 排序方向必须有效
                if (order.getAdvEnumsOrder() == null) {
                    throw new IllegalArgumentException("排序规则第" + (i + 1) + "项：排序方向（AdvEnumsOrder）不能为空");
                }
            }
        }
    }

    /**
     * 字段名转义（避免PostgreSQL关键字冲突，如 "order"、"user"）
     */
    private String quoteFieldName(String fieldName) {
        if (StrUtil.isEmpty(fieldName)) {
            return fieldName;
        }
        // 若已转义，直接返回（避免重复转义）
        if (fieldName.startsWith(FIELD_QUOTE_PREFIX) && fieldName.endsWith(FIELD_QUOTE_SUFFIX)) {
            return fieldName;
        }
        return FIELD_QUOTE_PREFIX + fieldName + FIELD_QUOTE_SUFFIX;
    }


    /**
     * 计算偏移量
     */
    private long calculateOffset(int pageNum, int pageSize, boolean pageNumStartZero) {
        return pageNumStartZero ? (long) pageNum * pageSize : (long) (pageNum - 1) * pageSize;
    }

    /**
     * 计算最后一页页码
     */
    private int calculateLastPageNum(long total, int pageSize) {
        if (total <= 0 || pageSize <= 0) {
            return 0;
        }
        return (int) ((total + pageSize - 1) / pageSize);
    }

    /**
     * 构建分页SQL语句
     */
    private String buildPageSql(String noPageSql, int pageSize, long offset) {
        String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSql);
        return StrUtil.format("{} LIMIT {} OFFSET {}", cleanSql, pageSize, offset);
    }


    /**
     * 创建分页结果对象
     */
    private PageApo<GirAdvOneRow> createPageApo(long total, int pageNum, int pageSize,
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
}
