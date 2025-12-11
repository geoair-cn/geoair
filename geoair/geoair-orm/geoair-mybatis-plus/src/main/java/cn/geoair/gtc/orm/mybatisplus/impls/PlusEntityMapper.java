package cn.geoair.gtc.orm.mybatisplus.impls;


import java.io.Serializable;

import cn.geoair.gtc.base.gpa.entity.GiCrudEntity;
import cn.geoair.gtc.orm.mybatis.impls.MyBatisMapper;


public interface PlusEntityMapper<T extends GiCrudEntity<PK>,PK extends Serializable> extends PlusInsertMapper<T,PK>,PlusDeleteMapper<T,PK>,PlusUpdateMapper<T,PK>,PlusRetrieveMapper<T,PK>,PlusPagerMapper<T,PK>, MyBatisMapper<T,PK> {

}
