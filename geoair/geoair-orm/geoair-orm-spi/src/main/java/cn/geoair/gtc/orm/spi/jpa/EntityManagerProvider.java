package cn.geoair.gtc.orm.spi.jpa;

import org.hibernate.metamodel.internal.MetamodelImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.walking.spi.AttributeDefinition;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import java.beans.PropertyDescriptor;
import java.util.Map;

/**
 * @author ：张俊
 * @date ：Created in 2022/7/1 14:05 @description： EntityManager提供者
 */
@Component
public class EntityManagerProvider {

	@PersistenceContext
	EntityManager entityManager;

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public void Test() {
		// 通过EntityManager获取factory
		EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
		MetamodelImpl metaData = (MetamodelImpl) entityManagerFactory.getMetamodel();
		Map<String, EntityPersister> persisterMap = metaData.entityPersisters();
		for (Map.Entry<String, EntityPersister> entity : persisterMap.entrySet()) {
			Class<?> targetClass = entity.getValue().getMappedClass();
			SingleTableEntityPersister persister = (SingleTableEntityPersister) entity.getValue();
			Iterable<AttributeDefinition> attributes = persister.getAttributes();
			String entityName = targetClass.getSimpleName();// Entity的名称
			String tableName = persister.getTableName();// Entity对应的表的英文名
			System.out.println("类名：" + entityName + " => 表名：" + tableName);
			for (AttributeDefinition attr : attributes) {
				String propertyName = attr.getName(); // 在entity中的属性名称
				String[] columnName = persister.getPropertyColumnNames(propertyName); // 对应数据库表中的字段名
				String type = "";
				// attr.getSource().
				PropertyDescriptor targetPd = BeanUtils.getPropertyDescriptor(targetClass, propertyName);
				if (targetPd != null) {
					type = targetPd.getPropertyType().getSimpleName();
				}
				System.out.println("属性名：" + propertyName + " => 类型：" + type + " => 数据库字段名：" + columnName[0]);
			}

		}
	}

	public EntityPersister getEntityPersister(Class<?> entityClass) {
		String canonicalName = entityClass.getCanonicalName();
		// 通过EntityManager获取factory
		EntityManagerFactory entityManagerFactory = entityManager.getEntityManagerFactory();
		MetamodelImpl metaData = (MetamodelImpl) entityManagerFactory.getMetamodel();
		Map<String, EntityPersister> persisterMap = metaData.entityPersisters();
		EntityPersister entityPersister = persisterMap.get(canonicalName);
		return entityPersister;
	}

	public String getEntityTableField(Class<?> entityClass, String propertyName) {
		SingleTableEntityPersister entityPersister = (SingleTableEntityPersister) getEntityPersister(entityClass);
		String[] columnName = entityPersister.getPropertyColumnNames(propertyName);
		return columnName[0];
	}

}
