package cn.geoair.orm.springjpa.impls;

import cn.geoair.base.gpa.dao.GiEntityDao;
import cn.geoair.base.gpa.entity.GiCrudEntity;
import java.io.Serializable;

public interface EntityRepository<T extends GiCrudEntity<PK>, PK extends Serializable>
        extends InsertRepository<T, PK>,
                DeleteRepository<T, PK>,
                UpdateRepository<T, PK>,
                RetrieveRepository<T, PK>,
                PagerRepository<T, PK>,
                GiEntityDao<T, PK> {}
