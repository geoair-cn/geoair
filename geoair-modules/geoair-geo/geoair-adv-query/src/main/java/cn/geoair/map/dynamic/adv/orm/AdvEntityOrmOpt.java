package cn.geoair.map.dynamic.adv.orm;

import cn.geoair.base.gpa.entity.GiCrudEntity;
import java.io.Serializable;

public interface AdvEntityOrmOpt<T extends GiCrudEntity<PK>, PK extends Serializable>
        extends AdvInsertOrmOpt<T, PK>,
                AdvDeleteOrmOpt<T, PK>,
                AdvUpdateOrmOpt<T, PK>,
                AdvRetrieveOrmOpt<T, PK>,
                AdvPagerOrmOpt<T, PK> {}
