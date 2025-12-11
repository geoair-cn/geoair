package cn.geoair.gtc.orm.springjpa.util;

import cn.geoair.gtc.base.Gtc;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.lang.ref.WeakReference;


/**
 * @author ：张俊
 * @date ：Created in 2022/7/1 14:05
 * @description： EntityManager提供者
 */
@Component
public class EntityManagerProvider {

    private static WeakReference<EntityManagerProvider> weakReference = null;

    @PersistenceContext
    EntityManager entityManager;

    public EntityManager getEntityManager() {
        return entityManager;
    }


    public static EntityManagerProvider getEntityManagerProvider() {
        if (weakReference == null) {
            weakReference = new WeakReference<>( Gtc.beans.getBean(EntityManagerProvider.class));
        }
        return weakReference.get();

    }

}
