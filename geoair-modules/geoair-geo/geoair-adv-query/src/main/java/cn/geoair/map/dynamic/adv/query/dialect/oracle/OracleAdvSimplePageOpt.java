package cn.geoair.map.dynamic.adv.query.dialect.oracle;

import cn.geoair.base.log.GiLogger;
import cn.geoair.base.log.GirLogger;
import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseOpt;
import cn.geoair.map.dynamic.adv.query.IAdvDDLOpt;
import cn.geoair.map.dynamic.adv.query.IAdvGeoPreOpt;
import cn.geoair.map.dynamic.adv.query.apo.*;
import cn.geoair.map.dynamic.adv.query.dialect.AbstractExecAdvSimplePagePreOpt;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

import cn.geoair.map.dynamic.adv.query.utils.GirAdvQueryCommonUtils;
import cn.hutool.core.util.StrUtil;

/**
 * Oracle带参数分页实现类
 */
public class OracleAdvSimplePageOpt extends AbstractExecAdvSimplePagePreOpt {

    protected static final GiLogger log = GirLogger.getLoger();

    // Oracle专属的依赖类
    protected IAdvGeoPreOpt advGeoPreOpt;
    protected IAdvBaseOpt baseOpt;
    protected IAdvDDLOpt advDDLOpt;

    public OracleAdvSimplePageOpt(IDataSourceGetter dataSourceGetter, IAdvBaseOpt baseOpt,
                                  IAdvGeoPreOpt advGeoPreOpt, IAdvDDLOpt advDDLOpt) {
        super(dataSourceGetter);
        this.baseOpt = baseOpt;
        this.advDDLOpt = advDDLOpt;
        this.advGeoPreOpt = advGeoPreOpt;
    }

    @Override
    protected DialectTableNameProcessor getDialectTableNameProcessor() {
        return OracleDialectTableNameUtil.getInstance();
    }

    @Override
    protected IAdvBaseOpt getAdvBaseOpt() {
        return baseOpt;
    }

    @Override
    protected IAdvDDLOpt getAdvDDLOpt() {
        return advDDLOpt;
    }

    @Override
    protected IAdvGeoPreOpt getAdvGeoPreOpt() {
        return advGeoPreOpt;
    }

    /**
     * 重写父类方法，解决Oracle不支持子查询别名使用AS关键字的问题
     * Oracle语法：SELECT COUNT(*) FROM (子查询) 别名   （注意：别名前没有AS）
     */
    @Override
    public Long pCount(String noPageSqlStatement, GirSqlParam sqlParam) {
        if (StrUtil.isEmpty(noPageSqlStatement)) {
            throw new IllegalArgumentException("分页统计SQL不能为空");
        }

        try {
            String cleanSql = getDialectTableNameProcessor().tbRemoveSqlSpaces(noPageSqlStatement);
            // Oracle专用：去掉子查询别名前的 AS 关键字
            String countSql = StrUtil.format(
                    "SELECT COUNT(*) AS count FROM ({}) {}",
                    cleanSql,
                    getDialectTableNameProcessor().tbGetTempAliasTableName()
            );
            return executeCountSqlWithParam(countSql, sqlParam);
        } catch (Exception e) {
            log.error("Oracle带参数分页统计失败，SQL: {}, 参数: {}", noPageSqlStatement, sqlParam, e);
            throw new RuntimeException("Oracle带参数分页统计异常: " + e.getMessage(), e);
        }
    }

    /**
     * 重写分页方法，同样需要处理子查询别名问题
     */
    @Override
    public PageApo<GirAdvOneRow> pPage(
            String noPageSqlStatement,
            GirSqlParam sqlParam,
            int pageNum,
            int pageSize,
            boolean pageNumStartZero,
            AdvEnumsGeomOpt advEnumsGeomOpt,
            boolean hasFieldsInfo,
            java.util.List<OrderApo> orders) {

        validateFullPageParams(noPageSqlStatement, pageNum, pageSize, pageNumStartZero, orders);

        // 获取字段元数据
        DataFieldsApo dataFieldsApo = null;
        try {
            dataFieldsApo = getColumnsBySQLWithParam(noPageSqlStatement, sqlParam);
        } catch (Exception e) {
            log.error("查询带参数SQL字段元数据失败，SQL：{}，参数：{}", noPageSqlStatement, sqlParam, e);
            throw new RuntimeException("获取字段信息异常：" + e.getMessage(), e);
        }

        // 重构SQL（去掉子查询的AS关键字）
        java.util.List<String> fieldNames = dataFieldsApo.getFieldList(FieldBySchemaApo::getColumnName, true);
        java.util.List<String> geomFieldNameList = dataFieldsApo.getGeomFieldNameList();
        String quotedFields = fieldNames.stream().map(this::quoteFieldName).collect(java.util.stream.Collectors.joining(", "));
        String tableAlias = getDialectTableNameProcessor().tbGetTempAliasTableName();

        // Oracle专用：去掉 AS
        String refactorNoPageSql = StrUtil.format(
                "SELECT {} FROM ({}) {}",
                quotedFields,
                getDialectTableNameProcessor().tbRemoveSqlSpaces(noPageSqlStatement),
                tableAlias
        );

        String sqlWithOrder = pBuildSqlWithOrder(refactorNoPageSql, orders, tableAlias);

        // 统计总数（调用重写后的pCount）
        long total = pCount(sqlWithOrder, sqlParam);

        // 计算分页参数
        long offset = calculateOffset(pageNum, pageSize, pageNumStartZero);
        int lastPageNum = calculateLastPageNum(total, pageSize);

        // 构建分页SQL（使用Oracle方言）
        String pageSql = getDialectTableNameProcessor().tbBuildPageSql(sqlWithOrder, pageSize, offset);

        // 执行分页查询
        java.util.List<GirAdvOneRow> records = getAdvGeoPreOpt().eSelectList(pageSql, sqlParam, advEnumsGeomOpt, geomFieldNameList);

        // 构建分页结果
        cn.geoair.map.dynamic.adv.query.apo.PageApo<GirAdvOneRow> pageApo =
                GirAdvQueryCommonUtils.createPageApo(
                        total, pageNum, pageSize, pageNumStartZero, lastPageNum, offset, records);

        if (hasFieldsInfo) {
            pageApo.setDataFieldsApo(dataFieldsApo);
        }

        return pageApo;
    }

    /**
     * 执行带参数的统计SQL（复用父类方法）
     */
    protected Long executeCountSqlWithParam(String countSql, GirSqlParam sqlParam) {
        GirAdvOneRow result = getAdvBaseOpt().bSelectOne(countSql, sqlParam);
        return result != null ? result.getLong("COUNT") : 0L;
    }
}
