package cn.geoair.gtc.orm.springjpa.impls;

import java.io.Serializable;
import java.util.List;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.util.Assert;
import cn.geoair.gtc.base.gpa.dao.GiDeleteDao;
import cn.geoair.gtc.base.gpa.entity.GiEntityRemovable;

@NoRepositoryBean
public interface DeleteRepository<T extends GiEntityRemovable<PK>, PK extends Serializable> extends JpaRepository<T, PK>, GiDeleteDao<T, PK> {

    /**
     * 根据实体属性作为条件进行删除，查询条件使用等号
     *
     * @param t
     * @return
     */
	@Override
    default int  gtcDeleteBy(T t) {
		Example<T> of = Example.of(t);
        List<T> list = this.findAll(of);
        int len = list.size();
        if(len > 0) {
        	this.deleteAll(list);
        }
        return len;
	}

    /**
     * 根据主键删除记录
     *
     * @param key
     * @return
     */
	@Override
    default void  gtcDeleteByPK(PK pk) {
		Assert.notNull(pk, "The given id must not be null!");
        this.deleteById(pk);
	}

    /**
     * 根据主键批量删除
     *
     * @param key
     * @return
     */
	@Override
    default void  gtcDeleteByPK(List<PK> pks) {
		this.deleteAllByIdInBatch(pks);
	}

    /**
     * 删除所有数据
     *
     * @param key
     * @return
     */
	@Override
    default void  gtcDeleteAll() {
		this.deleteAll();
	}
}
