package cn.geoair.gtc.orm.spi;

import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.data.page.GiPager;
import cn.geoair.gtc.orm.spi.support.GtcExample;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2022/7/2 15:14
 * @description： ExampleExecutor   Example 执行器
 */
public interface GtcExampleExecutor {

    /**
     * 根据Example查询总数
     *
     * @param
     * @return
     */
    <T> T selectCountByExample( GtcExample gtcExample);

    /**
     * 根据Example删除
     *
     * @param  gtcExample
     * @return
     */
    <T> T deleteByExample( GtcExample gtcExample);

    /**
     * 根据Example查询
     *
     * @param  gtcExample
     * @return
     */
     <E> List<E> selectByExample(E type,  GtcExample gtcExample);

    /**
     * 根据Example查询
     *
     * @param  gtcExample
     * @return
     */
    <E> List<E> selectByExample( GtcExample gtcExample);

    /**
     * 根据Example查询 分页查询
     *
     * @param  gtcExample
     * @return
     */
     <E> GiPager<E> selectPageByExample(E type, GtcExample gtcExample, GiPageParam pageParam);

    /**
     * 根据Example查询 分页查询
     *
     * @param  gtcExample
     * @return
     */
    <E> GiPager<E> selectPageByExample(GtcExample gtcExample, GiPageParam pageParam);

    /**
     * 根据Example查询
     *
     * @param  gtcExample
     * @return
     */
    <E> List<E> selectByExampleAndRowBounds( GtcExample gtcExample);

    /**
     * 根据Example更新非null字段
     *
     * @param
     * @return
     */
    <T> T updateByExampleSelective(Object updateEntity,  GtcExample gtcExample);

    /**
     * 根据Example更新
     *
     * @param
     * @return
     */
    <T> T updateByExample(Object updateEntity,  GtcExample gtcExample);


}
