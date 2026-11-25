package cn.geoair.orm.springjpa.impls;

import cn.geoair.base.exception.GirException;
import cn.geoair.base.gpa.dao.GiCreateDao;
import cn.geoair.base.gpa.entity.GiEntitySaveable;
import cn.geoair.orm.springjpa.extra.BatchRepository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public interface InsertRepository<T extends GiEntitySaveable<PK>, PK extends Serializable>
        extends JpaRepository<T, PK>, GiCreateDao<T, PK> {

    /**
     * 保存一条记录(属性不判空，为空的属性插入为空，无视数据库默认值)
     *
     * @param t
     * @return
     */
    @Override
    default PK gtcAccess(T t) {

        this.saveAndFlush(t);
        return t.id();
    }

    /**
     * 批量插入(属性不判空)
     *
     * @param records
     * @return
     */
    @Override
    default List<PK> gtcAccess(List<T> records) {

        // todo 这里要实现批量插入 BatchRepository

        if (this instanceof BatchRepository) {
            BatchRepository<T, PK> dao = (BatchRepository<T, PK>) this;
            dao.batchSave(records);
        } else {
            throw new GirException(
                    "{} 没有实现任何批量插入接口;"
                            + "请在JPA的该对象的 Repository层 继承   cn.geoair.orm.springjpa.extra.BatchRepository;",
                    this.getClass().getName());
        }
        //
        // this.saveAllAndFlush(records);
        List<PK> res = new ArrayList<>();
        for (T item : records) {
            res.add(item.id());
        }
        return res;
    }

    /**
     * 插入一条记录(属性判空，为空的属性不做插入操作)
     *
     * @param t
     * @return
     */
    @Override
    default PK gtcAccessSelective(T t) {

        // todo 这里要实现null值不插入
        this.saveAndFlush(t);
        return t.id();
    }
}
