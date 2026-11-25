package cn.geoair.orm.tkmapper.impls;

import cn.geoair.base.gpa.entity.GiCrudEntity;
import cn.geoair.orm.mybatis.impls.MyBatisMapper;

import java.io.Serializable;

public interface TkEntityMapper<T extends GiCrudEntity<PK>, PK extends Serializable>
        extends TkInsertMapper<T, PK>,
                TkDeleteMapper<T, PK>,
                TkUpdateMapper<T, PK>,
                TkRetrieveMapper<T, PK>,
                TkPagerMapper<T, PK>,
                MyBatisMapper<T, PK> /* ,MySqlMapper<T>,IdsMapper<T> */ {

    /**
     * 插入或者更新一条记录(属性不判空)
     *
     * @param t
     * @return
     */
    // int accessOrUpdate(T t);

    /**
     * 插入或者更新一条记录(属性判空)
     *
     * @param t
     * @return
     */
    // int accessOrUpdateSelective(T t);

}
