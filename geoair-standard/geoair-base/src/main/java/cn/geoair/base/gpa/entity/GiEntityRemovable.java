package cn.geoair.base.gpa.entity;

import cn.geoair.base.gpa.dao.GiDeleteDao;

import java.io.Serializable;

/**
 * 可以被移除的模型
 *
 * @author Ray
 * @param <ID> 主键类型
 */
public interface GiEntityRemovable<ID extends Serializable> extends GiEntityable<ID> {

    @SuppressWarnings("unchecked")
    default <T extends GiEntityRemovable<ID>> GiDeleteDao<T, ID> deleteDao() {
        return GiDeleteDao.getDao((Class<T>) this.modelClass());
    }

    default void removeSelf() {
        this.deleteDao().gtcDeleteByPK(this.id());
    }

    default int removeBySelf() {
        return this.deleteDao().gtcDeleteBy(this);
    }
}
