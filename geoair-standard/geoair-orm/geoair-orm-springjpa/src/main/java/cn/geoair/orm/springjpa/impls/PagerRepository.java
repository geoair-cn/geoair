package cn.geoair.orm.springjpa.impls;

import java.io.Serializable;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.gpa.dao.GiPagerDao;
import cn.geoair.base.gpa.entity.GiEntityQueryable;
import cn.geoair.base.util.GutilReflection;
import cn.geoair.orm.springjpa.support.GirSpringJpaPageHelper;

public interface PagerRepository<T extends GiEntityQueryable<PK>, PK extends Serializable>
		extends JpaRepository<T, PK>, JpaSpecificationExecutor<T>, GiPagerDao<T, PK> {

	/**
	 * gtcPageParam 转换 Pageable
	 * @param pageParam
	 * @return
	 */
	default Pageable toPageable(GiPageParam pageParam) {
		return GirSpringJpaPageHelper.toPageable(pageParam);
	}

	/**
	 * 分页查询单表所有记录
	 * @return
	 */
	@Override
	default GiPager<T> gtcSearchPageAll(GiPageParam pageParam) {
		// int pageNum = pageParam.pageNum();
		// int pageSize = pageParam.pageSize();
		// PageRequest pageRequest = PageRequest.of(pageNum, pageSize);
		Page<T> jpapage = this.findAll((Specification<T>) null, toPageable(pageParam));
		pageParam.putParam(jpapage.getSize(), jpapage.getNumber(), null);
		return getModelPager().put(jpapage.getContent(), jpapage.getTotalElements(), pageParam);
	}

	/**
	 * 根据PO条件分页查询多条记录
	 * @param t
	 * @return
	 */
	// @SuppressWarnings("unchecked")
	@Override
	default GiPager<T> gtcSearchPage(T t, GiPageParam pageParam) {

		Class<T> modelClass = this.getModelClass();
		// JpaSpecificationExecutor<T> simpleJpaRepository =
		// gtc.beans.getBean(JpaSpecificationExecutor.class, new Type[]{modelClass});
		Example<T> of = Example.of(t);
		// int pageNum = pageParam.pageNum();
		// int pageSize = pageParam.pageSize();
		// PageRequest pageRequest = PageRequest.of(pageNum, pageSize);
		Page<T> jpapage = this.findAll(of, toPageable(pageParam));
		pageParam.putParam(jpapage.getSize(), jpapage.getNumber(), null);
		return GiPager.ofClass(modelClass).put(jpapage.getContent(), jpapage.getTotalElements(), pageParam);

	}

	@SuppressWarnings("unchecked")
	@Override
	default GiPager<T> gtcSearchPageByPK(List<PK> pks, GiPageParam pageParam) {

		Class<T> modelClass = this.getModelClass();

		JpaEntityInformation<T, ?> entityInformation = (JpaEntityInformation<T, ?>) GutilReflection.getFieldValue(this,
				GutilReflection.findField(this.getClass(), "entityInformation", JpaEntityInformation.class));

		// JpaEntityInformationSupport.getEntityInformation(modelClass, em);

		Specification<T> specification = new Specification<T>() {
			/**
			 *
			 */
			private static final long serialVersionUID = -5083792737032473764L;

			@Override
			public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

				Path<?> path = root.get(entityInformation.getIdAttribute());
				// Collection<PK> idCollection = Streamable.of(pks).toList();
				return path.in(pks);
			}
		};

		Page<T> jpapage = this.findAll(specification, toPageable(pageParam));
		pageParam.putParam(jpapage.getSize(), jpapage.getNumber(), null);
		return GiPager.ofClass(modelClass).put(jpapage.getContent(), jpapage.getTotalElements(), pageParam);

	}

}
