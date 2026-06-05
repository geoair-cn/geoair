package cn.geoair.map.dynamic.adv.orm;

import cn.geoair.base.gpa.dao.GiRetrieveDao;
import cn.geoair.base.gpa.entity.GiEntityQueryable;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvQueryRequest;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.spring.GirSpringAdvExecutor;


import java.io.Serializable;
import java.util.List;
import java.util.Set;

public interface AdvRetrieveOrmOpt<T extends GiEntityQueryable<PK>, PK extends Serializable>
        extends
        GiRetrieveDao<T, PK> {

    /**
     * 是否能够找到主键的记录
     *
     * @param pk
     * @return
     */
    @Override
    default boolean gtcExistsWithPK(PK pk) {
        Class<T> modelClass = getModelClass();

        List<String> idByAnnotation = GirAdvSqlUtils.getIdByAnnotation(modelClass);
        String idKey = idByAnnotation.get(0);
        Number number = GirSpringAdvExecutor.getInstance().wSelectCount(GirAdvQueryRequest.
                builder(getModelClass()).
                where(w -> w.eq(idKey, pk)).build()
        );
        return number.intValue() != 0;
    }

    /**
     * 根据主键查询记录
     *
     * @param pk
     * @return
     */
    @Override
    default T gtcSearchByPK(PK pk) {
        Class<T> modelClass = getModelClass();
        List<String> idByAnnotation = GirAdvSqlUtils.getIdByAnnotation(modelClass);
        String idKey = idByAnnotation.get(0);
        T t = GirSpringAdvExecutor.getInstance().wSelectObjOne(
                GirAdvQueryRequest.
                        builder(getModelClass()).
                        where(w -> w.eq(idKey, pk)).build(), modelClass
        );
        return t;
    }

    /**
     * 根据多个主键查询多个结果
     *
     * @param pks
     * @return
     */
    @Override
    default List<T> gtcSearchByPK(Set<PK> pks) {
        Class<T> modelClass = getModelClass();

        List<String> idByAnnotation = GirAdvSqlUtils.getIdByAnnotation(modelClass);
        String idKey = idByAnnotation.get(0);
        List<T> ts = GirSpringAdvExecutor.getInstance().wSelectObjList(GirAdvQueryRequest.
                builder(getModelClass()).
                where(w -> w.in(idKey, pks)).build(), modelClass
        );
        return ts;

    }

    /**
     * 根据实体中的属性进行查询，只能有一个返回值，有多个结果时抛出异常，查询条件使用等号
     *
     * @param t
     * @return
     */
    @Override
    default T gtcSearchOne(T t) {
        Class<T> modelClass = getModelClass();
        T t1 = GirSpringAdvExecutor.getInstance().wSelectObjOne(GirAdvQueryRequest.
                builder(getModelClass()).
                where(GirAdvWhereFilter.ofBean(t)).build(), modelClass
        );
        return t1;

    }

    /**
     * 根据实体中的属性进行查询，只返回第一条数据,查询条件使用等号
     *
     * @param t
     * @return
     */
    @Override
    default T gtcSearchFirst(T t) {
        Class<T> modelClass = getModelClass();
        T t1 = GirSpringAdvExecutor.getInstance().wSelectObjOne(GirAdvQueryRequest.
                builder(getModelClass()).
                where(GirAdvWhereFilter.ofBean(t)).build(), modelClass
        );
        return t1;
    }

    /**
     * 查询单表所有记录
     *
     * @return
     */
    @Override
    default List<T> gtcSearchAll() {
        Class<T> modelClass = getModelClass();
        List<T> ts = GirSpringAdvExecutor.getInstance().wSelectObjList(GirAdvQueryRequest.
                builder(getModelClass())
                .build(), modelClass
        );
        return ts;
    }

    /**
     * 根据条件查询多条记录
     *
     * @param t
     * @return
     */
    @Override
    default List<T> gtcSearch(T t) {
        Class<T> modelClass = getModelClass();
        List<T> ts = GirSpringAdvExecutor.getInstance().wSelectObjList(GirAdvQueryRequest.
                builder(getModelClass()).
                where(GirAdvWhereFilter.ofBean(t)).build(), modelClass
        );
        return ts;
    }

    /**
     * 根据条件查询总数
     *
     * @param t
     * @return
     */
    @Override
    default long gtcSearchCount(T t) {
        Number number = GirSpringAdvExecutor.getInstance().wSelectCount(GirAdvQueryRequest.
                builder(getModelClass()).
                where(GirAdvWhereFilter.ofBean(t)).build()
        );
        return number.longValue();
    }

    /**
     * 查询总数
     *
     * @param
     * @return
     */
    @Override
    default long gtcSearchCount() {
        Number number = GirSpringAdvExecutor.getInstance().wSelectCount(GirAdvQueryRequest.
                builder(getModelClass())
                .build()
        );
        return number.longValue();
    }
}
