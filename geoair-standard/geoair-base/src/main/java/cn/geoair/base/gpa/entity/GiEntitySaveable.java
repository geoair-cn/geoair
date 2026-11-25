package cn.geoair.base.gpa.entity;

import cn.geoair.base.gpa.dao.GiCreateDao;

import java.io.Serializable;

/**
 * 可以保存的模型
 *
 * @author Ray
 * @param <ID> 主键类型
 */
public interface GiEntitySaveable<ID extends Serializable> extends GiEntityable<ID> {

    @SuppressWarnings("unchecked")
    default <T extends GiEntitySaveable<ID>> GiCreateDao<T, ID> insertDao() {
        return GiCreateDao.getDao((Class<T>) this.modelClass());
    }

    default void save() {
        this.insertDao().gtcAccess(this);
    }

    default void saveSelective() {
        this.insertDao().gtcAccessSelective(this);
    }
}
