package cn.geoair.map.dynamic.adv.query;

import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.wherequery.QueryRequest;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;


import java.util.List;

/**
 * 动态查询接口
 * <p>自动组装WHERE条件的查询</p>
 *
 * @author zhangjun
 * @date Created in 2026/4/16 09:28
 */
public interface IAdvSelectWhereOpt {

    /**
     * 查询列表
     *
     * @param query 查询请求对象
     * @return 查询结果列表
     */
    List<GirAdvOneRow> wSelectList(QueryRequest query);

    /**
     * 分页查询
     *
     * @param query 查询请求对象
     * @return 分页结果
     */
    PageApo<GirAdvOneRow> wSelectPage(QueryRequest query);

    /**
     * 查询单条记录
     *
     * @param query 查询请求对象
     * @return 单条记录，可能为null
     */
    GirAdvOneRow wSelectOne(QueryRequest query);

    /**
     * 统计记录数
     *
     * @param query 查询请求对象
     * @return 记录总数
     */
    int wSelectCount(QueryRequest query);


}
