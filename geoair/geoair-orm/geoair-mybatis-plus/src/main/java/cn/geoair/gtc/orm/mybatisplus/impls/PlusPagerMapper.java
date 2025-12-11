package cn.geoair.gtc.orm.mybatisplus.impls;

import cn.geoair.gtc.base.data.page.GfunPageExcute;
import cn.geoair.gtc.base.data.page.GiPageExcuter;
import cn.geoair.gtc.base.data.page.GiPageParam;
import cn.geoair.gtc.base.data.page.GiPager;
import cn.geoair.gtc.base.gpa.dao.GiPagerDao;
import cn.geoair.gtc.base.gpa.dao.GiRetrieveDao;
import cn.geoair.gtc.base.gpa.entity.GiEntityQueryable;
import cn.geoair.gtc.orm.mybatisplus.page.PlusGithubPageHelper;


import java.io.Serializable;
import java.util.List;

/**
 * 分页查询
 *
 * @author Ray
 */
public interface PlusPagerMapper<M extends GiEntityQueryable<PK>, PK extends Serializable> extends GiPagerDao<M, PK> {


	/*
    public static <P, R>  gtcPager<R> excute( gtcParamPageExcute<P, R> excute, P param) {
        return TkPagerMapper.excute(excute, param, null);
    }

    public static <P, R>  gtcPager<R> excute( gtcParamPageExcute<P, R> excute, P param,  gtcPageParam pageParam) {
         gtcPageExcute<R> exec = new  gtcPageExcute<R>() {
            @Override
            public List<R> excute() {
                return excute.excute(param);
            }
        };
        return  gtcPageHelper.excute(exec, pageParam);
    }
    */


    /**
     * 获得分页处理者
     *
     * @return
     */
    default GiPageExcuter pageExcuter() {
        return PlusGithubPageHelper.getPageExcuter();
    }

    /*
    default  gtcPagerProvider getPagerProvider() {
        return  gtcBeanHelper.getBean( gtcPagerProvider.class);
    }

    default  gtcPager<M> get gtcPager(Class<M> modelCls) {
        if (modelCls == null) {
            modelCls = this.getModelClass();
        }
        return  gtcPageHelper.getPager(modelCls);
    }

    default  gtcPageParamProvider getPageParamProvider() {
        return  gtcBeanHelper.getBean( gtcPageParamProvider.class);
    }

    default  gtcPageParam getPageParam() {
        return this.getPageParamProvider().getPageParam();
    }
    */

    /**
     * 根据主键数组分页查询记录
     * @return default  gtcPager<M> selectByPKsPage(Object[] keys){
    Class<M> modelClass = this.getModelClass();
    gtcSelectDao<M,PK> selectDao =  gtcSelectDao.<M,PK>getDao(modelClass);
    gtcPager<M> pager = this.get gtcPager(modelClass);
    gtcPageExcute<M> exec = new  gtcPageExcute<M>() {
    @Override public  gtcPager<M> get gtcPager(){
    return pager;
    }
    @Override public List<M> excute() {
    return selectDao.selectByPKs(keys);
    }};

    return getPageExcuter().excutePage(exec,getPageParam());
    }*/
    /**
     * 分页查询单表所有记录
     *
     * @return
     */
    @Override
    default GiPager<M> gtcSearchPageAll(GiPageParam pageParam) {
        Class<M> modelClass = this.getModelClass();
        GiRetrieveDao<M, PK> selectDao = GiRetrieveDao.<M, PK>getDao(modelClass);
        GfunPageExcute<M> exec = new GfunPageExcute<M>() {
            @Override
            public Iterable<M> excute() {
                return selectDao.gtcSearchAll();
            }
        };

        return pageExcuter().excutePage(exec, pageParam);
    }

    /**
     * 根据PO条件分页查询多条记录
     *
     * @param t
     * @return
     */
    @Override
    default GiPager<M> gtcSearchPage(M t, GiPageParam pageParam) {
        Class<M> modelClass = this.getModelClass();
        GiRetrieveDao<M, PK> selectDao = GiRetrieveDao.<M, PK>getDao(modelClass);
        GfunPageExcute<M> exec = new GfunPageExcute<M>() {
            /**
             *
             */
            private static final long serialVersionUID = 5789221554389121763L;

            @Override
            public Iterable<M> excute() {
                return selectDao.gtcSearch(t);
            }
        };
        return pageExcuter().excutePage(exec, pageParam);
    }

    /**
     * 根据PO条件分页查询多条记录
     *
     * @param t
     * @return
     */
    @Override
    default GiPager<M> gtcSearchPageByPK(List<PK> pks, GiPageParam pageParam) {
        Class<M> modelClass = this.getModelClass();
        GiRetrieveDao<M, PK> selectDao = GiRetrieveDao.<M, PK>getDao(modelClass);
        GfunPageExcute<M> exec = new GfunPageExcute<M>() {
            /**
             *
             */
            private static final long serialVersionUID = 5789221554389121763L;

            @Override
            public Iterable<M> excute() {
                return selectDao.gtcSearchByPK(pks);
            }
        };
        return pageExcuter().excutePage(exec, pageParam);
    }


}
