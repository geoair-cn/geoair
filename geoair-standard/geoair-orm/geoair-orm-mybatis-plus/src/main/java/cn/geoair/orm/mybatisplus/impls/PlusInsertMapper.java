package cn.geoair.orm.mybatisplus.impls;

import cn.geoair.base.exception.GirException;
import cn.geoair.base.gpa.dao.GiCreateDao;
import cn.geoair.base.gpa.entity.GiEntitySaveable;
import cn.geoair.base.util.GutilCollection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public interface PlusInsertMapper<T extends GiEntitySaveable<PK>, PK extends Serializable>
        extends GiCreateDao<T, PK>, BaseMapper<T> {

    /**
     * 保存一条记录(属性不判空，为空的属性插入为空，无视数据库默认值)
     *
     * @param t
     * @return
     */
    @Override
    default PK gtcAccess(T t) {
        this.insert(t);
        return t.id();
    }

    /**
     * 批量插入(属性不判空，为空的属性插入为空，无视数据库默认值)
     *
     * @param records
     * @return
     */
    @Override
    default List<PK> gtcAccess(List<T> records) {
        if (GutilCollection.isEmpty(records)) {
            throw new GirException("更新的记录集为空");
        }
        List<PK> list = new ArrayList<>();
        for (T record : records) {
            list.add(gtcAccess(record));
        }
        return list;
    }

    /**
     * 插入一条记录(属性判空，为空的属性不做插入操作)
     *
     * @param t
     * @return
     */
    @Override
    default PK gtcAccessSelective(T t) {
        return gtcAccess(t);
    }

    /**
     * 批量插入(属性判空，为空的属性不做插入操作)
     *
     * @param records
     * @return
     */
    @Override
    default List<PK> gtcAccessSelective(List<T> records) {
        // throw new RuntimeException("方法尚未实现");
        return gtcAccess(records);
    }
}
