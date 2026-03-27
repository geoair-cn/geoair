package cn.geoair.orm.mybatisplus.page;

import cn.geoair.base.Gir;
import cn.geoair.base.data.page.GfunPageExcute;
import cn.geoair.base.data.page.GiPageExcuter;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.gpa.support.GirSort;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;

/**
 * TkMapper + GitHubPager的组合工具
 *
 * @author Ray
 */
public class PlusGithubPageHelper implements GiPageExcuter {

    private static PlusGithubPageHelper instance = new PlusGithubPageHelper();

    /**
     * 获取分页执行器
     *
     * @return
     */
    public static GiPageExcuter getPageExcuter() {
        return instance;
    }

    private PlusGithubPageHelper() {
        super();
    }

    /**
     * 从gtcSort对象拼装 排序语句
     *
     * @param sort
     * @return
     */
    public static String orderBySqlFromGirSort(GirSort sort) {
        Gir.log.error("暂时不支持GirSort生成排序。");
        // if(sort != null) {
        //
        // Iterator<GirOrder<?>> iterator = sort.iterator();
        //
        // StringBuilder orderBy = new StringBuilder();
        // while(iterator.hasNext()) {
        //
        // GirOrder<?> order = iterator.next();
        //
        // if(order.getPropertyFun() != null) {
        //
        // GkfLambdaMeta lm = GutilLambda.extract(order.getPropertyFun());
        //
        // String methName = lm.getImplMethodName().toUpperCase();
        //
        // EntityColumn ec =
        // TkEntityHelper.getEntityColumnByMethodName(order.getEntityClass(), methName);
        //
        // if(ec != null) {
        // if (orderBy.length() != 0) {
        // orderBy.append(",");
        // }
        // orderBy.append(ec.getColumn()).append("
        // ").append(order.getDirection().value());
        // }
        // }else if(order.getProperty() != null) {
        //
        // EntityTable table = EntityHelper.getEntityTable(order.getEntityClass());
        //
        // loop2:for (EntityColumn column : table.getEntityClassColumns()) {
        //
        // if(order.getProperty().equalsIgnoreCase(column.getProperty())) {
        // if (orderBy.length() != 0) {
        // orderBy.append(",");
        // }
        // orderBy.append(column.getColumn()).append("
        // ").append(order.getDirection().value());
        // break loop2;
        // }
        // }
        // }
        // }
        //
        // if(orderBy.length() > 0) {
        // return orderBy.toString();
        // }
        // }

        return null;
    }

    @Override
    public <F> GiPager<F> excutePage(GfunPageExcute<F> pageExcute, GiPageParam pageParam) {

        // PageHelper.orderBy(TkGithubPageHelper.orderBySqlFromgtcSort(pageParam.sort()));
        Page<F> page =
                PageHelper.startPage(
                        pageParam.pageNum(),
                        pageParam.pageSize(),
                        PlusGithubPageHelper.orderBySqlFromGirSort(pageParam.sort()));
        // page.setOrderBy(TkGithubPageHelper.orderBySqlFromgtcSort(pageParam.sort()));

        Iterable<F> list = pageExcute.excute();
        PageInfo<F> pageInfo = new PageInfo<F>((List<F>) list);
        GiPager<F> pager = pageExcute.getgtcPager();
        pageParam.putParam(pageInfo.getPageSize(), pageInfo.getPageNum(), pageInfo.getStartRow());
        pager.put(pageInfo.getList(), pageInfo.getTotal(), pageParam);
        return pager;
        /*
         * pager.setList(list);//
         *
         * pager.setPageNum(pageInfo.getPageNum());//
         * pager.setPageSize(pageInfo.getPageSize());// pager.setSize(pageInfo.getSize());
         * pager.setStartRow(pageInfo.getStartRow());
         * pager.setEndRow(pageInfo.getEndRow());
         *
         * pager.setPages(pageInfo.getPages());// pager.setPrePage(pageInfo.getPrePage());
         * pager.setNextPage(pageInfo.getNextPage());
         * pager.setIsFirstPage(pageInfo.isIsFirstPage());
         * pager.setIsLastPage(pageInfo.isIsLastPage());
         * pager.setHasPreviousPage(pageInfo.isHasPreviousPage());
         * pager.setHasNextPage(pageInfo.isHasNextPage());
         * pager.setNavigatePages(pageInfo.getNavigatePages());
         * pager.setNavigatepageNums(pageInfo.getNavigatepageNums());
         * pager.setNavigateFirstPage(pageInfo.getNavigateFirstPage());
         * pager.setNavigateLastPage(pageInfo.getNavigateLastPage());
         * pager.setTotal(pageInfo.getTotal());// return pager;
         */
    }
}
