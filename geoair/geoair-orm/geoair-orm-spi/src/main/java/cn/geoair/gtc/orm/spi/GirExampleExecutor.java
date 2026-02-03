package cn.geoair.gtc.orm.spi;

import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.data.page.GiPager;
import cn.geoair.gtc.orm.spi.support.GirExample;

import java.util.List;

/**
 * @author ：张俊
 * @date ：Created in 2022/7/2 15:14
 * @description： ExampleExecutor   Example 执行器
 */
public interface GirExampleExecutor {

    /**
     * 根据Example查询总数
     *
     * @param
     * @return
     */
    <T> T selectCountByExample( GirExample girExample);

    /**
     * 根据Example删除
     *
     * @param  girExample
     * @return
     */
    <T> T deleteByExample( GirExample girExample);

    /**
     * 根据Example查询
     *
     * @param  girExample
     * @return
     */
     <E> List<E> selectByExample(E type,  GirExample girExample);

    /**
     * 根据Example查询
     *
     * @param  girExample
     * @return
     */
    <E> List<E> selectByExample( GirExample girExample);

    /**
     * 根据Example查询 分页查询
     *
     * @param  girExample
     * @return
     */
     <E> GiPager<E> selectPageByExample(E type, GirExample girExample, GiPageParam pageParam);

    /**
     * 根据Example查询 分页查询
     *
     * @param  girExample
     * @return
     */
    <E> GiPager<E> selectPageByExample(GirExample girExample, GiPageParam pageParam);

    /**
     * 根据Example查询
     *
     * @param  girExample
     * @return
     */
    <E> List<E> selectByExampleAndRowBounds( GirExample girExample);

    /**
     * 根据Example更新非null字段
     *
     * @param
     * @return
     */
    <T> T updateByExampleSelective(Object updateEntity,  GirExample girExample);

    /**
     * 根据Example更新
     *
     * @param
     * @return
     */
    <T> T updateByExample(Object updateEntity,  GirExample girExample);


}
