package cn.geoair.base.gpa.dao;

import cn.geoair.base.gpa.entity.GiCrudEntity;
import java.io.Serializable;

/**
 * 表实体Dao
 *
 * @author Ray
 * @param <M> 模型类型
 * @param <PK> 主键类型
 */
public interface GiEntityDao<M extends GiCrudEntity<PK>, PK extends Serializable>
        extends GiCreateDao<M, PK>,
                GiDeleteDao<M, PK>,
                GiUpdateDao<M, PK>,
                GiRetrieveDao<M, PK>,
                GiPagerDao<M, PK> {

    @SuppressWarnings("unchecked")
    public static <M extends GiCrudEntity<PK>, PK extends Serializable> GiEntityDao<M, PK> getDao(
            Class<M> modelCls) {
        return GiDao.getDao(GiEntityDao.class, modelCls);
    }

    /**
     * 插入或者更新一条记录(属性不判空)
     *
     * @param t
     * @return
     */
    // int gtcAccessOrUpdate(M t);

    /**
     * 插入或者更新一条记录(属性判空)
     *
     * @param t
     * @return
     */
    // int gtcAccessOrUpdateSelective(M t);

}
