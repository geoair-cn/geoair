package cn.geoair.orm.springjpa.support;

import java.util.Iterator;
import java.util.function.Function;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import cn.geoair.base.data.page.GfunPageExcute;
import cn.geoair.base.data.page.GiPageExcuter;
import cn.geoair.base.data.page.GiPageParam;
import cn.geoair.base.data.page.GiPager;
import cn.geoair.base.gpa.support.GirOrder;
import cn.geoair.base.gpa.support.GirSort;

/**
 * @author ：张俊
 * @date ：Created in 2022/6/21 9:55 @description： spring jpa的 分页执行器
 */
public class GirSpringJpaPageHelper implements GiPageExcuter {

	/**
	 * 从 girSort对象拼装 Spring Data Jpa Sort
	 * @param sort
	 * @return
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Sort sortFromGirSort(GirSort sort) {

		Sort jpaSort = Sort.unsorted();
		if (sort != null) {
			Iterator<GirOrder<?>> iterator = sort.iterator();
			while (iterator.hasNext()) {

				GirOrder<?> order = iterator.next();
				if (order.getPropertyFun() != null) {
					Class<?> cls = order.getEntityClass();

					Sort sort1 = Sort.sort(cls).by((Function) order.getPropertyFun());

					if (order.getDirection() == GirOrder.Direction.DESC) {
						jpaSort = jpaSort.and(sort1.descending());
					}
					else {
						jpaSort = jpaSort.and(sort1);
					}
				}
				else if (order.getProperty() != null) {
					jpaSort = jpaSort
							.and(Sort.by(Direction.fromString(order.getDirection().value()), order.getProperty()));
				}
			}
		}

		return jpaSort;
	}

	/**
	 * 从 girPageParam 转换 Spring Data Jpa Pageable (spring data jpa 的页码从0开始)
	 * @param pageParam
	 * @return
	 */
	public static Pageable toPageable(GiPageParam pageParam) {
		int pageNum = pageParam.pageNum() - 1;
		int pageSize = pageParam.pageSize();
		PageRequest pageRequest = PageRequest.of(pageNum, pageSize, sortFromGirSort(pageParam.sort()));
		return pageRequest;
	}

	@Override
	public <R> GiPager<R> excutePage(GfunPageExcute<R> pageExcute, GiPageParam pageParam) {

		// int pageNum = pageParam.pageNum();
		// int pageSize = pageParam.pageSize();
		//
		// PageRequest of = PageRequest.of(pageNum, pageSize);
		//
		// Iterable<R> excute = pageExcute.excute();
		//
		// Page pageobj = simpleJpaRepository.findAll(of);
		// pageParam.putParam(pageobj.getSize(), pageobj.getNumber(), null);
		// gtcPager<R> pager = pageExcute.get gtcPager();
		// pager.put(pageobj.toList(), pageobj.getTotalElements(), pageParam);
		return null;
	}

}
