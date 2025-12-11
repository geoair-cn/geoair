package cn.geoair.gtc.orm.springjpa.impls;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import cn.geoair.gtc.base.gpa.dao.GiPagerDao;
import cn.geoair.gtc.base.gpa.dao.GiRetrieveDao;
import cn.geoair.gtc.base.gpa.entity.GiEntityQueryable;

public interface RetrieveRepository<T extends GiEntityQueryable<PK>, PK extends Serializable> extends JpaRepository<T, PK>, JpaSpecificationExecutor<T>, GiRetrieveDao<T, PK>, GiPagerDao<T, PK> {

    /**
     * 是否能够找到主键的记录
     * @param pk
     * @return
     */
    @Override
	default boolean  gtcExistsWithPK(PK pk) {
    	return this.existsById(pk);
    }
    /**
     * 根据主键查询记录
     * @param pk
     * @return
     */
    @Override
	default T  gtcSearchByPK(PK pk) {
    	return this.findById(pk).orElse(null);
    }
	/**
	 * 根据多个主键查询多个结果
	 * @param pks
	 * @return
	 */
    @Override
	default List<T>  gtcSearchByPK(Set<PK> pks) {
    	return this.findAllById(pks);
    }

    /**
     * 根据实体中的属性进行查询，只能有一个返回值，有多个结果时抛出异常，查询条件使用等号
     * @param t
     * @return
     */
    @Override
	default T  gtcSearchOne(T t) {
    	 Example<T> of = Example.of(t);
         Optional<T> value = this.findOne(of);//如果查询到多个结果，这里会抛异常
         return value.get();
    }
    /**
     * 根据实体中的属性进行查询，只返回第一条数据,查询条件使用等号
     * @param t
     * @return
     */
    @Override
	default T  gtcSearchFirst(T t) {
    	//这里需要优化，全查性能有问题
    	Example<T> of = Example.of(t);
        Iterable<T> list = this.findAll(of);
        if (list.iterator().hasNext()) {
            return list.iterator().next();
        }
        return null;
    }

    /**
     * 查询单表所有记录
     * @return
     */
    @Override
	default List<T>  gtcSearchAll() {
    	return this.findAll();
    }
    /**
     * 根据条件查询多条记录
     * @param t
     * @return
     */
    @Override
	default List<T>  gtcSearch(T t) {
    	Example<T> of = Example.of(t);
        return this.findAll(of);
    }
    /**
     * 根据条件查询总数
     * @param t
     * @return
     */
    @Override
	default long  gtcSearchCount(T t) {
    	Example<T> of = Example.of(t);
    	return this.count(of);
    }

    /**
     * 查询总数
     * @param
     * @return
     */
    @Override
	default long  gtcSearchCount() {
    	return this.count();
    }

}
