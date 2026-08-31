package cn.geoair.base.gpa.dao;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.gpa.entity.GiEntityQueryable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * 分页查询
 *
 * @author Ray
 */
public interface GiPagerDao<M extends GiEntityQueryable<PK>, PK extends Serializable>
        extends GiDao<M, PK> {

    @SuppressWarnings({"unchecked"})
    public static <M extends GiEntityQueryable<PK>, PK extends Serializable>
            GiPagerDao<M, PK> getDao(Class<M> modelCls) {
        return GiDao.getDao(GiPagerDao.class, modelCls);
    }

    default GiPager<M> getModelPager() {
        Class<M> modelClass = this.getModelClass();
        return GiPager.ofClass(modelClass);
    }

    /**
     * 分页查询单表所有记录
     *
     * @param pageParam 分页参数
     * @return 分页结果
     */
    GiPager<M> gtcSearchPageAll(GiPageParam pageParam);

    /**
     * 根据PO条件分页查询多条记录
     *
     * @param t 查询条件对象
     * @param pageParam 分页参数
     * @return 分页结果
     */
    GiPager<M> gtcSearchPage(M t, GiPageParam pageParam);

    /**
     * 根据主键列表分页查询
     *
     * @param pks 主键列表
     * @param pageParam 分页参数
     * @return 分页结果
     */
    GiPager<M> gtcSearchPageByPK(List<PK> pks, GiPageParam pageParam);

    /**
     * 根据主键数组分页查询
     *
     * @param pks 主键数组
     * @param pageParam 分页参数
     * @return 分页结果
     */
    default GiPager<M> gtcSearchPageByPK(PK[] pks, GiPageParam pageParam) {
        return this.gtcSearchPageByPK(Arrays.asList(pks), pageParam);
    }

    /**
     * 根据Example条件分页查询多条记录
     *
     * @param example 查询条件对象
     * @return 分页结果
     */
    // default gtcPager<M> searchByExamplePage(Object example) {
    // Class<M> modelClass = this.getModelClass();
    // gtcRetrieveDao<M, PK> selectDao = gtcRetrieveDao.<M, PK>getDao(modelClass);
    // gtcPager<M> pager = this.get gtcPager(modelClass);
    // gtcPageExcute<M> exec = new gtcPageExcute<M>() {
    // @Override
    // public gtcPager<M> get gtcPager() {
    // return pager;
    // }
    //
    // @Override
    // public List<M> excute() {
    // return selectDao.search(example);
    // }
    // };
    //
    // return getPageExcuter().excutePage(exec, getPageParam());
    // }

}
