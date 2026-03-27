package cn.geoair.base.data.page;

/**
 * 分页执行器
 *
 * @author Ray
 */
public interface GiPageExcuter {

    public <R> GiPager<R> excutePage(GfunPageExcute<R> pageExcute, GiPageParam pageParam);
}
