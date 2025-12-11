package cn.geoair.gtc.orm.springjpa.impls;

import java.io.Serializable;
import cn.geoair.gtc.base.gpa.dao.GiEntityDao;
import cn.geoair.gtc.base.gpa.entity.GiCrudEntity;


public interface EntityRepository<T extends GiCrudEntity<PK>,PK extends Serializable> extends InsertRepository<T,PK>,DeleteRepository<T,PK>,UpdateRepository<T,PK>,RetrieveRepository<T,PK>,PagerRepository<T,PK>,GiEntityDao<T,PK>{






}
