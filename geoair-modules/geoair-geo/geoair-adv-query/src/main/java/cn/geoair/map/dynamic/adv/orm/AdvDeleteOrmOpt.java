package cn.geoair.map.dynamic.adv.orm;

import cn.geoair.base.gpa.dao.GiDeleteDao;
import cn.geoair.base.gpa.entity.GiEntityRemovable;
import cn.geoair.map.dynamic.adv.query.utils.GirAdvSqlUtils;
import cn.geoair.map.dynamic.adv.query.wherequery.GirAdvWhereFilter;
import cn.geoair.map.dynamic.adv.spring.GirSpringAdvExecutor;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

public interface AdvDeleteOrmOpt<T extends GiEntityRemovable<PK>, PK extends Serializable>
        extends GiDeleteDao<T, PK> {

    /**
     * 根据实体属性作为条件进行删除，查询条件使用等号
     *
     * @param t
     * @return
     */
    @Override
    default int gtcDeleteBy(T t) {
        GirAdvWhereFilter whereFilter = GirAdvWhereFilter.ofBean(t);
        String tableName = GirAdvSqlUtils.getTableName(t.getClass());
        return GirSpringAdvExecutor.getInstance().bDeleteByWhere(tableName, whereFilter);
    }

    /**
     * 根据主键删除记录
     *
     * @param
     * @return
     */
    @Override
    default void gtcDeleteByPK(PK pk) {
        Class<T> modelClass = getModelClass();
        String tableName = GirAdvSqlUtils.getTableName(modelClass);
        List<String> idByAnnotation = GirAdvSqlUtils.getIdByAnnotation(modelClass);
        String idKey = idByAnnotation.get(0);
        GirSpringAdvExecutor.getInstance().bDeleteByPK(tableName, idKey, pk);
    }

    /**
     * 根据主键批量删除
     *
     * @param key
     * @return
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    default void gtcDeleteByPK(List<PK> pks) {
        Class<T> modelClass = getModelClass();
        String tableName = GirAdvSqlUtils.getTableName(modelClass);
        List<String> idByAnnotation = GirAdvSqlUtils.getIdByAnnotation(modelClass);
        String idKey = idByAnnotation.get(0);
        GirSpringAdvExecutor.getInstance().bDeleteByPKs(tableName, idKey, new HashSet<>(pks));
    }

    /**
     * 删除所有数据
     *
     * @param key
     * @return
     */
    @Override
    default void gtcDeleteAll() {
        //  不支持
    }
}
