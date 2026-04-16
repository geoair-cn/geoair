package cn.geoair.map.dynamic.adv.query;

import cn.geoair.map.dynamic.adv.query.apo.PageApo;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.result.GirAdvOneRow;


import java.util.List;

/**
 * 动态查询接口
 * <p>自动组装WHERE条件的查询</p>
 *
 * @author zhangjun
 * @date Created in 2026/4/16 09:28
 */
public interface IAdvWhereSelectOpt {

    /**
     * 查询列表
     *
     * @param query 查询请求对象
     * @return 查询结果列表
     */
    List<GirAdvOneRow> wSelectList(GirAdvQueryRequest query);

    /**
     * 查询列表  返回对象
     *
     * @param query 查询请求对象
     * @return 查询结果列表
     */
    <T> List<T> wSelectListObj(GirAdvQueryRequest query, Class<T> objClass);

    /**
     * 分页查询
     *
     * @param query 查询请求对象
     * @return 分页结果
     */
    PageApo<GirAdvOneRow> wSelectPage(GirAdvQueryRequest query);


    /**
     * 统计记录数
     *
     * @param query 查询请求对象
     * @return 记录总数
     */
    int wSelectCount(GirAdvQueryRequest query);


}
