package cn.geoair.base.gpa.entity;

import cn.geoair.base.gpa.dao.GiUpdateDao;

import java.io.Serializable;

/**
 * 可以更新的实体接口
 *
 * @author Ray
 * @param <PK> 主键类型
 */
public interface GiEntityAlterable<PK extends Serializable> extends GiEntityable<PK> {

    /**
     * 获取更新DAO实例
     *
     * @param <M> 实体类型
     * @return 更新DAO实例
     */
    @SuppressWarnings("unchecked")
    default <M extends GiEntityAlterable<PK>> GiUpdateDao<M, PK> updateDao() {
        return GiUpdateDao.getDao((Class<M>) this.getClass());
    }

    /** 根据主键更新实体的所有字段 */
    default void alterByPK() {
        this.updateDao().gtcUpdateByPK(this);
    }

    /** 根据主键选择性更新实体字段（只更新非空字段） */
    default void alterByPKSelective() {
        this.updateDao().gtcUpdateByPKSelective(this);
    }
}
