package cn.geoair.map.dynamic.adv.orm;

import cn.geoair.base.gpa.dao.GiCreateDao;
import cn.geoair.base.gpa.entity.GiEntitySaveable;
import cn.geoair.map.dynamic.adv.spring.GirSpringAdvExecutor;


import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public interface AdvInsertOrmOpt<T extends GiEntitySaveable<PK>, PK extends Serializable>
        extends GiCreateDao<T, PK> {

    /**
     * 保存一条记录(属性不判空，为空的属性插入为空，无视数据库默认值)
     *
     * @param t
     * @return
     */
    @Override
    default PK gtcAccess(T t) {
        GirSpringAdvExecutor.getInstance().bInsertOne(t);
        return t.id();
    }

    /**
     * 批量插入(属性不判空)各Mapper实现不同的InsertListMapper接口
     *
     * @param records
     * @return
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    default List<PK> gtcAccess(List<T> records) {
        GirSpringAdvExecutor.getInstance().bInsertBatch(records);
        return records.stream().map(T::id).collect(Collectors.toList());
    }

    /**
     * 插入一条记录(属性判空，为空的属性不做插入操作)
     *
     * @param t
     * @return
     */
    @Override
    default PK gtcAccessSelective(T t) {
        GirSpringAdvExecutor.getInstance().bInsertSelectiveOne(t);
        return t.id();
    }
}
