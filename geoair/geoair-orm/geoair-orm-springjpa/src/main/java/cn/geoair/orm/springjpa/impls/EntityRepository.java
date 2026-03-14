package cn.geoair.orm.springjpa.impls;

import java.io.Serializable;
import cn.geoair.base.gpa.dao.GiEntityDao;
import cn.geoair.base.gpa.entity.GiCrudEntity;

public interface EntityRepository<T extends GiCrudEntity<PK>, PK extends Serializable>
		extends InsertRepository<T, PK>, DeleteRepository<T, PK>, UpdateRepository<T, PK>, RetrieveRepository<T, PK>,
		PagerRepository<T, PK>, GiEntityDao<T, PK> {

}
