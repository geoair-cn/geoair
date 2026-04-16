package cn.geoair.map.dynamic.adv.query.dialect;

import cn.geoair.comp.dynamic.ds.IDataSourceGetter;
import cn.geoair.map.dynamic.adv.query.DialectTableNameProcessor;
import cn.geoair.map.dynamic.adv.query.IAdvBaseSelectOpt;
import cn.geoair.map.dynamic.adv.query.IAdvWhereSelectOpt;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;

import java.util.Collections;
import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2026/4/16 12:03
 * @description：
 */
public abstract class AbstractExecAdvWhereSelectOpt implements IAdvWhereSelectOpt {

    protected IDataSourceGetter dataSourceGetter;

    protected DialectTableNameProcessor dialectTableNameProcessor;


    protected abstract DialectTableNameProcessor getDialectTableNameProcessor();


    public AbstractExecAdvWhereSelectOpt(IDataSourceGetter dataSourceGetter) {
        this.dataSourceGetter = dataSourceGetter;
    }


    @Override
    public List<GirAdvOneRow> wSelectList(GirAdvQueryRequest query) {
        return Collections.emptyList();
    }

    @Override
    public PageApo<GirAdvOneRow> wSelectPage(GirAdvQueryRequest query) {
        return null;
    }

    @Override
    public int wSelectCount(GirAdvQueryRequest query) {
        return 0;
    }
}
