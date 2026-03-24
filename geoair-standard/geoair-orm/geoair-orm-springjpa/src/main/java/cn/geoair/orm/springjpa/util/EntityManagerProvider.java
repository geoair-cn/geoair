package cn.geoair.orm.springjpa.util;

import java.lang.ref.WeakReference;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Component;

import cn.geoair.base.Gir;

/**
 * @author ：张俊
 * @date ：Created in 2022/7/1 14:05 @description： EntityManager提供者
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
			weakReference = new WeakReference<>(Gir.beans.getBean(EntityManagerProvider.class));
		}
		return weakReference.get();

	}

}
