package cn.geoair.orm.mybatisplus.impls;

import cn.geoair.base.gpa.entity.GiCrudEntity;
import cn.geoair.orm.mybatis.impls.MyBatisMapper;
import java.io.Serializable;

public interface PlusEntityMapper<T extends GiCrudEntity<PK>, PK extends Serializable>
        extends PlusInsertMapper<T, PK>,
                PlusDeleteMapper<T, PK>,
                PlusUpdateMapper<T, PK>,
                PlusRetrieveMapper<T, PK>,
                PlusPagerMapper<T, PK>,
                MyBatisMapper<T, PK> {}
