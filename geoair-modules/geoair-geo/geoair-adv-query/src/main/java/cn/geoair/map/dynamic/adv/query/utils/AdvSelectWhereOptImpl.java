package cn.geoair.map.dynamic.adv.query.utils;


import cn.geoair.map.dynamic.adv.query.IAdvSelectWhereOpt;
import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.apo.QueryRequest;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;

import java.util.Collections;
import java.util.List;

/**
 * 动态查询接口实现类
 *
 * @author zhangjun
 */
public class AdvSelectWhereOptImpl implements IAdvSelectWhereOpt {


    @Override
    public List<GirAdvOneRow> wSelectList(QueryRequest query) {
        return Collections.emptyList();
    }

    @Override
    public PageApo<GirAdvOneRow> wSelectPage(QueryRequest query) {
        return null;
    }

    @Override
    public GirAdvOneRow wSelectOne(QueryRequest query) {
        return null;
    }

    @Override
    public int wSelectCount(QueryRequest query) {
        return 0;
    }
}
