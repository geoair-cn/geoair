package cn.geoair.base.gpa.dao;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import cn.geoair.base.bean.GirBeanHelper;
import cn.geoair.base.data.model.GiModelable;
import cn.geoair.base.util.GutilClass;

public interface GiDao<M extends GiModelable<PK>, PK extends Serializable> {

	/**
	 * 通过Dao类型和模型类型获取Dao实例
	 * @param <DAO> Dao接口类型
	 * @param <M> 模型类型，必须实现GiModelable接口
	 * @param <PK> 主键类型，必须实现Serializable接口
	 * @param daoCls Dao接口的Class对象
	 * @param modelCls 模型类的Class对象
	 * @return 指定类型的Dao实例
	 */
	public static <DAO extends GiDao<M, PK>, M extends GiModelable<PK>, PK extends Serializable> DAO getDao(
			Class<DAO> daoCls, Class<M> modelCls) {
		return GirBeanHelper.getProvider().getBean(daoCls, new Type[] { modelCls, GiModelable.getIDClass(modelCls) });
	}

	/**
	 * 获取Dao接口的泛型参数类型数组
	 * @param daoClass Dao接口的Class对象
	 * @return 包含模型类型和主键类型的Type数组，如果未找到则返回null
	 */
	public static Type[] getDaoGenericTypes(Class<?> daoClass) {

		Type[] genericInterfaces = daoClass.getGenericInterfaces();
		for (Type type : genericInterfaces) {
			if (type instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType) type;

				if (GiDao.class.isAssignableFrom((Class<?>) pType.getRawType())) {

					Type[] ts = pType.getActualTypeArguments();

					if (ts.length == 2) {
						if (GiModelable.class.isAssignableFrom((Class<?>) ts[0])
								&& Serializable.class.isAssignableFrom((Class<?>) ts[1])) {
							return ts;
						}
					}
				}
			}
			if (type instanceof Class && GiDao.class.isAssignableFrom((Class<?>) type)) {
				return getDaoGenericTypes((Class<?>) type);
			}
		}
		return null;
	}

	/**
	 * 获取当前Dao关联的模型类Class对象
	 * @return 模型类的Class对象
	 */
	@SuppressWarnings("unchecked")
	default Class<M> getModelClass() {
		return (Class<M>) getDaoGenericTypes(GutilClass.getUserClass(this.getClass()))[0];
	}

	/**
	 * 获取当前Dao关联的主键类Class对象
	 * @return 主键类的Class对象
	 */
	@SuppressWarnings("unchecked")
	default Class<PK> getPKClass() {
		return (Class<PK>) getDaoGenericTypes(GutilClass.getUserClass(this.getClass()))[1];
	}

}
