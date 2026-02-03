package cn.geoair.gtc.orm.springjpa.extra;

import cn.geoair.gtc.base.Gir;
import cn.geoair.gtc.base.exception.GirException;
import cn.geoair.gtc.base.gpa.entity.GiEntityable;
import cn.geoair.gtc.base.util.GutilReflection;
import cn.geoair.gtc.orm.springjpa.util.EntityManagerProvider;

import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Iterator;

/**
 * @author ：zhangjun
 * @date ：Created in 2022/10/25 16:33
 * @description： 批量操作相关的 存储仓库
 */
public interface BatchRepository<T extends GiEntityable<PK>, PK extends Serializable> {

    Integer BATCH_SIZE = 500;

    /**
     * 批量保存操作
     *
     * @param collect
     * @param <S>
     * @return
     */
    @Transactional
    default <S extends T> Iterable<S> batchSave(Iterable<S> collect) {
//        EntityManager entityManager = (EntityManager)  gtcReflectionUtil
//                .getFieldValue(this,  gtcReflectionUtil.findField( gtcAopUtil.getTargetClass(this.getClass()), "em", EntityManager.class));
////        EntityManagerProvider.getEntityManagerProvider().getEntityManager();
        EntityManager entityManager = EntityManagerProvider.getEntityManagerProvider().getEntityManager();
        Iterator<S> iterator = collect.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            S next = iterator.next();
            if (entityManager.contains(next)) {
                 Gir.log.warn("该对象数据库中已经存在，不执行保存操作!：{}->{}", next.getClass().getName(),  Gir.toJson(next).toJSONString());
            } else {
                entityManager.persist(next);
            }
            index++;
            if (index % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
        if (index % BATCH_SIZE != 0) {
            entityManager.flush();
            entityManager.clear();
        }
        return collect;
    }


    /**
     * 批量更新操作
     *
     * @param collect
     * @param <S>
     * @return
     */
    @Transactional
    default <S extends T> Iterable<S> batchUpdate(Iterable<S> collect) {
        EntityManager entityManager = EntityManagerProvider.getEntityManagerProvider().getEntityManager();
        Iterator<S> iterator = collect.iterator();
        int index = 0;

        while (iterator.hasNext()) {
            S next = iterator.next();
            boolean contains = entityManager.contains(next);
            if (contains) {
                entityManager.merge(next);
            } else {
                 Gir.log.warn("数据库中不存在该对象，不执行更新操作！：{}->{}", next.getClass().getName(),  Gir.toJson(next).toJSONString());
            }
            index++;
            if (index % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        if (index % BATCH_SIZE != 0) {
            entityManager.flush();
            entityManager.clear();
        }
        return collect;
    }

    /**
     * 批量更新操作 选择更新
     *
     * @param collect
     * @param <S>
     * @return
     */
    @Transactional
    default <S extends T> Iterable<S> batchUpdateSelective(Iterable<S> collect) {
        EntityManager entityManager = EntityManagerProvider.getEntityManagerProvider().getEntityManager();
        Iterator<S> iterator = collect.iterator();
        int index = 0;
        while (iterator.hasNext()) {
            S next = iterator.next();
            S dabaseObj = (S) entityManager.find(next.getClass(), next.id());
            if (dabaseObj != null) {
                Field[] fields = dabaseObj.getClass().getDeclaredFields();
                for (Field field : fields) {
                    try {
                        GutilReflection.makeAccessible(field);
                        //条件：对于某个Field,如果orig的值不为null,并且src的值为null
                        if (field.get(dabaseObj) != null && field.get(next) == null) {
                            //操作：则将orig的当前Field的值，赋值给dest对应的Field
                            field.set(next, field.get(dabaseObj));
                        }
                    } catch (Exception e) {
                        throw new GirException("po对象无法反射赋值，请检查对象配置", e);
                    }
                }
                entityManager.merge(next);
            } else {
                 Gir.log.warn("数据库中不存在该对象，不执行更新操作！：{}->{}", next.getClass().getName(),  Gir.toJson(next).toJSONString());
            }
            index++;
            if (index % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        if (index % BATCH_SIZE != 0) {
            entityManager.flush();
            entityManager.clear();
        }
        return collect;
    }


}
