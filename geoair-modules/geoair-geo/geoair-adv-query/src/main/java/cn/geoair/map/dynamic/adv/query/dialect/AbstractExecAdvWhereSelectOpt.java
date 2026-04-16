package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.mybatis.SqlMeta;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvWhereSelectOpt;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.enums.AdvEnumsGeomOpt;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryFilter;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQuerySqlBuilder;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQuerySqlBuilder.SqlBuildResult;
import cn.hutool.db.Entity;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.sql.SqlExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
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
            GirAdvQuerySqlBuilder sqlBuilder = getSqlBuilder();
            SqlBuildResult result = sqlBuilder.buildSelectSql(query);
            log.debug("执行查询SQL: {}, 参数: {}", result.getSql(), result.getParams());
            return getBaseSelectOpt().bSelectList(result.getSql(), result.getParams());
        } catch (Exception e) {
            log.error("执行查询失败", e);
            throw new RuntimeException("执行查询失败: " + e.getMessage(), e);
        }
    }

    @Override
    public PageApo<GirAdvOneRow> wSelectPage(GirAdvQueryRequest query) {
        return null;
    }

    @Override
    public int wSelectCount(GirAdvQueryRequest query) {
        return 0;
    }

    /**
     * 流式查询
     *
     * @param query       查询请求
     * @param rowConsumer 行数据消费器
     */
    public void wSelectStream(GirAdvQueryRequest query, Consumer<GirAdvOneRow> rowConsumer) {

    }


}
