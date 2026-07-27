package cn.geoair.orm.springjpa.util;

import cn.geoair.base.Gir;
import java.lang.ref.WeakReference;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

/**
 * @author ：张俊
 * @date ：Created in 2022/7/1 14:05 @description： EntityManager提供者
 */
@Component
public class EntityManagerProvider extends cn.geoair.orm.spi.jpa.EntityManagerProvider {

    private static WeakReference<EntityManagerProvider> weakReference = null;

    @PersistenceContext EntityManager entityManager;

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    public static EntityManagerProvider getEntityManagerProvider() {
        if (weakReference == null) {
            weakReference = new WeakReference<>(Gir.beans.getBean(EntityManagerProvider.class));
        }
        return weakReference.get();
    }
}
