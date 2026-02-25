package cn.geoair.map.dynamic.adv.query.dialect.pg;

import cn.geoair.gtc.base.log.GiLogger;
import cn.geoair.gtc.base.log.GirLogger;
import cn.geoair.map.dynamic.adv.query.IAdvSimplePagePreOpt;
import cn.geoair.map.dynamic.adv.query.apo.DataFieldsApo;
import cn.geoair.map.dynamic.adv.query.apo.OrderApo;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.apo.FieldBySchemaApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.ds.IDataSourceGetter;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.stream.Collectors;

import static cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt.不做任何操作;

/**
 * @author ：张逢吉
 * @date ：Created in   18:47
 * @description： PostgreSQL 带参数的分页处理实现类
 * 支持带 SqlParamMap 参数的分页查询，继承自 PgAdvSimplePageOpt，复用核心分页逻辑
 */
public class PgAdvSimplePagePreOpt extends PgAdvSimplePageOpt implements IAdvSimplePagePreOpt {
    protected static final GiLogger log = GirLogger.getLoger();

    public PgAdvSimplePagePreOpt(IDataSourceGetter dataSourceGetter) {
        super(dataSourceGetter);
    }

    /**
     * 带参数的分页总数统计
     * @param noPageSqlStatement 无分页SQL语句
     * @param sqlParam SQL参数映射
     * @return 总记录数
     */
    @Override
    public Long pCount(String noPageSqlStatement, SqlParamMap sqlParam) {
        // 参数校验
        if (StrUtil.isEmpty(noPageSqlStatement)) {
            throw new IllegalArgumentException("分页统计SQL不能为空");
        }

        try {
            // 构建计数SQL并执行（复用父类的SQL清理逻辑）
            String cleanSql = dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSqlStatement);
            String countSql = StrUtil.format("SELECT COUNT (1) AS count FROM ({}) AS {}", cleanSql, dialectTableNameProcessor.tbGetTempAliasTableName());

            // 执行带参数的查询
            GirAdvOneRow result = baseOpt.bSelectOne(countSql, sqlParam);
            return result != null ? result.getLong("count") : 0L;
        } catch (Exception e) {
            log.error("带参数分页统计失败，SQL: {}, 参数: {}", noPageSqlStatement, sqlParam, e);
            throw new RuntimeException("带参数分页统计异常: " + e.getMessage(), e);
        }
    }

    /**
     * 核心带参数分页方法（所有重载方法最终调用此方法）
     */
    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize,
                                       boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders) {
        // 1. 参数校验（复用父类的校验逻辑，补充参数非空校验）
        validateFullPageParams(noPageSqlStatement, pageNum, pageSize, pageNumStartZero, orders);
        if (sqlParam == null) {
            sqlParam = new SqlParamMap(); // 空参数时初始化空对象，避免NPE
        }

        // 2. 查询SQL对应的字段元数据（含空间字段识别）
        DataFieldsApo dataFieldsApo = null;
        try {
            // 调用带参数的字段元数据查询方法
            dataFieldsApo = pgAdvDDLOpt.dGetColumnsBySQL(noPageSqlStatement, sqlParam);
        } catch (Exception e) {
            log.error("查询带参数SQL字段元数据失败，SQL：{}，参数：{}", noPageSqlStatement, sqlParam, e);
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
        // 3.4 固定临时表别名（避免排序混乱导致分页重复）
        String tableAlias = "t_384_page_temp";
        // 3.5 重构SQL（统一格式：SELECT "字段1","字段2" FROM (原始SQL) AS 临时表）
        String refactorNoPageSql = StrUtil.format(
                "SELECT {} FROM ({}) AS {}",
                quotedFields,
                dialectTableNameProcessor.tbRemoveSqlSpaces(noPageSqlStatement),
                tableAlias
        );
        // 3.6 追加排序条件
        String sqlWithOrder = pBuildSqlWithOrder(refactorNoPageSql, orders, tableAlias);

        // 4. 计算总条数（带参数）
        long total = pCount(sqlWithOrder, sqlParam);

        // 5. 计算分页参数
        long offset = calculateOffset(pageNum, pageSize, pageNumStartZero);
        int lastPageNum = calculateLastPageNum(total, pageSize);

        // 6. 构建分页SQL
        String pageSql = buildPageSql(sqlWithOrder, pageSize, offset);

        // 7. 执行带参数的分页查询（处理空间字段）
        List<GirAdvOneRow> records = pgAdvGeoPreOpt.eSelectList(pageSql, sqlParam, advEnumsGeomOpt, geomFieldNameList);

        // 8. 构建分页结果对象
        PageApo<GirAdvOneRow> pageApo = createPageApo(total, pageNum, pageSize, pageNumStartZero, lastPageNum, offset, records);

        // 9. 组装字段元数据（如果需要）
        if (hasFieldsInfo) {
            pageApo.setDataFieldsApo(dataFieldsApo);
        }

        return pageApo;
    }

    // ==================== 以下是所有重载方法的实现（统一调用核心方法） ====================
    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, 不做任何操作, false, ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, advEnumsGeomOpt, false, ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, List<OrderApo> orders) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, 不做任何操作, false, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, boolean hasFieldsInfo) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, 不做任何操作, hasFieldsInfo, ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, hasFieldsInfo, ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, 不做任何操作, false, ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, List<OrderApo> orders) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, 不做任何操作, false, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, false, ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, boolean pageNumStartZero, AdvEnumsGeomOpt advEnumsGeomOpt, List<OrderApo> orders) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, pageNumStartZero, advEnumsGeomOpt, false, orders);
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, advEnumsGeomOpt, hasFieldsInfo, ListUtil.empty());
    }

    @Override
    public PageApo<GirAdvOneRow> pPage(String noPageSqlStatement, SqlParamMap sqlParam, int pageNum, int pageSize, AdvEnumsGeomOpt advEnumsGeomOpt, boolean hasFieldsInfo, List<OrderApo> orders) {
        return pPage(noPageSqlStatement, sqlParam, pageNum, pageSize, false, advEnumsGeomOpt, hasFieldsInfo, orders);
    }
}
