package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvSimplePageOpt;
import cn.geoair.map.dynamic.adv.query.IAdvWhereSelectOpt;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamList;
import cn.geoair.map.dynamic.adv.query.apo.SqlParamMap;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQuerySqlBuilder;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQuerySqlBuilder.SqlBuildResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
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

    /**
     * 构造函数
     *
     * @param dataSourceGetter 数据源获取器
     */
    public AbstractExecAdvWhereSelectOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }

    /**
     * 获取数据库方言处理器（由子类实现）
     */
    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();

    /**
     * 获取基础查询实现（由子类实现）
     */
    protected abstract IAdvBaseSelectOpt getBaseSelectOpt();

    protected abstract IAdvSimplePageOpt getSimplePageOpt();

    /**
     * 获取SQL构建器
     */
    protected GirAdvQuerySqlBuilder getSqlBuilder() {
        return new GirAdvQuerySqlBuilder(getDialectTableNameProcessor(), dataSourceGetter);
    }


    @Override
    public GirAdvOneRow wSelectOne(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectOne(result.getSql(), result.getParams());
    }

    @Override
    public List<GirAdvOneRow> wSelectList(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectList(result.getSql(), result.getParams());
    }

    @Override
    public PageApo<GirAdvOneRow> wSelectPage(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getSimplePageOpt()
                .pPage(result.getSql(),
                        query.getPageNum(),
                        query.getPageSize(),
                        query.getPageNumStartZero(),
                        query.getAdvEnumsGeomOpt(), query.getHasFieldsInfo());

    }

    @Override
    public Number wSelectCount(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectRecordRowCount(result.getSql(), result.getParams());
    }


    @Override
    public void wSelectStream(GirAdvQueryRequest query, Consumer<GirAdvOneRow> rowConsumer) {
        SqlBuildResult result = getSqlBuildResult(query);
        getBaseSelectOpt().bSelectListStream(result.getSql(), result.getParams(), rowConsumer);
    }

    @Override
    public List<List<Object>> wSelectListToValueList(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectListToValueList(result.getSql(), result.getParams());
    }

    @Override
    public Number wSelectNumber(GirAdvQueryRequest query) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectNumber(result.getSql(), result.getParams());
    }

    @Override
    public <E> E wSelectObjOne(GirAdvQueryRequest query, Class<E> clazz) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectObjOne(result.getSql(), result.getParams(), clazz);
    }

    @Override
    public <E> List<E> wSelectObjList(GirAdvQueryRequest query, Class<E> clazz) {
        SqlBuildResult result = getSqlBuildResult(query);
        return getBaseSelectOpt().bSelectObjList(result.getSql(), result.getParams(), clazz);
    }

    @Override
    public <E> void wSelectObjStream(GirAdvQueryRequest query, Class<E> clazz, Consumer<E> rowConsumer) {
        SqlBuildResult result = getSqlBuildResult(query);
        getBaseSelectOpt().bSelectObjListStream(result.getSql(), result.getParams(), clazz, rowConsumer);
    }

    private SqlBuildResult getSqlBuildResult(GirAdvQueryRequest query) {
        GirAdvQuerySqlBuilder sqlBuilder = getSqlBuilder();
        return sqlBuilder.buildSelectSql(query);
    }

}
