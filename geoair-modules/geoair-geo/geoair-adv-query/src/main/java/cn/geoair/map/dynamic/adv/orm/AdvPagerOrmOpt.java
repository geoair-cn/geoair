package cn.geoair.map.dynamic.adv.orm;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.gpa.dao.GiPagerDao;
import cn.geoair.base.gpa.entity.GiEntityQueryable;

import java.io.Serializable;
import java.util.List;

/**
 * 分页查询
 *
 * @author Ray
 */
public interface AdvPagerOrmOpt<M extends GiEntityQueryable<PK>, PK extends Serializable>
        extends GiPagerDao<M, PK> {

    /**
     * 分页查询单表所有记录
     *
     * @return
     */
    @Override
    default GiPager<M> gtcSearchPageAll(GiPageParam pageParam) {
        throw new UnsupportedOperationException();
    }

    /**
     * 根据PO条件分页查询多条记录
     *
     * @param t
     * @return
     */
    @Override
    default GiPager<M> gtcSearchPage(M t, GiPageParam pageParam) {
        throw new UnsupportedOperationException();
    }

    /**
     * 根据PO条件分页查询多条记录
     *
     * @param t
     * @return
     */
    @Override
    default GiPager<M> gtcSearchPageByPK(List<PK> pks, GiPageParam pageParam) {
        throw new UnsupportedOperationException();
    }
}
