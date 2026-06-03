package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.config.AdvQueryGlobalConfig;
import cn.geoair.map.dynamic.adv.query.*;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvSqlComposer;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvSqlComposer.SqlBuildResult;
import cn.geoair.map.dynamic.adv.query.wherequery.queryr.QueryRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * 构造函数
     *
     * @param dataSourceGetter 数据源获取器
     */
    public AbstractExecAdvWhereSelectOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    /**
     * 获取数据库方言处理器
     */
    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();

    /**
     * 获取基础查询实现
     */
    protected abstract IAdvBaseSelectOpt getBaseSelectOpt();

    protected abstract IAdvSimplePageOpt getSimplePageOpt();

    protected abstract IAdvGeoPreOpt getGeoOpt();

    @Override
    public AdvQueryGlobalConfig getConfig() {
        return getBaseSelectOpt().getConfig();
    }

    /**
     * 获取SQL构建器
     */
    protected GirAdvSqlComposer getSqlBuilder() {
        return new GirAdvSqlComposer(getDialectTableNameProcessor(), dataSourceGetter);
    }


    @Override
    public GirAdvOneRow wSelectOne(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getGeoOpt().eSelectOne(result.getSql(), result.getParams(), query.getAdvEnumsGeomOpt());
    }

    @Override
    public <T> GirAdvOneRow wSelectOne(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(entityClass);
        consumer.accept(builder);
        return wSelectOne(builder.build());
    }

    @Override
    public <T> GirAdvOneRow wSelectOne(Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(null);
        consumer.accept(builder);
        return wSelectOne(builder.build());
    }

    @Override
    public List<GirAdvOneRow> wSelectList(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getGeoOpt().eSelectList(result.getSql(), result.getParams(), query.getAdvEnumsGeomOpt());
    }

    @Override
    public <T> List<GirAdvOneRow> wSelectList(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(entityClass);
        consumer.accept(builder);
        return wSelectList(builder.build());
    }

    @Override
    public <T> List<GirAdvOneRow> wSelectList(Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(null);
        consumer.accept(builder);
        return wSelectList(builder.build());
    }

    @Override
    public PageApo<GirAdvOneRow> wSelectPage(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResultToPage(query);
        return getSimplePageOpt()
                .pPage(result.getSql(), result.getParams(),
                        query.getPageNum() == null ? query.getPageNumStartZero() ? 0 : 1 : query.getPageNum(),
                        query.getPageSize() == null ? 25 : query.getPageSize(),
                        query.getPageNumStartZero(),
                        query.getAdvEnumsGeomOpt(), query.getHasFieldsInfo(), query.getOrders(), query.getAdvEnumsKeyTran());
    }

    @Override
    public <T> PageApo<GirAdvOneRow> wSelectPage(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(entityClass);
        consumer.accept(builder);
        return wSelectPage(builder.build());
    }

    @Override
    public <T> PageApo<GirAdvOneRow> wSelectPage(Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(null);
        consumer.accept(builder);
        return wSelectPage(builder.build());
    }

    @Override
    public void wSelectStream(GirAdvQueryRequest query, Consumer<GirAdvOneRow> rowConsumer) {
        SqlBuildResult result = getSqlBuildResult(query);
        getBaseSelectOpt().bSelectListStream(result.getSql(), result.getParams(), rowConsumer);
    }

    @Override
    public <T> void wSelectStream(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer, Consumer<GirAdvOneRow> rowConsumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(entityClass);
        consumer.accept(builder);
        wSelectStream(builder.build(), rowConsumer);
    }

    @Override
    public <T> void wSelectStream(Consumer<QueryRequestBuilder<T>> consumer, Consumer<GirAdvOneRow> rowConsumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(null);
        consumer.accept(builder);
        wSelectStream(builder.build(), rowConsumer);
    }

    @Override
    public Number wSelectCount(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectRecordRowCount(result.getSql(), result.getParams());
    }

    @Override
    public <T> Number wSelectCount(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(entityClass);
        consumer.accept(builder);
        return wSelectCount(builder.build());
    }

    @Override
    public <T> Number wSelectCount(Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(null);
        consumer.accept(builder);
        return wSelectCount(builder.build());
    }

    @Override
    public List<List<Object>> wSelectListToValueList(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        //  暂时不支持空间操作
        return getBaseSelectOpt().bSelectListToValueList(result.getSql(), result.getParams());
    }

    @Override
    public <T> List<List<Object>> wSelectListToValueList(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(entityClass);
        consumer.accept(builder);
        return wSelectListToValueList(builder.build());
    }

    @Override
    public <T> List<List<Object>> wSelectListToValueList(Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(null);
        consumer.accept(builder);
        return wSelectListToValueList(builder.build());
    }

    @Override
    public Number wSelectNumber(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectNumber(result.getSql(), result.getParams());
    }

    @Override
    public <T> Number wSelectNumber(Class<T> entityClass, Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(entityClass);
        consumer.accept(builder);
        return wSelectNumber(builder.build());
    }

    @Override
    public <T> Number wSelectNumber(Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(null);
        consumer.accept(builder);
        return wSelectNumber(builder.build());
    }

    @Override
    public <E> E wSelectObjOne(GirAdvQueryRequest query, Class<E> clazz) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectObjOne(result.getSql(), result.getParams(), clazz);
    }

    @Override
    public <T, R> R wSelectObjOne(Class<T> entityClass, Class<R> resultClass, Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(entityClass);
        consumer.accept(builder);
        return wSelectObjOne(builder.build(), resultClass);
    }

    @Override
    public <T, R> R wSelectObjOne(Class<R> resultClass, Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(null);
        consumer.accept(builder);
        return wSelectObjOne(builder.build(), resultClass);
    }

    @Override
    public <E> List<E> wSelectObjList(GirAdvQueryRequest query, Class<E> clazz) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectObjList(result.getSql(), result.getParams(), clazz);
    }

    @Override
    public <T, R> List<R> wSelectObjList(Class<T> entityClass, Class<R> resultClass, Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(entityClass);
        consumer.accept(builder);
        return wSelectObjList(builder.build(), resultClass);
    }

    @Override
    public <T, R> List<R> wSelectObjList(Class<R> resultClass, Consumer<QueryRequestBuilder<T>> consumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(null);
        consumer.accept(builder);
        return wSelectObjList(builder.build(), resultClass);
    }

    @Override
    public <E> void wSelectObjStream(GirAdvQueryRequest query, Class<E> clazz, Consumer<E> rowConsumer) {
        SqlBuildResult result = getSqlBuildResult(query);
        //  暂时不支持空间操作
        getBaseSelectOpt().bSelectObjListStream(result.getSql(), result.getParams(), clazz, rowConsumer);
    }

    @Override
    public <T, R> void wSelectObjStream(Class<T> entityClass, Class<R> resultClass, Consumer<QueryRequestBuilder<T>> consumer, Consumer<R> rowConsumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(entityClass);
        consumer.accept(builder);
        wSelectObjStream(builder.build(), resultClass, rowConsumer);
    }

    @Override
    public <T, R> void wSelectObjStream(Class<R> resultClass, Consumer<QueryRequestBuilder<T>> consumer, Consumer<R> rowConsumer) {
        QueryRequestBuilder<T> builder = GirAdvQueryRequest.builder(null);
        consumer.accept(builder);
        wSelectObjStream(builder.build(), resultClass, rowConsumer);
    }

    private SqlBuildResult getSqlBuildResult(GirAdvQueryRequest query) {
        GirAdvSqlComposer sqlBuilder = getSqlBuilder();
        return sqlBuilder.buildPageSql(query);
    }

    private SqlBuildResult getSqlBuildResultToPage(GirAdvQueryRequest query) {
        GirAdvSqlComposer sqlBuilder = getSqlBuilder();
        return sqlBuilder.buildSelectSql(query);
    }
}
