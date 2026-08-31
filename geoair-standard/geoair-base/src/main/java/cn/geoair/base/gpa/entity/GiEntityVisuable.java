package cn.geoair.base.gpa.entity;

import cn.geoair.base.data.model.GiVisualModelable;
import cn.geoair.base.gpa.dao.GiVisualSelectDao;

import java.io.Serializable;

public interface GiEntityVisuable<PK extends Serializable>
        extends GiEntityQueryable<PK>, GiVisualModelable<PK> {

    @SuppressWarnings("unchecked")
    default <E extends GiEntityVisuable<PK>> GiVisualSelectDao<E, PK> visualSelectDao() {
        return GiVisualSelectDao.getDao((Class<E>) this.getClass());
    }
}
