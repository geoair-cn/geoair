package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvWhereSelectOpt;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQuerySqlBuilder;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQuerySqlBuilder.SqlBuildResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 动态查询抽象实现类
 * <p>基于 IAdvBaseSelectOpt 实现，复用基础查询能力</p>
 *
 * @author 张俊
 * @date Created in 2026/4/16 12:03
 */
public abstract class AbstractExecAdvWhereSelectOpt implements IAdvWhereSelectOpt {

    private static final Logger log = LoggerFactory.getLogger(AbstractExecAdvWhereSelectOpt.class);

    protected IDataSourceGetter dataSourceGetter;
    protected DialectTableNameProcessor dialectTableNameProcessor;
    protected IAdvBaseSelectOpt baseSelectOpt;

    /**
     * 构造函数
     *
     * @param dataSourceGetter 数据源获取器
     */
    public AbstractExecAdvWhereSelectOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
        this.dialectTableNameProcessor = getDialectTableNameProcessor();
        this.baseSelectOpt = getBaseSelectOpt();
        if (this.baseSelectOpt != null) {
            this.baseSelectOpt.setDataSourceGetter(dataSourceGetter);
        }
    }

    /**
     * 获取数据库方言处理器（由子类实现）
     */
    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();

    /**
     * 获取基础查询实现（由子类实现）
     */
    protected abstract IAdvBaseSelectOpt getBaseSelectOpt();

    /**
     * 获取SQL构建器
     */
    protected GirAdvQuerySqlBuilder getSqlBuilder() {
        return new GirAdvQuerySqlBuilder(dialectTableNameProcessor, dataSourceGetter);
    }

    /**
     * 执行原始SQL查询（无参数）
     *
     * @param sql 原始SQL
     * @return 查询结果列表
     */
    protected List<GirAdvOneRow> executeQuery(String sql) {
        if (baseSelectOpt == null) {
            throw new IllegalStateException("baseSelectOpt is not initialized");
        }
        return baseSelectOpt.bSelectList(sql);
    }

    /**
     * 执行原始SQL查询（带参数）
     *
     * @param sql    原始SQL
     * @param params 参数
     * @return 查询结果列表
     */
    protected List<GirAdvOneRow> executeQuery(String sql, SqlParamMap params) {
        if (baseSelectOpt == null) {
            throw new IllegalStateException("baseSelectOpt is not initialized");
        }
        return baseSelectOpt.bSelectList(sql, params);
    }

    /**
     * 执行计数查询
     *
     * @param sql    计数SQL
     * @param params 参数
     * @return 记录数
     */
    protected Number executeCount(String sql, SqlParamMap params) {
        if (baseSelectOpt == null) {
            throw new IllegalStateException("baseSelectOpt is not initialized");
        }
        return baseSelectOpt.bSelectNumber(sql, params);
    }

    @Override
    public List<GirAdvOneRow> wSelectList(GirAdvQueryRequest query) {
        try {


            // 构建SQL
            GirAdvQuerySqlBuilder sqlBuilder = getSqlBuilder();
            SqlBuildResult result = sqlBuilder.buildSelectSql(query);

            log.debug("执行查询SQL: {}, 参数: {}", result.getSql(), result.getParams());

            // 转换参数格式
            SqlParamMap paramMap = convertToSqlParamMap(result.getParams());

            // 执行查询
            List<GirAdvOneRow> list = executeQuery(result.getSql(), paramMap);

            log.debug("查询结果数量: {}", list.size());
            return list;

        } catch (Exception e) {
            log.error("执行查询失败", e);
            throw new RuntimeException("执行查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PageApo<GirAdvOneRow> wSelectPage(GirAdvQueryRequest query) {
        return null;
//        try {
//
//            GirAdvQuerySqlBuilder sqlBuilder = getSqlBuilder();
//
//            // 1. 查询总数
//            SqlBuildResult countResult = sqlBuilder.buildCountSql(query);
//            log.debug("执行总数查询SQL: {}, 参数: {}", countResult.getSql(), countResult.getParams());
//
//            SqlParamMap countParamMap = convertToSqlParamMap(countResult.getParams());
//            Number totalNum = executeCount(countResult.getSql(), countParamMap);
//
//            long total = totalNum != null ? totalNum.longValue() : 0L;
//
//
//
//            // 2. 分页查询数据
//            SqlBuildResult dataResult = sqlBuilder.buildPageSql(query);
//            log.debug("执行分页查询SQL: {}, 参数: {}", dataResult.getSql(), dataResult.getParams());
//
//            SqlParamMap dataParamMap = convertToSqlParamMap(dataResult.getParams());
//            List<GirAdvOneRow> data = executeQuery(dataResult.getSql(), dataParamMap);
//
//            // 3. 构建分页结果
//            int pageNum = query.getActualPageNum();
//            int pageSize = query.getPageSize();
//
//            PageApo<GirAdvOneRow> pageResult = PageApo.of(data, (int) total, pageNum, pageSize);
//            log.debug("分页查询结果: total={}, pageNum={}, pageSize={}, dataSize={}",
//                    total, pageNum, pageSize, data.size());
//
//            return pageResult;
//
//        } catch (Exception e) {
//            log.error("执行分页查询失败", e);
//            throw new RuntimeException("执行分页查询失败: " + e.getMessage(), e);
//        }
    }

    @Override
    public int wSelectCount(GirAdvQueryRequest query) {
        try {


            // 构建计数SQL
            GirAdvQuerySqlBuilder sqlBuilder = getSqlBuilder();
            SqlBuildResult result = sqlBuilder.buildCountSql(query);

            log.debug("执行计数查询SQL: {}, 参数: {}", result.getSql(), result.getParams());

            // 执行查询
            SqlParamMap paramMap = convertToSqlParamMap(result.getParams());
            Number count = executeCount(result.getSql(), paramMap);

            int total = count != null ? count.intValue() : 0;
            log.debug("计数结果: {}", total);
            return total;

        } catch (Exception e) {
            log.error("执行计数查询失败", e);
            throw new RuntimeException("执行计数查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 流式查询
     *
     * @param query       查询请求
     * @param rowConsumer 行数据消费器
     */
    public void wSelectStream(GirAdvQueryRequest query, Consumer<GirAdvOneRow> rowConsumer) {
        try {
            // 设置数据源获取器
            if (query.getDataSourceGetter() == null) {
                query.setDataSourceGetter(dataSourceGetter);
            }

            // 构建SQL
            GirAdvQuerySqlBuilder sqlBuilder = getSqlBuilder();
            SqlBuildResult result = sqlBuilder.buildSelectSql(query);

            log.debug("执行流式查询SQL: {}, 参数: {}", result.getSql(), result.getParams());

            // 转换参数格式
            SqlParamMap paramMap = convertToSqlParamMap(result.getParams());

            // 执行流式查询
            if (baseSelectOpt != null) {
                baseSelectOpt.bSelectList(result.getSql(), paramMap, rowConsumer);
            } else {
                throw new IllegalStateException("baseSelectOpt is not initialized");
            }

            log.debug("流式查询完成");

        } catch (Exception e) {
            log.error("执行流式查询失败", e);
            throw new RuntimeException("执行流式查询失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量查询
     *
     * @param queries 查询请求列表
     * @return 查询结果列表
     */
    public List<List<GirAdvOneRow>> wSelectBatch(List<GirAdvQueryRequest> queries) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyList();
        }
        return queries.stream()
                .map(this::wSelectList)
                .toList();
    }

    /**
     * 将参数列表转换为 SqlParamMap
     *
     * @param params 参数列表
     * @return SqlParamMap
     */
    private SqlParamMap convertToSqlParamMap(List<Object> params) {
        SqlParamMap paramMap = SqlParamMap.of();
        if (params == null || params.isEmpty()) {
            return paramMap;
        }

        // 使用索引作为参数名
        for (int i = 0; i < params.size(); i++) {
            paramMap.addOne("param" + i, params.get(i));
        }
        return paramMap;
    }

    /**
     * 检查记录是否存在
     *
     * @param query 查询请求
     * @return true=存在，false=不存在
     */
    public boolean wSelectExists(GirAdvQueryRequest query) {
        return wSelectCount(query) > 0;
    }
}
