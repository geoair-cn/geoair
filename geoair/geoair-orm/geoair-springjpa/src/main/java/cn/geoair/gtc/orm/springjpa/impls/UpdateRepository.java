package cn.geoair.gtc.orm.springjpa.impls;

import cn.geoair.gtc.base.Gir;
import cn.geoair.gtc.base.exception.GirException;
import cn.geoair.gtc.base.gpa.dao.GiUpdateDao;
import cn.geoair.gtc.base.gpa.entity.GiEntityAlterable;
import cn.geoair.gtc.base.util.GutilObject;
import cn.geoair.gtc.base.util.GutilReflection;
import cn.geoair.gtc.orm.springjpa.extra.BatchRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;

public interface UpdateRepository<T extends GiEntityAlterable<PK>, PK extends Serializable> extends JpaRepository<T, PK>, GiUpdateDao<T, PK> {

	/*
    @PersistenceContext
    EntityManager em = null;
    */


    /**
     * 根据主键更新记录(更新所有字段)
     *
     * @param t
     * @return
     */
    @Override
    default int gtcUpdateByPK(T t) {
        if (GutilObject.isNull(t.id())) {
            throw new GirException("更新的对象ID不存在");//如果ID为空，抛出异常，区别于保存方法。
        }
        boolean b = this.existsById(t.id());
        if (!b) {
            throw new GirException("非更新操作! 对象: {}", Gir.toJson(t).toJSONString());
        }
        this.save(t);
        return 1;
    }

    /**
     * 根据主键更新记录(更新不为Null的字段)
     *
     * @param t
     * @return
     */
    @Override
    default int gtcUpdateByPKSelective(T t) {
        PK pk = t.id();
        if (GutilObject.isNull(pk)) {
            throw new GirException("更新的对象ID不存在");//如果ID为空，抛出异常，区别于保存方法。
        }

        T orig = this.findById(pk).orElse(null);
        if (GutilObject.isNull(orig)) {
            throw new GirException("非更新操作! 对象: {}", Gir.toJson(t).toJSONString());
        }

        Field[] fields = t.getClass().getDeclaredFields();
        for (Field field : fields) {
            try {
                GutilReflection.makeAccessible(field);
                //条件：对于某个Field,如果orig的值不为null,并且src的值为null
                if (field.get(orig) != null && field.get(t) == null) {
                    //操作：则将orig的当前Field的值，赋值给dest对应的Field
                    field.set(t, field.get(orig));
                }
            } catch (Exception e) {
                throw new GirException("po对象无法反射赋值，请检查对象配置", e);
            }
        }
        this.saveAndFlush(t);
        return 1;
    }

    /**
     * 根据主键批量更新
     *
     * @param records
     * @return
     */
    @Override
    default int gtcUpdateByPK(List<T> records) {
        if (this instanceof BatchRepository) {
            BatchRepository<T, PK> dao = (BatchRepository<T, PK>) this;
            dao.batchUpdate(records);
        } else {
            throw new GirException("{} 没有实现任何批量更新接口;"
                    + "请在JPA的该对象的 Repository层 继承   com.gtc.orm.springjpa.extra.BatchRepository;", this.getClass().getName());
        }
        return 1;
//        this.saveAll(records);
    }

    /**
     * 根据主键批量更新(更新不为Null的字段)
     *
     * @param records
     * @return
     */
    @Override
    default int gtcUpdateByPKSelective(List<T> records) {
        if (this instanceof BatchRepository) {
            BatchRepository<T, PK> dao = (BatchRepository<T, PK>) this;
            dao.batchUpdateSelective(records);
        } else {
            throw new GirException("{} 没有实现任何批量更新接口;"
                    + "请在JPA的该对象的 Repository层 继承   com.gtc.orm.springjpa.extra.BatchRepository;", this.getClass().getName());
        }
        return 1;
    }

}
